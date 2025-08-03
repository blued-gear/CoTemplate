package apps.chocolatecakecodes.cotemplate.service

import apps.chocolatecakecodes.cotemplate.auth.Role
import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import apps.chocolatecakecodes.cotemplate.db.UserEntity
import apps.chocolatecakecodes.cotemplate.dto.TemplateCreatedDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateDetailsDto
import apps.chocolatecakecodes.cotemplate.exception.TemplateExceptions
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.RollbackException
import jakarta.transaction.Transactional
import org.hibernate.exception.ConstraintViolationException
import java.util.*

@ApplicationScoped
internal class TemplateService(
    private val passwordService: PasswordService,
) {

    companion object {
        internal const val MAX_TEMPLATE_DIMENSION: Int = 8192
        internal val NAME_REGEX = Regex("[a-zA-Z0-9_:]{4,128}")
    }

    fun createTemplate(name: String, width: Int, height: Int): TemplateCreatedDto {
        if(width <= 0 || height <= 0 || width > MAX_TEMPLATE_DIMENSION || height > MAX_TEMPLATE_DIMENSION)
            throw TemplateExceptions.invalidDimensions()

        if(!NAME_REGEX.matches(name))
            throw TemplateExceptions.invalidName()

        try {
            return createTemplate0(name, width, height)
        } catch(e: RollbackException) {
            val violatedConstraint = (e.cause as? ConstraintViolationException)?.constraintName
            if(violatedConstraint != null && violatedConstraint.contains("uc_unique_name", true)) {
                throw TemplateExceptions.templateAlreadyExists(name)
            } else {
                throw e
            }
        }
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

    private fun randomPassword(): Pair<String, String> {
        val pass = passwordService.generateRandomPassword()
        val passEnc = passwordService.hashPassword(pass)
        return Pair(pass, passEnc)
    }
}
