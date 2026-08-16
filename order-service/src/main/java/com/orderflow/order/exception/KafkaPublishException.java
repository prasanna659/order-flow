package com.orderflow.order.exception;

public class KafkaPublishException extends RuntimeException {
    private final String errorCode;

    public KafkaPublishException(String message) {
        super(message);
        this.errorCode = "KAFKA_PUBLISH_FAILED";
    }

    public KafkaPublishException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public KafkaPublishException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "KAFKA_PUBLISH_FAILED";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
