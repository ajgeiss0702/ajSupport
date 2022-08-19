package us.ajg0702.bots.ajsupport;

public class EchoException extends Exception {
    private final String message;

    public EchoException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
