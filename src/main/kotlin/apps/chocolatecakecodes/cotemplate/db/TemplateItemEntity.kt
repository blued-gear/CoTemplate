package apps.chocolatecakecodes.cotemplate.db

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.*
import java.security.SecureRandom

@Entity
@Table(
    indexes = [Index(columnList = "img_id, template_id", unique = true, name = "uc_item_tpl_img")]
)
internal class TemplateItemEntity() : PanacheEntity() {

    var imgId: Long = 0
    @ManyToOne(fetch = FetchType.LAZY)
    lateinit var template: TemplateEntity
    @ManyToOne
    lateinit var owner: UserEntity
    lateinit var description: String
    var x: Int = 0
    var y: Int = 0
    var z: Int = 0
    var width: Int = 0
    var height: Int = 0

    constructor(template: TemplateEntity, owner: UserEntity, description: String, x: Int, y: Int, z: Int, w: Int, h: Int) : this() {
        this.template = template
        this.owner = owner
        this.description = description
        this.x = x
        this.y = y
        this.z = z
        this.width = w
        this.height = h

        var imgId: Long = 0
        while(imgId == 0L) imgId = rng.nextLong()
        this.imgId = imgId
    }

    companion object : PanacheCompanion<TemplateItemEntity> {

        private val rng = SecureRandom()

        fun findByTemplateAndImgId(template: TemplateEntity, imgId: Long): TemplateItemEntity? {
            return find("template = ?1 and imgId = ?2", template, imgId).firstResult()
        }

        fun findAllByTemplate(template: TemplateEntity): List<TemplateItemEntity> {
            return find("template = ?1", template).list()
        }

        fun findAllByTemplateAndImageId(template: TemplateEntity, imgIds: Set<Long>): List<TemplateItemEntity> {
            return find("template = ?1 and imgId in ?2", template, imgIds).list()
        }

        fun countByTemplate(template: TemplateEntity): Int {
            return find("template = ?1", template).count().toInt()
        }
    }
}
