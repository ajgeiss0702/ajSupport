package us.ajg0702.bots.ajsupport.autorespond;

public class Response {
    private final int confidence;
    private final String message;

    public Response(int confidence, String message) {
        this.confidence = confidence;
        this.message = message;
    }

    public int getConfidence() {
        return confidence;
    }

    public String getMessage() {
        return message;
    }
}
