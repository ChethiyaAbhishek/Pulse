package com.ecads.pulse.common.exception;

import com.ecads.pulse.authentication.exception.FirebaseAuthFailedException;
import com.ecads.pulse.common.dto.ExceptionResponseDTO;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FirebaseAuthFailedException.class)
    public ResponseEntity<Object> firebaseAuthHandler(FirebaseAuthFailedException ex, @NonNull WebRequest request) {
        log.error(ex.getMessage());
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> dataIntegrityHandler(DataIntegrityViolationException ex, @NonNull WebRequest request) {
        log.error(ex.getMessage());
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Request could not be processed due to a data constraint violation.",
                request.getDescription(false)
        );

        return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FirebaseInitializationFailedException.class)
    public ResponseEntity<Object> firebaseInitializationHandler(FirebaseInitializationFailedException ex, @NonNull WebRequest request) {
        log.error(ex.getMessage());
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(FirebaseFirestoreException.class)
    public ResponseEntity<Object> firebaseInitializationHandler(FirebaseFirestoreException ex, @NonNull WebRequest request) {
        log.error(ex.getMessage());
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ShowErrorException.class)
    public ResponseEntity<Object> showErrorHandler(ShowErrorException ex, @NonNull WebRequest request) {
        log.error(ex.getMessage());
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                ex.getMessage(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(responseDTO, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // MethodArgumentNotValidException -> validations

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> genericHandler(@NonNull Exception ex, @NonNull WebRequest request) {
        log.error(ex.toString());
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred.",
                request.getDescription(false)
        );
        return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
