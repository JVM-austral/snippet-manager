package manager.config

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        ex: ResponseStatusException,
        request: org.springframework.web.context.request.WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse =
            ErrorResponse(
                timestamp =
                    java.time.Instant
                        .now()
                        .toString(),
                status = ex.statusCode.value(),
                error = ex.statusCode.toString(),
                message = ex.reason ?: "No message available",
                path = request.getDescription(false).replace("uri=", ""),
            )
        return ResponseEntity(errorResponse, ex.statusCode)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: org.springframework.web.context.request.WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse =
            ErrorResponse(
                timestamp =
                    java.time.Instant
                        .now()
                        .toString(),
                status = 500,
                error = "Internal Server Error",
                message = ex.message ?: "An unexpected error occurred",
                path = request.getDescription(false).replace("uri=", ""),
            )
        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
