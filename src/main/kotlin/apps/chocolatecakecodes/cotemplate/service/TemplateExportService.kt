package apps.chocolatecakecodes.cotemplate.service

import apps.chocolatecakecodes.cotemplate.auth.CotemplateSecurityIdentity
import apps.chocolatecakecodes.cotemplate.auth.Role
import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import apps.chocolatecakecodes.cotemplate.db.TemplateItemEntity
import apps.chocolatecakecodes.cotemplate.db.UserEntity
import apps.chocolatecakecodes.cotemplate.dto.TemplateCreatedDto
import apps.chocolatecakecodes.cotemplate.dto.export.CoTemplateDto
import apps.chocolatecakecodes.cotemplate.dto.export.ItemDto
import apps.chocolatecakecodes.cotemplate.dto.export.TemplateDto
import apps.chocolatecakecodes.cotemplate.dto.export.UserDto
import apps.chocolatecakecodes.cotemplate.exception.ExportExceptions
import apps.chocolatecakecodes.cotemplate.exception.TemplateExceptions
import apps.chocolatecakecodes.cotemplate.service.TemplateTeamService.Companion.TEAM_REGEX
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.absolute
import kotlin.io.path.name

@OptIn(ExperimentalSerializationApi::class)
@ApplicationScoped
internal class TemplateExportService(
    private val mngService: TemplateManagementService,
    private val itemService: TemplateItemService,
    private val serde: Json,
) {

    companion object {

        internal const val MAX_ZIP_CONTENT_SIZE = 2L * 1024 * 1024 * 1024
        internal const val MAX_ZIP_CONTENT_COUNT = 1024
    }

    fun exportTemplate(ident: CotemplateSecurityIdentity, tplName: String): InputStream {
        mngService.checkTemplateAccess("exporting template", ident, tplName)

        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)

        val streamLeft = PipedOutputStream()
        val streamRight = PipedInputStream()
        streamLeft.connect(streamRight)

        CoroutineScope(Dispatchers.IO).launch {
            val scratchDir = Files.createTempDirectory("cotemplate-export")
            try {
                packData(tpl, scratchDir)
                packZip(scratchDir, streamLeft)
            } finally {
                scratchDir.toFile().deleteRecursively()
            }
        }

        return streamRight
    }

    @Transactional
    fun importTemplate(name: String, stream: InputStream): TemplateCreatedDto {
        val scratchDir = Files.createTempDirectory("cotemplate-import")
        try {
            unpackZip(scratchDir, stream)

            val metadata = readMetadata(scratchDir)
            val (tpl, createdDto) = unpackTemplate(metadata.template, name)
            unpackUsers(tpl, metadata.users)
            unpackItems(tpl, metadata.items, metadata.users, scratchDir)

            return createdDto
        } finally {
            scratchDir.toFile().deleteRecursively()
        }
    }

    @Transactional
    protected fun packData(tpl: TemplateEntity, dir: Path) {
        val tplDetails = packTemplateDetails(tpl)
        val users = packUsers(tpl)
        val items = packItems(tpl, users, dir)
        val metadata = CoTemplateDto(tplDetails, users, items)

        Files.newOutputStream(dir.resolve("data.json"), StandardOpenOption.CREATE_NEW).use { out ->
            serde.encodeToStream(metadata, out)
        }
    }

    private fun packTemplateDetails(tpl: TemplateEntity): TemplateDto {
        return TemplateDto(tpl.name, tpl.width, tpl.height, tpl.teamCreatePolicy)
    }

    private fun packUsers(tpl: TemplateEntity): List<UserDto> {
        return UserEntity.findAllByTemplate(tpl).map {
            UserDto(it.name, it.pass, it.role)
        }
    }

    private fun packItems(tpl: TemplateEntity, users: List<UserDto>, dir: Path): List<ItemDto> {
        val dst = dir.resolve("items")
        Files.createDirectories(dst)

        val items = TemplateItemEntity.findAllByTemplate(tpl)
        val ret = ArrayList<ItemDto>()
        var id = 1
        for(item in items) {
            Files.copy(itemService.imgStoragePath(item), dst.resolve("$id"))

            val ownerIdx = users.indexOfFirst { it.name == item.owner.name }
            assert(ownerIdx >= 0) { "user referenced by item does not exist" }
            ret.add(ItemDto(id, ownerIdx, item.description, item.x, item.y, item.z))

            id++
        }

        return ret
    }

    private fun packZip(dir: Path, out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("data.json"))
            Files.newInputStream(dir.resolve("data.json")).use { it.transferTo(zip) }

            Files.list(dir.resolve("items")).forEach { item ->
                zip.putNextEntry(ZipEntry("items/${item.name}"))
                Files.newInputStream(item).use { it.transferTo(zip) }
            }
        }
    }

    private fun unpackZip(dir: Path, ins: InputStream) {
        val dest = dir.toRealPath()
        var size = 0L
        var count = 0
        ZipInputStream(ins).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while(entry != null) {
                val path = dest.resolve(entry.name).absolute().normalize()
                if(!path.startsWith(dest))
                    throw ExportExceptions.invalidZip("contains path which is not inside zip-root")

                size += entry.size
                count++
                if(size > MAX_ZIP_CONTENT_SIZE)
                    throw ExportExceptions.invalidZip("zip contents are too large")
                if(count > MAX_ZIP_CONTENT_COUNT)
                    throw ExportExceptions.invalidZip("too many zip elements")

                Files.createDirectories(path.parent)
                Files.newOutputStream(path).use { zip.transferTo(it) }

                entry = zip.nextEntry
            }
        }
    }

    private fun readMetadata(dir: Path): CoTemplateDto {
        try {
            return Files.newInputStream(dir.resolve("data.json")).use {
                serde.decodeFromStream(it)
            }
        } catch(e: Exception) {
            throw ExportExceptions.invalidMetadata(e.message ?: "unknown reason", e)
        }
    }

    private fun unpackTemplate(details: TemplateDto, name: String): Pair<TemplateEntity, TemplateCreatedDto> {
        val created = mngService.createTemplate(name, details.width, details.height, details.teamCreatePolicy)
        val tpl = TemplateEntity.findByUniqueName(created.uniqueName) ?:
            throw AssertionError("newly created template is not in DB")
        return Pair(tpl, created)
    }

    private fun unpackUsers(tpl: TemplateEntity, users: List<UserDto>) {
        users.forEach {
            if(!TEAM_REGEX.matches(it.name))
                throw TemplateExceptions.invalidName()
            if(it.role != Role.TEMPLATE_TEAM && it.role != Role.TEMPLATE_OWNER)
                throw ExportExceptions.invalidMetadata("user '${it.name}' has invalid role")
            if(it.name == TemplateManagementService.OWNER_USER_NAME)
                return@forEach// is already created by mngService.createTemplate()

            UserEntity().apply {
                this.name = it.name
                this.pass = it.pass
                this.role = it.role
                this.template = tpl
            }.persist()
        }
    }

    private fun unpackItems(tpl: TemplateEntity, items: List<ItemDto>, users: List<UserDto>, dir: Path) {
        val itemsDir = dir.resolve("items")
        items.forEachIndexed { idx, item ->
            val user = users.getOrNull(item.ownerRef)?.let {
                UserEntity.findByTemplateAndName(tpl, it.name)
            } ?: throw ExportExceptions.invalidMetadata("user for item [$idx] is invalid")

            val bytes = Files.readAllBytes(itemsDir.resolve(item.imgRef.toString()))

            itemService.addItemInternal(tpl, user, item.description, item.x, item.y, item.z, bytes)
        }
    }
}
