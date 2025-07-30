package apps.chocolatecakecodes.cotemplate.db

import apps.chocolatecakecodes.cotemplate.auth.Role
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
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

    companion object : PanacheCompanion<UserEntity> {
        @JvmStatic
        internal fun findByTemplateAndName(template: TemplateEntity, name: String): UserEntity? {
            return find("template = ?1 and name = ?2", template, name).firstResult()
        }
    }
}
