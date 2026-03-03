package com.ecads.pulse.authentication.exception;

public class FirebaseAuthFailedException extends RuntimeException {
    public FirebaseAuthFailedException(String message) {
        super(message);
    }
}
