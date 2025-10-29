package se.emulator.engine;

/**
 * Represents the outcome of attempting to load an S‑program definition
 * from disk.  If {@link #isSuccess()} is {@code true} then a program
 * has been loaded successfully and is available for execution.  In
 * either case a human readable message is provided for display to the
 * user.
 */
public final class LoadResult {
    private final boolean success;
    private final String message;

    public LoadResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}