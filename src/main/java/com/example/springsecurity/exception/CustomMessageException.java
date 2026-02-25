package com.example.springsecurity.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CustomMessageException extends RuntimeException {

    private String message;
    private String code;

    public CustomMessageException(String message, String code) {
        super(message);
        this.message = message;
        this.code = code;
    }

    public CustomMessageException(String message) {
        super(message);
        this.message = message;
        this.code = "INTERNAL_SERVER_ERROR";
    }

}
