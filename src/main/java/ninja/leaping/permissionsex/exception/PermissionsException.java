package ninja.leaping.permissionsex.exception;

public class PermissionsException extends Exception {
    public PermissionsException() {
        super();
    }

    public PermissionsException(String message) {
        super(message);
    }

    public PermissionsException(String message, Throwable cause) {
        super(message, cause);
    }

    public PermissionsException(Throwable cause) {
        super(cause);
    }
}
