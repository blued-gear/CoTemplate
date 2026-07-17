package apps.chocolatecakecodes.cotemplate.configuration

import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.core.UriInfo
import jakarta.ws.rs.ext.Provider

@Provider
internal class CorsFilter : ContainerResponseFilter {

    override fun filter(req: ContainerRequestContext, resp: ContainerResponseContext) {
        val origin = req.getHeaderString("Origin")
        if(origin.isNullOrBlank())
            return

        if(applyCorsForTemplateImage(resp, origin, req.uriInfo))
            return
    }

    private fun applyCorsForTemplateImage(resp: ContainerResponseContext, origin: String, uri: UriInfo): Boolean {
        val tplName = uri.pathParameters["name"]?.getOrNull(0) ?: return false
        if(uri.path != "/api/templates/$tplName/template") return false

        resp.headers.apply {
            putSingle("Access-Control-Allow-Origin", origin)
            putSingle("Access-Control-Allow-Credentials", "false")
            putSingle("Access-Control-Allow-Methods", "GET")
        }
        return true
    }
}
