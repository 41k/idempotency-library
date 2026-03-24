package root.idempotency;

import lombok.Value;

import java.util.Map;

public final class IdempotencyException {

    private IdempotencyException() {}

    public static class OperationIsInProgress extends RuntimeException {
    }

    @Value
    public static class OperationFailed extends RuntimeException {
        Map<String, Object> details;
    }
}
