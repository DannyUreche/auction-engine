package com.DannyU.auction_engine.exception;

import org.springframework.http.HttpStatus;
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

@RestControllerAdvice
public class GlobalExceptionHandler {

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
