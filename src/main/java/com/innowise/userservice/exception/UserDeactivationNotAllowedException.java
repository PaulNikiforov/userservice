package com.innowise.userservice.exception;

public class UserDeactivationNotAllowedException extends RuntimeException {
    public UserDeactivationNotAllowedException(String message) {
        super(message);
    }
}
