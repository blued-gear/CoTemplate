package apps.chocolatecakecodes.cotemplate.util

import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import apps.chocolatecakecodes.cotemplate.db.TemplateItemEntity
import apps.chocolatecakecodes.cotemplate.db.UserEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.control.ActivateRequestContext
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.nio.file.Files
import java.nio.file.Path

@ApplicationScoped
internal class CleanupHelper(
    @Inject
    @param:ConfigProperty(name = "cotemplate.image-storage")
    val imgDirPath: String,
) {

    @ActivateRequestContext
    @Transactional
    fun cleanDb() {
        TemplateItemEntity.deleteAll()
        UserEntity.deleteAll()
        TemplateEntity.deleteAll()
    }

    fun cleanImages() {
        Files.list(Path.of(imgDirPath)).forEach {
            it.toFile().deleteRecursively()
        }
    }

}
