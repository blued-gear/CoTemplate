package apps.chocolatecakecodes.cotemplate.exception

import io.netty.handler.codec.http.HttpResponseStatus
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

internal object ExportExceptions {

    fun invalidZip(msg: String, e: Throwable? = null): Exception {
        return buildException(HttpResponseStatus.BAD_REQUEST.code(), "zip file is invalid ($msg)", e)
    }

    fun invalidMetadata(msg: String, e: Throwable? = null): Exception {
        return buildException(HttpResponseStatus.BAD_REQUEST.code(), "data.json entry is invalid ($msg)", e)
    }

    private fun buildException(status: Int, msg: String, e: Throwable?): Exception {
        val resp = Response
            .status(status)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(ExceptionBody(msg))
            .build()
        return WebApplicationException(msg, e, resp)
    }
}
