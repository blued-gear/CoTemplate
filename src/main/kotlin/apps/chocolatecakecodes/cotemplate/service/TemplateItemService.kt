package apps.chocolatecakecodes.cotemplate.service

import apps.chocolatecakecodes.cotemplate.auth.CotemplateSecurityIdentity
import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import apps.chocolatecakecodes.cotemplate.db.TemplateItemEntity
import apps.chocolatecakecodes.cotemplate.db.UserEntity
import apps.chocolatecakecodes.cotemplate.dto.TemplateItemDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateItemsDto
import apps.chocolatecakecodes.cotemplate.exception.TemplateExceptions
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.MutableImage
import com.sksamuel.scrimage.nio.PngWriter
import io.quarkus.cache.Cache
import io.quarkus.cache.CacheName
import io.quarkus.cache.CacheResult
import io.quarkus.cache.CompositeCacheKey
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@ApplicationScoped
internal class TemplateItemService(
    private val mngService: TemplateManagementService,
    @ConfigProperty(name = "cotemplate.image-storage")
    imgDirPath: String,
) {

    companion object {

        private val LOGGER = LoggerFactory.getLogger(TemplateItemService::class.java)
    }

    @Inject
    @CacheName("template-rendered")
    private lateinit var renderCache: Cache
    private val imgDir: Path

    init {
        if(imgDirPath.isEmpty())
            throw IllegalArgumentException("COTEMPLATE_IMG_STOARGE was unset")
        imgDir = try {
            Files.createDirectories(Path.of(imgDirPath).toAbsolutePath().normalize())
        } catch (e: IOException) {
            throw IllegalArgumentException("COTEMPLATE_IMG_STOARGE could not be accessed / created", e)
        }
        LOGGER.info("using imgDir: $imgDir")
    }

    fun addItem(ident: CotemplateSecurityIdentity, tplName: String, desc: String, x: Int, y: Int, z: Int, img: ByteArray): TemplateItemDto {
        mngService.checkTeamAccess("modifying items", ident, tplName)

        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)
        val user = UserEntity.findById(ident.userId)!!

        val entity = addItemInternal(tpl, user, desc, x, y, z, img)
        return itemEntityToDto(entity)
    }

    internal fun addItemInternal(tpl: TemplateEntity, user: UserEntity, desc: String, x: Int, y: Int, z: Int, img: ByteArray): TemplateItemEntity {
        val (w, h) = getImageDimensions(img)
        val entity = addItemEntity(tpl, user, desc, x, y, z, w, h)

        try {
            Files.write(imgStoragePath(entity), img, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        } catch(e: Exception) {
            LOGGER.error("unable to store image for item ${tpl.uniqueName}::${entity.imgId}", e)
            entity.delete()
            throw e
        }

        return entity
    }

    @Transactional
    protected fun addItemEntity(tpl: TemplateEntity, owner: UserEntity, desc: String, x: Int, y: Int, z: Int, w: Int, h: Int): TemplateItemEntity {
        return TemplateItemEntity(tpl, owner, desc, x, y, z, w, h)
            .also { it.persist() }
    }

    @Transactional
    fun deleteItem(ident: CotemplateSecurityIdentity, tplName: String, imgId: ULong) {
        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)
        val item = TemplateItemEntity.findByTemplateAndImgId(tpl, imgId.toLong())
            ?: throw TemplateExceptions.itemNotFound(tplName, imgId)

        mngService.checkItemAccess("modifying items", ident, tplName, item.owner)

        try {
            Files.delete(imgStoragePath(item))
        } catch(e: Exception) {
            LOGGER.error("exception while deleting image for item $tplName::$imgId", e)
        }

        item.delete()
        invalidateCachedWithItem(tplName, imgId)
    }

    @Transactional
    fun updateItemDetails(ident: CotemplateSecurityIdentity, tplName: String, imgId: ULong, desc: String?, x: Int?, y: Int?, z: Int?): TemplateItemDto {
        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)
        val item = TemplateItemEntity.findByTemplateAndImgId(tpl, imgId.toLong())
            ?: throw TemplateExceptions.itemNotFound(tplName, imgId)

        mngService.checkItemAccess("modifying items", ident, tplName, item.owner)

        if(desc != null) item.description = desc
        if(x != null) item.x = x
        if(y != null) item.y = y
        if(z != null) item.z = z

        item.persist()
        invalidateCachedWithItem(tplName, imgId)
        return itemEntityToDto(item)
    }

    @Transactional
    fun updateItemImage(ident: CotemplateSecurityIdentity, tplName: String, imgId: ULong, img: ByteArray): TemplateItemDto {
        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)
        val item = TemplateItemEntity.findByTemplateAndImgId(tpl, imgId.toLong())
            ?: throw TemplateExceptions.itemNotFound(tplName, imgId)

        mngService.checkItemAccess("modifying items", ident, tplName, item.owner)

        getImageDimensions(img).let { (w, h) ->
            item.width = w
            item.height = h
        }

        try {
            Files.write(imgStoragePath(item), img)
        } catch(e: Exception) {
            LOGGER.error("unable to overwrite image for item $tplName::$imgId", e)
            throw e
        }

        item.persist()
        invalidateCachedWithItem(tplName, imgId)
        return itemEntityToDto(item)
    }

    fun getItemDetails(tplName: String, imgId: ULong): TemplateItemDto {
        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)
        val item = TemplateItemEntity.findByTemplateAndImgId(tpl, imgId.toLong())
            ?: throw TemplateExceptions.itemNotFound(tplName, imgId)

        return itemEntityToDto(item)
    }

    fun getItemImage(tplName: String, imgId: ULong): ByteArray {
        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)
        val item = TemplateItemEntity.findByTemplateAndImgId(tpl, imgId.toLong())
            ?: throw TemplateExceptions.itemNotFound(tplName, imgId)

        return try {
            Files.readAllBytes(imgStoragePath(item))
        } catch(e: Exception) {
            LOGGER.error("unable to read image for item $tplName::$imgId", e)
            throw e
        }
    }

    fun getItems(tplName: String): TemplateItemsDto {
        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)

        return TemplateItemEntity.findAllByTemplate(tpl).map(this::itemEntityToDto).let {
            TemplateItemsDto(it)
        }
    }

    fun render(tplName: String, items: Set<ULong>): ByteArray {
        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)
        val imgs = TemplateItemEntity.findAllByTemplateAndImageId(tpl, items.map { it.toLong() }.toSet())
        if(items.size != imgs.size)
            throw TemplateExceptions.itemsNotFound(tplName, items)

        return render(tpl, imgs)
    }

    fun renderAll(tplName: String): ByteArray {
        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)
        val imgs = TemplateItemEntity.findAllByTemplate(tpl)

        return render(tpl, imgs)
    }

    @CacheResult(cacheName = "template-rendered")
    protected fun render(tpl: TemplateEntity, imgs: List<TemplateItemEntity>): ByteArray {
        val canvas = MutableImage(BufferedImage(tpl.width, tpl.height, BufferedImage.TYPE_INT_ARGB))
        imgs.sortedBy { it.z }.forEach { item ->
            val img = try {
                ImmutableImage.loader().fromPath(imgStoragePath(item))
                    .toNewBufferedImage(BufferedImage.TYPE_INT_ARGB)
            } catch(e: Exception) {
                LOGGER.error("unable to read image for item ${tpl.uniqueName}::${item.imgId}", e)
                throw e
            }
            canvas.overlayInPlace(img, item.x, item.y)
        }

        return canvas.bytes(PngWriter.MaxCompression)
    }

    internal fun mkImageDir(tplName: String) {
        Files.createDirectories(imgDir.resolve(tplName))
    }

    internal fun rmImageDir(tpl: TemplateEntity) {
        TemplateItemEntity.findAllByTemplate(tpl).forEach { item ->
            try {
                Files.delete(imgStoragePath(item))
            } catch(e: Exception) {
                LOGGER.error("unable to delete image of item ${tpl.uniqueName}::$item", e)
            }
            item.delete()
        }

        try {
            Files.delete(imgDir.resolve(tpl.uniqueName))
        } catch(e: Exception) {
            LOGGER.error("unable to delete image dir of template ${tpl.uniqueName}", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun invalidateCachedWithItem(tpl: String, item: ULong) {
        renderCache.invalidateIf {
            val components = (it as CompositeCacheKey).keyElements
            assert(components.size == 2)
            if(components[0] != tpl) return@invalidateIf false
            val items = components[1] as Set<ULong>
            return@invalidateIf items.contains(item)
        }.await().indefinitely()
    }

    internal fun invalidateCachedWithTemplate(tpl: String) {
        renderCache.invalidateIf {
            val components = (it as CompositeCacheKey).keyElements
            assert(components.size == 2)
            return@invalidateIf components[0] == tpl
        }.await().indefinitely()
    }

    internal fun imgStoragePath(item: TemplateItemEntity): Path = imgDir.resolve("${item.template.uniqueName}/${item.imgId.toULong()}")

    private fun itemEntityToDto(entity: TemplateItemEntity) = TemplateItemDto(
        entity.imgId.toULong().toString(),
        entity.description,
        entity.owner.name,
        entity.width,
        entity.height,
        entity.x,
        entity.y,
        entity.z,
    )

    private fun getImageDimensions(img: ByteArray): Pair<Int, Int> {
        val parsedImg = try {
            ImmutableImage.loader().fromBytes(img)
        } catch(e: Exception) {
            throw TemplateExceptions.invalidImage("unable to decode image", e)
        }
        return Pair(parsedImg.width, parsedImg.height)
    }
}
