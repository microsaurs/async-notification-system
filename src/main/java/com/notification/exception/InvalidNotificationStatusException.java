package com.notification.exception;

public class InvalidNotificationStatusException extends RuntimeException {

    public InvalidNotificationStatusException(String message) {
        super(message);
    }
}
