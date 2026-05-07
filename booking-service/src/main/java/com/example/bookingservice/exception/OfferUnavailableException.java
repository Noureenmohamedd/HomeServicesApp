package com.example.bookingservice.exception;

public class OfferUnavailableException extends RuntimeException {

    public OfferUnavailableException(String message) {
        super(message);
    }
}
