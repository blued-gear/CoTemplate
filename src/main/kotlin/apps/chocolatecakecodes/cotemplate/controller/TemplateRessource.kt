package apps.chocolatecakecodes.cotemplate.controller

import apps.chocolatecakecodes.cotemplate.dto.*
import apps.chocolatecakecodes.cotemplate.exception.TemplateExceptions
import apps.chocolatecakecodes.cotemplate.service.TemplateService
import jakarta.ws.rs.*
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.jboss.resteasy.reactive.PartType
import org.jboss.resteasy.reactive.ResponseStatus
import org.jboss.resteasy.reactive.RestForm
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.multipart.FileUpload
import java.nio.file.Files

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
    @ResponseStatus(RestResponse.StatusCode.CREATED)
    fun createTemplate(@RequestBody args: TemplateCreateDto): TemplateCreatedDto {
        return templateService.createTemplate(args.name, args.width, args.height)
    }

    @GET
    @Path("/{name}/items")
    fun getTemplateItems(@PathParam("name") name: String): TemplateItemsDto {
        return templateService.getItems(name)
    }

    @GET
    @Path("/{name}/items/{id}/details")
    fun getTemplateItemDetails(@PathParam("name") name: String, @PathParam("id") idStr: String): TemplateItemDto {
        return templateService.getItemDetails(name, parseItemId(idStr))
    }

    @GET
    @Path("/{name}/items/{id}/image")
    @Produces("image/png")
    fun getTemplateItemImage(@PathParam("name") name: String, @PathParam("id") idStr: String): ByteArray {
        return templateService.getItemImage(name, parseItemId(idStr))
    }

    @POST
    @Path("/{name}/items")
    @ResponseStatus(RestResponse.StatusCode.CREATED)
    fun addTemplateItem(
        @PathParam("name") name: String,
        @RestForm("description") desc: String,
        @RestForm("x") x: Int,
        @RestForm("y") y: Int,
        @RestForm("z") z: Int,
        @RestForm("image") @PartType("image/png") img: FileUpload,
    ): TemplateItemDto {
        return templateService.addItem(name, desc, x, y, z, Files.readAllBytes(img.uploadedFile()))
    }

    @PUT
    @Path("/{name}/items/{id}/details")
    fun updateTemplateItemDetails(@PathParam("name") name: String, @PathParam("id") idStr: String, @RequestBody args: TemplateItemUpdateDto): TemplateItemDto {
        return templateService.updateItemDetails(
            name,
            parseItemId(idStr),
            args.description,
            args.x,
            args.y,
            args.z,
        )
    }

    @PUT
    @Path("/{name}/items/{id}/image")
    fun updateTemplateItemImage(@PathParam("name") name: String, @PathParam("id") idStr: String,
                                @RestForm("image") @PartType("image/png") img: FileUpload): TemplateItemDto {
        return templateService.updateItemImage(name, parseItemId(idStr), Files.readAllBytes(img.uploadedFile()))
    }

    @DELETE
    @Path("/{name}/items/{id}")
    @ResponseStatus(RestResponse.StatusCode.NO_CONTENT)
    fun deleteTemplateItem(@PathParam("name") name: String, @PathParam("id") idStr: String) {
        templateService.deleteItem(name, parseItemId(idStr))
    }

    private fun parseItemId(idStr: String): ULong {
        return idStr.toULongOrNull()
            ?: throw TemplateExceptions.invalidParam("id must be a positive number")
    }
}
