package gov.cabinetoffice.gap.exceptions;

public class EmptySqsEventException extends RuntimeException {

    public EmptySqsEventException() {
    }

    public EmptySqsEventException(String message) {
        super(message);
    }
}
