package apps.chocolatecakecodes.cotemplate.db

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.*

@Entity
@Table(
    indexes = [Index(columnList = "name, template_id", unique = true, name = "uc_user_unique_name")]
)
internal class UserEntity : PanacheEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    lateinit var template: TemplateEntity
    lateinit var name: String
    lateinit var pass: String
    lateinit var role: String

    companion object : PanacheCompanion<UserEntity> {

        internal fun findByTemplateAndName(template: TemplateEntity, name: String): UserEntity? {
            return find("template = ?1 and name = ?2", template, name).firstResult()
        }

        internal fun findAllByTemplate(template: TemplateEntity): List<UserEntity> {
            return find("template = ?1", template).list()
        }
    }
}
