package com.DannyU.auction_engine.exception;

import com.DannyU.auction_engine.service.ErrorMonitorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.ErrorResponse;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorMonitorService errorMonitorService;

    // Anything unexpected: recorded by the error monitor and answered with a generic 500 in the usual format.
    // Spring exceptions that already carry a status (404, 405, missing parameter...) keep it and are not recorded;
    // a client that went away (closed SSE stream, broken pipe) gets no answer at all.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex, HttpServletRequest request) throws Exception {
        if (ex instanceof ErrorResponse errorResponse) {
            String detail = errorResponse.getBody().getDetail();
            return ResponseEntity.status(errorResponse.getStatusCode())
                    .body(Map.of("eroare", detail != null ? detail : "Cerere invalida!"));
        }
        if (ex instanceof AsyncRequestNotUsableException || ex instanceof java.io.IOException) {
            throw ex;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication == null || authentication instanceof AnonymousAuthenticationToken ? null : authentication.getName();
        errorMonitorService.recordServerError(ex, request.getMethod() + " " + request.getRequestURI(), username,
                request.getHeader("User-Agent"));
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("eroare", "A aparut o eroare neasteptata. Am inregistrat-o si o vom rezolva."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return badRequest(ex.getMessage());
    }

    // Datele trimise nu respecta regulile de validare (@NotBlank, @Positive etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        String mesaj = ex.getBindingResult().getFieldErrors().stream()
                .map(eroare -> eroare.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining(" "));
        return badRequest(mesaj);
    }

    // JSON invalid sau tip gresit (ex: text in loc de numar)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        return badRequest("Cererea are un format invalid!");
    }

    // Imagine mai mare decat limita din application.properties
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return badRequest("Imaginea poate avea maxim 5 MB!");
    }

    // Cerere de upload fara fisier sau care nu e multipart
    @ExceptionHandler({MissingServletRequestPartException.class, MultipartException.class})
    public ResponseEntity<Map<String, String>> handleMissingFile(Exception ex) {
        return badRequest("Imaginea este obligatorie!");
    }

    private ResponseEntity<Map<String, String>> badRequest(String mesaj) {
        Map<String, String> response = new HashMap<>();
        response.put("eroare", mesaj);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
