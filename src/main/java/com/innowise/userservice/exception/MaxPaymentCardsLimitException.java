package com.innowise.userservice.exception;

public class MaxPaymentCardsLimitException extends RuntimeException {
    public MaxPaymentCardsLimitException(String message) {
        super(message);
    }
}
