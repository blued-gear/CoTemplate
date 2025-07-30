package apps.chocolatecakecodes.cotemplate.db

import apps.chocolatecakecodes.cotemplate.auth.Role
import io.quarkus.hibernate.orm.panache.PanacheEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne

@Entity
internal class UserEntity : PanacheEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    lateinit var template: TemplateEntity
    lateinit var name: String
    lateinit var pass: String
    lateinit var role: Role

    companion object {
        @JvmStatic
        internal fun findByTemplateAndName(template: TemplateEntity, name: String): UserEntity? {
            return find<UserEntity>("template = ? and name = ?", template, name).firstResult()
        }
    }
}
