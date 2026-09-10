package com.nexafarma.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Centraliza las excepciones de negocio y validacion de la API en JSON uniforme. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ResourceNotFoundException.class, RecursoNoEncontradoException.class})
    public ResponseEntity<ApiError> recursoNoEncontrado(RuntimeException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler({DuplicateResourceException.class, RecursoDuplicadoException.class, ResourceInUseException.class})
    public ResponseEntity<ApiError> conflicto(RuntimeException ex, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler({ReglaNegocioException.class, EstadoInvalidoException.class,
            FormulaMedicaInvalidaException.class, StockInsuficienteException.class})
    public ResponseEntity<ApiError> reglaNegocio(RuntimeException ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.put(error.getField(), error.getDefaultMessage());
        }
        return responder(HttpStatus.BAD_REQUEST, "Hay campos invalidos en la solicitud", request, campos);
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> solicitudInvalida(Exception ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "La solicitud contiene datos invalidos", request, Map.of());
    }

    private ResponseEntity<ApiError> responder(HttpStatus estado, String mensaje, HttpServletRequest request,
                                                Map<String, String> campos) {
        ApiError error = new ApiError(LocalDateTime.now(), estado.value(), estado.getReasonPhrase(),
                mensaje, request.getRequestURI(), campos);
        return ResponseEntity.status(estado).body(error);
    }
}
