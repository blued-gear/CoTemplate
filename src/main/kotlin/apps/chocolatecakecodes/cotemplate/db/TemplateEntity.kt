package apps.chocolatecakecodes.cotemplate.db

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.text.SimpleDateFormat
import java.util.*

@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(columnNames = ["uniqueName"], name = "uc_unique_name")],
    indexes = [
        Index(columnList = "uniqueName", unique = true),
        Index(columnList = "creationDate", unique = false),
    ],
)
internal class TemplateEntity : PanacheEntity() {

    lateinit var creationDate: Date
    lateinit var name: String
    var width: Int = 0
    var height: Int = 0
    lateinit var uniqueName: String

    companion object : PanacheCompanion<TemplateEntity> {

        private val dateFormat = SimpleDateFormat("yyyyMMdd")

        fun uniqueName(date: Date, name: String): String = "${dateFormat.format(date)}-$name"

        fun findByUniqueName(name: String): TemplateEntity? {
            return find("uniqueName", name).firstResult()
        }
    }
}
