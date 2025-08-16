package apps.chocolatecakecodes.cotemplate.db

import apps.chocolatecakecodes.cotemplate.auth.TeamCreatePolicy
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

@Entity
@Table(
    indexes = [
        Index(columnList = "unique_name", unique = true, name = "uc_template_unique_name"),
        Index(columnList = "creation_date", unique = false),
    ],
)
internal class TemplateEntity() : PanacheEntity() {

    lateinit var creationDate: Date
    lateinit var name: String
    var width: Int = 0
    var height: Int = 0
    lateinit var teamCreatePolicy: TeamCreatePolicy
    lateinit var uniqueName: String

    constructor(name: String, width: Int, height: Int, teamCreatePolicy: TeamCreatePolicy) : this() {
        this.creationDate = Date()
        this.name = name
        this.uniqueName = uniqueName(Date(), name)
        this.width = width
        this.height = height
        this.teamCreatePolicy = teamCreatePolicy
    }

    companion object : PanacheCompanion<TemplateEntity> {

        private val dateFormat = SimpleDateFormat("yyyyMMdd")

        fun uniqueName(date: Date, name: String): String = "${dateFormat.format(date)}-$name"

        fun findByUniqueName(name: String): TemplateEntity? {
            return find("uniqueName", name).firstResult()
        }

        fun findAllOverAge(age: Instant): List<TemplateEntity> {
            return find("creationDate < ?1", age).list()
        }
    }
}
