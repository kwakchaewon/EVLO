package com.evlo.parser;

public class EvtxParsingException extends RuntimeException {

    public EvtxParsingException(String message) {
        super(message);
    }

    public EvtxParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
