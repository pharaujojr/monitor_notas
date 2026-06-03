package br.com.pharaujo.monitornfe.service;

public class SefazIntegrationException extends RuntimeException {
    public SefazIntegrationException(String message) {
        super(message);
    }

    public SefazIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
