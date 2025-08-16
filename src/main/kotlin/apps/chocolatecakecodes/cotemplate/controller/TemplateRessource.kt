package apps.chocolatecakecodes.cotemplate.controller

import apps.chocolatecakecodes.cotemplate.auth.CotemplateSecurityIdentity
import apps.chocolatecakecodes.cotemplate.dto.*
import apps.chocolatecakecodes.cotemplate.exception.TemplateExceptions
import apps.chocolatecakecodes.cotemplate.service.TemplateItemService
import apps.chocolatecakecodes.cotemplate.service.TemplateManagementService
import apps.chocolatecakecodes.cotemplate.service.TemplateTeamService
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import jakarta.annotation.security.PermitAll
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.jboss.resteasy.reactive.*
import org.jboss.resteasy.reactive.multipart.FileUpload
import java.nio.file.Files

@Path("api/templates")
internal class TemplateRessource (
    private val templateService: TemplateManagementService,
    private val teamService: TemplateTeamService,
    private val itemService: TemplateItemService,
) {

    @GET
    @Path("/{name}")
    @PermitAll
    @Operation(
        operationId = "templateDetails",
        summary = "get details about the template"
    )
    fun templateDetails(@RestPath name: String): TemplateDetailsDto {
        return templateService.templateDetails(name)
    }

    @POST
    @ResponseStatus(RestResponse.StatusCode.CREATED)
    @PermitAll
    @Operation(
        operationId = "createTemplate",
        summary = "create a new template (+ owner account)"
    )
    fun createTemplate(@RequestBody args: TemplateCreateDto): TemplateCreatedDto {
        return templateService.createTemplate(args.name, args.width, args.height, args.teamCreatePolicy)
    }

    @PUT
    @Path("/{name}/size")
    @Authenticated
    @Operation(
        operationId = "updateTemplateSize",
        summary = "set the size of the template"
    )
    fun updateTemplateSize(@RestPath name: String, @RequestBody args: TemplateUpdateSizeDto, @Context auth: SecurityIdentity): TemplateDetailsDto {
        val identity = CotemplateSecurityIdentity.parse(auth)
        return templateService.updateTemplateSize(identity, name, args.width, args.height)
    }

    @PUT
    @Path("/{name}/teamCreatePolicy")
    @Authenticated
    @Operation(
        operationId = "updateTemplateTeamCreatePolicy",
        summary = "sets the updateTemplateTeamCreatePolicy for the template",
        description = "(only owner is permitted to do so)"
    )
    fun updateTemplateTeamCreatePolicy(@RestPath name: String, @RequestBody args: TemplateUpdateTeamCreatePolicyDto, @Context auth: SecurityIdentity): TemplateDetailsDto {
        val identity = CotemplateSecurityIdentity.parse(auth)
        return templateService.updateTemplateTeamCreatePolicy(identity, name, args.policy)
    }

    @GET
    @Path("/{name}/teams")
    @Operation(
        operationId = "getTeams",
        summary = "list all teams of the template"
    )
    fun getTeams(@RestPath name: String): TemplateTeamsDto {
        return teamService.getTeams(name)
    }

    @POST
    @Path("/{name}/teams/{team}")
    @ResponseStatus(RestResponse.StatusCode.CREATED)
    @Operation(
        operationId = "createTeam",
        summary = "creates a new team and returns its credentials"
    )
    fun createTeam(@RestPath name: String, @RestPath team: String, @Context auth: SecurityIdentity): TeamCreatedDto {
        val identity = CotemplateSecurityIdentity.parse(auth)
        return teamService.createTeam(identity, name, team)
    }

    @GET
    @Path("/{name}/items")
    @PermitAll
    @Operation(
        operationId = "getTemplateItems",
        summary = "get details of all images for the template"
    )
    fun getTemplateItems(@RestPath name: String): TemplateItemsDto {
        return itemService.getItems(name)
    }

    @GET
    @Path("/{name}/items/{id}/details")
    @PermitAll
    @Operation(
        operationId = "getTemplateItemDetails",
        summary = "get the settings of an image"
    )
    fun getTemplateItemDetails(@RestPath name: String, @RestPath("id") idStr: String): TemplateItemDto {
        return itemService.getItemDetails(name, parseItemId(idStr))
    }

    @GET
    @Path("/{name}/items/{id}/image")
    @Produces("image/png")
    @PermitAll
    @Operation(
        operationId = "getTemplateItemImage",
        summary = "get the content of an image"
    )
    fun getTemplateItemImage(@RestPath name: String, @RestPath("id") idStr: String): ByteArray {
        return itemService.getItemImage(name, parseItemId(idStr))
    }

    @POST
    @Path("/{name}/items")
    @ResponseStatus(RestResponse.StatusCode.CREATED)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Authenticated
    @Operation(
        operationId = "addTemplateItem",
        summary = "add image (must be png)"
    )
    fun addTemplateItem(
        @RestPath name: String,
        @RestForm("description") desc: String,
        @RestForm("x") x: Int,
        @RestForm("y") y: Int,
        @RestForm("z") z: Int,
        @RestForm("image") @PartType("image/png") img: FileUpload,
        @Context auth: SecurityIdentity,
    ): TemplateItemDto {
        val identity = CotemplateSecurityIdentity.parse(auth)
        return itemService.addItem(identity, name, desc, x, y, z, Files.readAllBytes(img.uploadedFile()))
    }

    @PUT
    @Path("/{name}/items/{id}/details")
    @Authenticated
    @Operation(
        operationId = "updateTemplateItemDetails",
        summary = "update settings of image"
    )
    fun updateTemplateItemDetails(
        @RestPath name: String,
        @RestPath("id") idStr: String,
        @RequestBody args: TemplateItemUpdateDto,
        @Context auth: SecurityIdentity,
    ): TemplateItemDto {
        val identity = CotemplateSecurityIdentity.parse(auth)
        return itemService.updateItemDetails(
            identity,
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
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Authenticated
    @Operation(
        operationId = "updateTemplateItemImage",
        summary = "replace the content of an image while keeping its settings"
    )
    fun updateTemplateItemImage(
        @RestPath name: String,
        @RestPath("id") idStr: String,
        @RestForm("image") @PartType("image/png") img: FileUpload,
        @Context auth: SecurityIdentity,
    ): TemplateItemDto {
        val identity = CotemplateSecurityIdentity.parse(auth)
        return itemService.updateItemImage(identity, name, parseItemId(idStr), Files.readAllBytes(img.uploadedFile()))
    }

    @DELETE
    @Path("/{name}/items/{id}")
    @ResponseStatus(RestResponse.StatusCode.NO_CONTENT)
    @Authenticated
    @Operation(
        operationId = "deleteTemplateItem",
        summary = "delete an image"
    )
    fun deleteTemplateItem(@RestPath name: String, @RestPath("id") idStr: String, @Context auth: SecurityIdentity) {
        val identity = CotemplateSecurityIdentity.parse(auth)
        itemService.deleteItem(identity, name, parseItemId(idStr))
    }

    @GET
    @Path("/{name}/template")
    @Produces("image/png")
    @Cache(noStore = true)
    @PermitAll
    @Operation(
        operationId = "renderTemplate",
        summary = "returns a png with all selected images on transparent background; if images = 'all' all images are selected"
    )
    fun renderTemplate(@RestPath name: String, @RestQuery images: String?): ByteArray {
        if(images == "all") {
            return itemService.renderAll(name)
        } else {
            val itemIds = images?.split(',')?.map(this::parseItemId)?.toSet() ?: emptySet()
            return itemService.render(name, itemIds)
        }
    }

    private fun parseItemId(idStr: String): ULong {
        return idStr.toULongOrNull()
            ?: throw TemplateExceptions.invalidParam("id must be a positive number")
    }
}
