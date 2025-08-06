package apps.chocolatecakecodes.cotemplate.service

import apps.chocolatecakecodes.cotemplate.auth.Role
import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import apps.chocolatecakecodes.cotemplate.db.TemplateItemEntity
import apps.chocolatecakecodes.cotemplate.db.UserEntity
import apps.chocolatecakecodes.cotemplate.dto.TemplateCreatedDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateDetailsDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateItemDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateItemsDto
import apps.chocolatecakecodes.cotemplate.exception.TemplateExceptions
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.MutableImage
import com.sksamuel.scrimage.nio.PngWriter
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.RollbackException
import jakarta.transaction.Transactional
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.hibernate.exception.ConstraintViolationException
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

@ApplicationScoped
internal class TemplateService(
    private val passwordService: PasswordService,
    @ConfigProperty(name = "cotemplate.image-storage")
    imgDirPath: String,
) {

    //TODO all operations must check if the user has permissions

    companion object {
        internal const val MAX_TEMPLATE_DIMENSION: Int = 8192
        internal val NAME_REGEX = Regex("[a-zA-Z0-9_:]{4,128}")

        private val LOGGER = LoggerFactory.getLogger(TemplateService::class.java)
    }

    private val imgDir: Path

    init {
        if(imgDirPath.isEmpty())
            throw IllegalArgumentException("COTEMPLATE_IMG_STOARGE was unset")
        imgDir = try {
            Files.createDirectories(Path.of(imgDirPath))
        } catch (e: IOException) {
            throw IllegalArgumentException("COTEMPLATE_IMG_STOARGE could not be accessed / created", e)
        }
        LOGGER.info("using imgDir: $imgDir")
    }

    fun createTemplate(name: String, width: Int, height: Int): TemplateCreatedDto {
        if(width <= 0 || height <= 0 || width > MAX_TEMPLATE_DIMENSION || height > MAX_TEMPLATE_DIMENSION)
            throw TemplateExceptions.invalidDimensions()

        if(!NAME_REGEX.matches(name))
            throw TemplateExceptions.invalidName()

        val tpl = try {
             createTemplate0(name, width, height)
        } catch(e: RollbackException) {
            val violatedConstraint = (e.cause as? ConstraintViolationException)?.constraintName
            if(violatedConstraint != null && violatedConstraint.contains("uc_unique_name", true)) {
                throw TemplateExceptions.templateAlreadyExists(name)
            } else {
                throw e
            }
        }

        try {
            Files.createDirectories(imgDir.resolve(tpl.uniqueName))
        } catch (e: Exception) {
            LOGGER.error("unable to create img-dir for template ${tpl.uniqueName}", e)
            throw e
        }

        return tpl
    }

    @Transactional
    protected fun createTemplate0(name: String, width: Int, height: Int): TemplateCreatedDto {
        val entity = TemplateEntity().apply {
            this.creationDate = Date()
            this.name = name
            this.uniqueName = TemplateEntity.uniqueName(Date(), name)
            this.width = width
            this.height = height
        }

        val (ownerPass, ownerPassEnc) = randomPassword()
        val owner = UserEntity().apply {
            this.template = entity
            this.role = Role.TEMPLATE_OWNER
            this.name = "owner"
            this.pass = ownerPassEnc
        }

        entity.persist()
        owner.persist()

        return TemplateCreatedDto(entity.uniqueName, owner.name, ownerPass)
    }

    fun templateDetails(name: String): TemplateDetailsDto {
        return TemplateEntity.findByUniqueName(name)?.let {
            TemplateDetailsDto(
                it.name,
                it.creationDate.time,
                it.width,
                it.height,
                0,//TODO
            )
        } ?: throw TemplateExceptions.templateNotFound(name)
    }

    fun addItem(tplName: String, desc: String, x: Int, y: Int, z: Int, img: ByteArray): TemplateItemDto {
        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)

        val (w, h) = getImageDimensions(img)
        val entity = addItemEntity(tpl, desc, x, y, z, w, h)

        try {
            Files.write(imgStoragePath(entity), img, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        } catch(e: Exception) {
            LOGGER.error("unable to store image for item $tplName::${entity.imgId}", e)
            entity.delete()
            throw e
        }

        return itemEntityToDto(entity)
    }

    @Transactional
    fun deleteItem(tplName: String, imgId: ULong) {
        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)
        val item = TemplateItemEntity.findByTemplateAndImgId(tpl, imgId.toLong())
            ?: throw TemplateExceptions.itemNotFound(tplName, imgId)

        try {
            Files.delete(imgStoragePath(item))
        } catch(e: Exception) {
            LOGGER.error("exception while deleting image for item $tplName::$imgId", e)
        }

        item.delete()
    }

    @Transactional
    fun updateItemDetails(tplName: String, imgId: ULong, desc: String?, x: Int?, y: Int?, z: Int?): TemplateItemDto {
        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)
        val item = TemplateItemEntity.findByTemplateAndImgId(tpl, imgId.toLong())
            ?: throw TemplateExceptions.itemNotFound(tplName, imgId)

        if(desc != null) item.description = desc
        if(x != null) item.x = x
        if(y != null) item.y = y
        if(z != null) item.z = z

        item.persist()
        return itemEntityToDto(item)
    }

    @Transactional
    fun updateItemImage(tplName: String, imgId: ULong, img: ByteArray): TemplateItemDto {
        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)
        val item = TemplateItemEntity.findByTemplateAndImgId(tpl, imgId.toLong())
            ?: throw TemplateExceptions.itemNotFound(tplName, imgId)

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

        //TODO access check

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

        val canvas = MutableImage(BufferedImage(tpl.width, tpl.height, BufferedImage.TYPE_INT_ARGB))
        imgs.sortedBy { it.z }.forEach { item ->
            val img = try {
                ImmutableImage.loader().fromPath(imgStoragePath(item))
                    .toNewBufferedImage(BufferedImage.TYPE_INT_ARGB)
            } catch(e: Exception) {
                LOGGER.error("unable to read image for item $tplName::${item.imgId}", e)
                throw e
            }
            canvas.overlayInPlace(img, item.x, item.y)
        }

        return canvas.bytes(PngWriter.MaxCompression)
    }

    @Transactional
    protected fun addItemEntity(tpl: TemplateEntity, desc: String, x: Int, y: Int, z: Int, w: Int, h: Int): TemplateItemEntity {
        return TemplateItemEntity(tpl, desc, x, y, z, w, h)
            .also { it.persist() }
    }

    private fun randomPassword(): Pair<String, String> {
        val pass = passwordService.generateRandomPassword()
        val passEnc = passwordService.hashPassword(pass)
        return Pair(pass, passEnc)
    }

    private fun imgStoragePath(item: TemplateItemEntity): Path = imgDir.resolve("${item.template.uniqueName}/${item.imgId.toULong()}")

    private fun itemEntityToDto(entity: TemplateItemEntity) = TemplateItemDto(
        entity.imgId.toULong().toString(),
        entity.description,
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
