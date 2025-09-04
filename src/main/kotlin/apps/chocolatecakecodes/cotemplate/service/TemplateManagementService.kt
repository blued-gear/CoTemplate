package apps.chocolatecakecodes.cotemplate.service

import apps.chocolatecakecodes.cotemplate.auth.CotemplateSecurityIdentity
import apps.chocolatecakecodes.cotemplate.auth.Role
import apps.chocolatecakecodes.cotemplate.auth.TeamCreatePolicy
import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import apps.chocolatecakecodes.cotemplate.db.TemplateItemEntity
import apps.chocolatecakecodes.cotemplate.db.UserEntity
import apps.chocolatecakecodes.cotemplate.dto.TemplateCreatedDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateDetailsDto
import apps.chocolatecakecodes.cotemplate.dto.TemplatesDto
import apps.chocolatecakecodes.cotemplate.exception.TemplateExceptions
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.RollbackException
import jakarta.transaction.Transactional
import org.hibernate.exception.ConstraintViolationException
import org.slf4j.LoggerFactory

@ApplicationScoped
internal class TemplateManagementService(
    private val itemService: TemplateItemService,
    private val passwordService: PasswordService,
) {

    companion object {

        internal const val MAX_TEMPLATE_DIMENSION: Int = 8192
        internal const val OWNER_USER_NAME = "owner"
        internal const val NAME_MIN_LENGTH = 4
        internal const val NAME_MAX_LENGTH = 128
        internal val NAME_REGEX = Regex("[a-zA-Z0-9_:]{$NAME_MIN_LENGTH,$NAME_MAX_LENGTH}")

        private val LOGGER = LoggerFactory.getLogger(TemplateManagementService::class.java)
    }

    fun listTemplates(ident: CotemplateSecurityIdentity): TemplatesDto {
        if(ident.isAnonymous || ident.role != Role.ADMIN)
            throw TemplateExceptions.forbidden("listing templates")

        return TemplateEntity.findAll().list().associate {
            Pair(it.uniqueName, templateEntityToDto(it))
        }.let {
            TemplatesDto(it)
        }
    }

    fun createTemplate(name: String, width: Int, height: Int, teamCreatePolicy: TeamCreatePolicy): TemplateCreatedDto {
        if(width <= 0 || height <= 0 || width > MAX_TEMPLATE_DIMENSION || height > MAX_TEMPLATE_DIMENSION)
            throw TemplateExceptions.invalidDimensions()

        if(!NAME_REGEX.matches(name))
            throw TemplateExceptions.invalidName()

        val tpl = try {
             createTemplate0(name, width, height, teamCreatePolicy)
        } catch(e: RollbackException) {
            val violatedConstraintKind = (e.cause as? ConstraintViolationException)?.kind
            if(violatedConstraintKind == ConstraintViolationException.ConstraintKind.UNIQUE) {
                throw TemplateExceptions.templateAlreadyExists(name)
            } else {
                throw e
            }
        }

        try {
            itemService.mkImageDir(tpl.uniqueName)
        } catch (e: Exception) {
            LOGGER.error("unable to create img-dir for template ${tpl.uniqueName}", e)
            throw e
        }

        return tpl
    }

    @Transactional
    protected fun createTemplate0(name: String, width: Int, height: Int, teamCreatePolicy: TeamCreatePolicy): TemplateCreatedDto {
        val entity = TemplateEntity(name, width, height, teamCreatePolicy)

        val (ownerPass, ownerPassEnc) = passwordService.randomPassword()
        val owner = UserEntity().apply {
            this.template = entity
            this.role = Role.TEMPLATE_OWNER
            this.name = OWNER_USER_NAME
            this.pass = ownerPassEnc
        }

        entity.persist()
        owner.persist()

        return TemplateCreatedDto(entity.uniqueName, owner.name, ownerPass)
    }

    fun templateDetails(name: String): TemplateDetailsDto {
        return TemplateEntity.findByUniqueName(name)?.let {
            templateEntityToDto(it)
        } ?: throw TemplateExceptions.templateNotFound(name)
    }

    @Transactional
    fun updateTemplateSize(ident: CotemplateSecurityIdentity, tplName: String, width: Int, height: Int): TemplateDetailsDto {
        checkTemplateAccess("modifying template settings", ident, tplName)
        if(width <= 0 || height <= 0 || width > MAX_TEMPLATE_DIMENSION || height > MAX_TEMPLATE_DIMENSION)
            throw TemplateExceptions.invalidDimensions()

        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)

        tpl.width = width
        tpl.height = height

        tpl.persist()
        itemService.invalidateCachedWithTemplate(tplName)

        return templateEntityToDto(tpl)
    }

    @Transactional
    fun updateTemplateTeamCreatePolicy(ident: CotemplateSecurityIdentity, tplName: String, policy: TeamCreatePolicy): TemplateDetailsDto {
        checkTemplateAccess("modifying template settings", ident, tplName)

        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)

        tpl.teamCreatePolicy = policy
        tpl.persist()

        return templateEntityToDto(tpl)
    }

    fun deleteTemplate(ident: CotemplateSecurityIdentity, tplName: String) {
        if(ident.isAnonymous || ident.role != Role.ADMIN)
            throw TemplateExceptions.forbidden("listing templates")

        deleteTemplateInternal(tplName)
    }

    @Transactional
    internal fun deleteTemplateInternal(tplName: String) {
        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)

        itemService.rmImageDir(tpl)

        UserEntity.findAllByTemplate(tpl).forEach { user ->
            user.delete()
        }

        tpl.delete()
        itemService.invalidateCachedWithTemplate(tplName)
    }

    internal fun checkTemplateAccess(action: String, ident: CotemplateSecurityIdentity, tplName: String) {
        if(ident.isAnonymous)
            throw TemplateExceptions.forbidden(action)
        if(ident.role == Role.ADMIN)
            return
        if(ident.template != tplName)
            throw TemplateExceptions.forbidden(action)
        if(ident.role != Role.TEMPLATE_OWNER)
            throw TemplateExceptions.forbidden(action)
    }

    internal fun checkTeamAccess(action: String, ident: CotemplateSecurityIdentity, tplName: String) {
        if(ident.isAnonymous)
            throw TemplateExceptions.forbidden(action)
        if(ident.role == Role.ADMIN)
            return
        if(ident.template != tplName)
            throw TemplateExceptions.forbidden(action)
        if(ident.role != Role.TEMPLATE_OWNER && ident.role != Role.TEMPLATE_TEAM)
            throw TemplateExceptions.forbidden(action)
    }

    internal fun checkItemAccess(action: String, ident: CotemplateSecurityIdentity, tplName: String, itemOwner: UserEntity) {
        checkTeamAccess(action, ident, tplName)
        if(ident.role == Role.ADMIN)
            return
        if(ident.role != Role.TEMPLATE_OWNER && ident.userId != itemOwner.id)
            throw TemplateExceptions.forbidden(action)
    }

    private fun templateEntityToDto(tpl: TemplateEntity): TemplateDetailsDto {
        val itemCount = TemplateItemEntity.countByTemplate(tpl)
        return TemplateDetailsDto(
            tpl.name,
            tpl.creationDate.time,
            tpl.teamCreatePolicy,
            tpl.width,
            tpl.height,
            itemCount,
        )
    }
}
