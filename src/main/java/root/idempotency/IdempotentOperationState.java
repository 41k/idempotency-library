package root.idempotency;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class IdempotentOperationState {

    String idempotencyId;
    Status status;
    Map<String, Object> failureDetails;

    public static IdempotentOperationState inProgress(String idempotencyId) {
        return new IdempotentOperationState(idempotencyId, Status.IN_PROGRESS, null);
    }

    public static IdempotentOperationState succeeded(String idempotencyId) {
        return new IdempotentOperationState(idempotencyId, Status.SUCCEEDED, null);
    }

    public static IdempotentOperationState failed(String idempotencyId, Map<String, Object> details) {
        return new IdempotentOperationState(idempotencyId, Status.FAILED, details);
    }

    public boolean isInProgress() {
        return status == Status.IN_PROGRESS;
    }

    public boolean isFailed() {
        return status == Status.FAILED;
    }

    enum Status {
        IN_PROGRESS,
        SUCCEEDED,
        FAILED
    }
}
