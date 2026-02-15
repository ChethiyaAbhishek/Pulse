package com.ecads.pulse.common.exception;

import com.ecads.pulse.authentication.exception.InvalidCredentialsException;
import com.ecads.pulse.authentication.exception.UserAlreadyExistsException;
import com.ecads.pulse.authentication.exception.UserNotFoundException;
import com.ecads.pulse.common.dto.ExceptionResponseDTO;
import lombok.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Object> userNotFoundHandler(@NonNull UserNotFoundException ex, @NonNull WebRequest request) {
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Object> invalidPasswordHandler(@NonNull InvalidCredentialsException ex, @NonNull WebRequest request) {
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Object> alreadyExistingUserHandler(@NonNull UserAlreadyExistsException ex, @NonNull WebRequest request) {
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> dataIntegrityHandler(DataIntegrityViolationException ex, @NonNull WebRequest request) {

        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Request could not be processed due to a data constraint violation.",
                request.getDescription(false)
        );

        return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> genericHandler(@NonNull Exception ex, @NonNull WebRequest request) {
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred.",
                request.getDescription(false)
        );
        return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
