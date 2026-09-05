package com.usedmarket.common.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.associate { fieldError ->
            fieldError.field to (fieldError.defaultMessage ?: "Invalid value")
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(status = HttpStatus.BAD_REQUEST.value(), message = "Validation failed", errors = errors)
        )
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.NOT_FOUND, ex.message ?: "Resource not found")

    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicate(ex: DuplicateResourceException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.CONFLICT, ex.message ?: "Resource already exists")

    @ExceptionHandler(UnauthorizedException::class, InvalidCredentialsException::class)
    fun handleUnauthorized(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.UNAUTHORIZED, ex.message ?: "Unauthorized")

    @ExceptionHandler(ForbiddenException::class, AccessDeniedException::class)
    fun handleForbidden(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.FORBIDDEN, ex.message ?: "Access denied")

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(ex: BadRequestException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.BAD_REQUEST, ex.message ?: "Bad request")

    @ExceptionHandler(InsufficientStockException::class, InvalidOrderStateException::class)
    fun handleConflict(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.CONFLICT, ex.message ?: "Conflict")

    @ExceptionHandler(PaymentException::class)
    fun handlePayment(ex: PaymentException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.PAYMENT_REQUIRED, ex.message ?: "Payment failed")

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception", ex)
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred")
    }

    private fun respond(status: HttpStatus, message: String): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(ErrorResponse(status = status.value(), message = message))
}
