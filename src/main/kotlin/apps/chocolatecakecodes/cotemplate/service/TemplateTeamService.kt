package apps.chocolatecakecodes.cotemplate.service

import apps.chocolatecakecodes.cotemplate.auth.AuthLoginProvider
import apps.chocolatecakecodes.cotemplate.auth.CotemplateSecurityIdentity
import apps.chocolatecakecodes.cotemplate.auth.Role
import apps.chocolatecakecodes.cotemplate.auth.TeamCreatePolicy
import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import apps.chocolatecakecodes.cotemplate.db.UserEntity
import apps.chocolatecakecodes.cotemplate.dto.TeamCreatedDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateTeamsDto
import apps.chocolatecakecodes.cotemplate.exception.TemplateExceptions
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.RollbackException
import jakarta.transaction.Transactional
import org.hibernate.exception.ConstraintViolationException
import org.slf4j.LoggerFactory

@ApplicationScoped
internal class TemplateTeamService(
    private val mngService: TemplateManagementService,
    private val passwordService: PasswordService,
) {

    companion object {

        internal const val NAME_MIN_LENGTH = 1
        internal const val NAME_MAX_LENGTH = 128
        internal val TEAM_REGEX = Regex("[a-zA-Z0-9_:]{$NAME_MIN_LENGTH,$NAME_MAX_LENGTH}")

        private val LOGGER = LoggerFactory.getLogger(TemplateTeamService::class.java)
    }

    fun createTeam(ident: CotemplateSecurityIdentity, tplName: String, teamName: String): TeamCreatedDto {
        if(!TEAM_REGEX.matches(teamName))
            throw TemplateExceptions.invalidName()
        if(teamName == AuthLoginProvider.ADMIN_USER_NAME)
            throw TemplateExceptions.invalidName("username may not be '${AuthLoginProvider.ADMIN_USER_NAME}'")

        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)

        when(tpl.teamCreatePolicy) {
            TeamCreatePolicy.OWNER -> {
                mngService.checkTemplateAccess("creating teams", ident, tplName)
            }
            TeamCreatePolicy.EVERYONE -> {
                if(!ident.isAnonymous) {
                    mngService.checkTeamAccess("creating teams", ident, tplName)
                }
            }
        }

        try {
            return createTeam0(tpl, teamName)
        } catch(e: RollbackException) {
            val violatedConstraintKind = (e.cause as? ConstraintViolationException)?.kind
            if(violatedConstraintKind == ConstraintViolationException.ConstraintKind.UNIQUE) {
                throw TemplateExceptions.teamAlreadyExists(teamName, tplName)
            } else {
                throw e
            }
        }
    }

    @Transactional
    protected fun createTeam0(tpl: TemplateEntity, name: String): TeamCreatedDto {
        val (pass, passEnc) = passwordService.randomPassword()
        UserEntity().apply {
            this.template = tpl
            this.role = Role.TEMPLATE_TEAM
            this.name = name
            this.pass = passEnc
        }.also {
            it.persist()
        }

        return TeamCreatedDto(tpl.uniqueName, name, pass)
    }

    fun getTeams(tplName: String): TemplateTeamsDto {
        val tpl = TemplateEntity.findByUniqueName(tplName)
            ?: throw TemplateExceptions.templateNotFound(tplName)

        return UserEntity.findAllByTemplate(tpl).filter {
            it.role == Role.TEMPLATE_TEAM
        }.map {
            it.name
        }.let {
            TemplateTeamsDto(it)
        }
    }
}
