package apps.chocolatecakecodes.cotemplate.service

import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

@ApplicationScoped
internal class TemplateEvictionService(
    private val templateService: TemplateService,
    @param:ConfigProperty(name = "cotemplate.template-max-age")
    private val templateMaxAge: Duration,
) {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TemplateEvictionService::class.java)
    }

    @Scheduled(cron = "0 0 1 1/1 * ?")
    fun runEviction() {
        val maxAge = Instant.now().minus(templateMaxAge)

        TemplateEntity.findAllOverAge(maxAge).forEach { tpl ->
            LOGGER.info("evicting ${tpl.uniqueName}")
            templateService.deleteTemplate(tpl.uniqueName)
        }
    }
}
