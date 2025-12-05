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
    @param:ConfigProperty(name = "cotemplate.template-max-size")
    private val templateMaxSize: Long,
) {

    companion object {

        internal const val DESCRIPTION_MAX_LEN = 1024

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
        validateImgDimensions(w, h)

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
            validateImgDimensions(w, h)

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

        return render(RenderArgs(tpl, imgs))
    }

    fun renderAll(tplName: String): ByteArray {
        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)
        val imgs = TemplateItemEntity.findAllByTemplate(tpl)

        return render(RenderArgs(tpl, imgs))
    }

    @CacheResult(cacheName = "template-rendered")
    protected fun render(args: RenderArgs): ByteArray {
        val canvas = MutableImage(BufferedImage(args.width, args.height, BufferedImage.TYPE_INT_ARGB))
        args.imgs.sortedBy { it.z }.forEach { item ->
            val img = try {
                ImmutableImage.loader().fromPath(imgStoragePath(args.tplUniqueName, item.imgId))
                    .toNewBufferedImage(BufferedImage.TYPE_INT_ARGB)
            } catch(e: Exception) {
                LOGGER.error("unable to read image for item ${args.tplUniqueName}::${item.imgId}", e)
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
            val args = it as RenderArgs
            if(args.tplUniqueName != tpl) return@invalidateIf false
            val items = args.imgs.map { it.imgId }
            return@invalidateIf items.contains(item.toLong())
        }.await().indefinitely()
    }

    internal fun invalidateCachedWithTemplate(tpl: String) {
        renderCache.invalidateIf {
            val args = it as RenderArgs
            return@invalidateIf args.tplUniqueName == tpl
        }.await().indefinitely()
    }

    internal fun imgStoragePath(item: TemplateItemEntity): Path = imgStoragePath(item.template.uniqueName, item.imgId)

    private fun imgStoragePath(tplName: String, imgId: Long) = imgDir.resolve("$tplName/${imgId.toULong()}")

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

    private fun validateImgDimensions(w: Int, h: Int) {
        if(w < 1 || h < 1 || w > templateMaxSize || h > templateMaxSize)
            throw TemplateExceptions.invalidImageSize()
    }

    protected data class RenderArgs(
        val tplUniqueName: String,
        val width: Int,
        val height: Int,
        val imgs: List<RenderImg>,
    ) {
        constructor(tpl: TemplateEntity, imgs: List<TemplateItemEntity>) : this(
            tpl.uniqueName,
            tpl.width,
            tpl.height,
            imgs.map { RenderImg(it) },
        )
    }

    protected data class RenderImg(
        val imgId: Long = 0,
        val x: Int = 0,
        val y: Int = 0,
        val z: Int = 0,
    ) {
        constructor(entity: TemplateItemEntity) : this(
            entity.imgId,
            entity.x,
            entity.y,
            entity.z,
        )
    }
}
