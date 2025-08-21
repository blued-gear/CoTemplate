package apps.chocolatecakecodes.cotemplate.exception

import apps.chocolatecakecodes.cotemplate.service.TemplateManagementService
import io.netty.handler.codec.http.HttpResponseStatus
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

internal object TemplateExceptions {

    fun templateAlreadyExists(name: String, e: Throwable? = null): Exception {
        val msg = "template with name '$name' already exists"
        return WebApplicationException(msg, e, buildResp(HttpResponseStatus.CONFLICT.code(), msg))
    }

    fun teamAlreadyExists(name: String, template: String, e: Throwable? = null): Exception {
        val msg = "team with name '$name' already exists for template '$template'"
        return WebApplicationException(msg, e, buildResp(HttpResponseStatus.CONFLICT.code(), msg))
    }

    fun invalidDimensions(): Exception {
        val msg = "template dimensions must be > 0 and <= ${TemplateManagementService.MAX_TEMPLATE_DIMENSION}"
        return WebApplicationException(msg, null, buildResp(HttpResponseStatus.BAD_REQUEST.code(), msg))
    }

    fun invalidName(msg: String? = null): Exception {
        val msg = if(msg != null) "invalid name ($msg)" else "invalid name"
        return WebApplicationException(msg, null, buildResp(HttpResponseStatus.BAD_REQUEST.code(), msg))
    }

    fun templateNotFound(name: String): Exception {
        val msg = "template with name '$name' does not exist"
        return WebApplicationException(msg, null, buildResp(HttpResponseStatus.NOT_FOUND.code(), msg))
    }

    fun itemNotFound(tpl: String, id: ULong): Exception {
        val msg = "item with id $id does not exist in template '$tpl'"
        return WebApplicationException(msg, null, buildResp(HttpResponseStatus.NOT_FOUND.code(), msg))
    }

    fun itemsNotFound(tpl: String, ids: Set<ULong>): Exception {
        val msg = "at least one item of {${ids.joinToString(", ")}} does not exist in template '$tpl'"
        return WebApplicationException(msg, null, buildResp(HttpResponseStatus.NOT_FOUND.code(), msg))
    }

    fun invalidImage(message: String, cause: Throwable? = null): Exception {
        val msg = "image is invalid: $message"
        return WebApplicationException(msg, cause, buildResp(HttpResponseStatus.BAD_REQUEST.code(), msg))
    }

    fun invalidParam(message: String): Exception {
        return WebApplicationException(message, null, buildResp(HttpResponseStatus.BAD_REQUEST.code(), message))
    }

    fun forbidden(action: String): Exception {
        val msg = "$action is not permitted for you"
        return WebApplicationException(msg, null, buildResp(HttpResponseStatus.FORBIDDEN.code(), msg))
    }

    private fun buildResp(status: Int, msg: String) = Response
        .status(status)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(ExceptionBody(msg))
        .build()
}
