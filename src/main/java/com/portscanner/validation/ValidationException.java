package com.portscanner.validation;

/**
 * Thrown when user-supplied scan parameters fail validation.
 * The message is written to be directly displayable to the end user
 * (i.e. no stack-trace jargon), since the GUI shows it in an alert dialog.
 */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }
}
