package apps.chocolatecakecodes.cotemplate.controller

import apps.chocolatecakecodes.cotemplate.dto.TemplateCreateDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateCreatedDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateDetailsDto
import apps.chocolatecakecodes.cotemplate.service.TemplateService
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.jboss.resteasy.reactive.ResponseStatus

@Path("templates")
internal class TemplateRessource (
    private val templateService: TemplateService,
) {

    @GET
    @Path("/{name}")
    fun templateDetails(@PathParam("name") name: String): TemplateDetailsDto {
        return templateService.templateDetails(name)
    }

    @POST
    @ResponseStatus(201)
    fun createTemplate(@RequestBody args: TemplateCreateDto): TemplateCreatedDto {
        return templateService.createTemplate(args.name, args.width, args.height)
    }
}
