package Assignment.RequestService.Exceptions;

public class APILimitReachedException extends RuntimeException {
    public APILimitReachedException(String message) {
        super(message);
    }
}
