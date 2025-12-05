package apps.chocolatecakecodes.cotemplate.controller

import jakarta.annotation.security.PermitAll
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.openapi.annotations.Operation
import java.time.Duration

@Path("api/misc")
internal class MiscellaneousRessource(
    @param:ConfigProperty(name = "cotemplate.template-max-age")
    private val templateMaxAge: Duration,
    @param:ConfigProperty(name = "cotemplate.template-max-size")
    private val templateMaxSize: Long,
) {

    @GET
    @Path("/maxTemplateAge")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(
        operationId = "maxTemplateAge",
        summary = "Returns the maximum age of a template, before it gets deleted (in milliseconds)"
    )
    fun maxTemplateAge(): Long {
        return templateMaxAge.toMillis()
    }

    @GET
    @Path("/maxTemplateSize")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(
        operationId = "maxTemplateSize",
        summary = "Returns the maximum size of a template (longest side in px)"
    )
    fun maxTemplateSize(): Long {
        return templateMaxSize
    }
}
