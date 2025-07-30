package apps.chocolatecakecodes.cotemplate.service

import apps.chocolatecakecodes.cotemplate.auth.Role
import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import apps.chocolatecakecodes.cotemplate.db.UserEntity
import apps.chocolatecakecodes.cotemplate.dto.TemplateCreatedDto
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.util.*

@ApplicationScoped
internal class TemplateService(
    private val passwordService: PasswordService,
) {

    fun createTemplate(name: String, width: Int, height: Int): TemplateCreatedDto {
        try {
            return createTemplate0(name, width, height)
        } catch(e: Exception) {
            //TODO catch unique constraint violation and transform to http-conflict
            e.printStackTrace()
            throw e
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

    private fun randomPassword(): Pair<String, String> {
        val pass = passwordService.generateRandomPassword()
        val passEnc = passwordService.hashPassword(pass)
        return Pair(pass, passEnc)
    }
}
