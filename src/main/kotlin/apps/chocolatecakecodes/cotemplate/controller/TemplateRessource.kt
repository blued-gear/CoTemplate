package apps.chocolatecakecodes.cotemplate.controller

import apps.chocolatecakecodes.cotemplate.dto.TemplateCreateDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateCreatedDto
import apps.chocolatecakecodes.cotemplate.service.TemplateService
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.jboss.resteasy.reactive.ResponseStatus

@Path("templates")
internal class TemplateRessource (
    private val templateService: TemplateService,
) {

    @POST
    @ResponseStatus(201)
    fun createTemplate(@RequestBody args: TemplateCreateDto): TemplateCreatedDto {
        return templateService.createTemplate(args.name, args.width, args.height)
    }
}
