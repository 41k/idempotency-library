package root.idempotency;

import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class IdempotentOperationExecutor {

    private final IdempotentOperationStateService operationStateService;
    private final IdempotentOperationFailureDetailsProvider failureDetailsProvider;

    public <T> T executeOperation(String idempotencyId,
                                  Supplier<T> operation,
                                  Supplier<T> operationResultProvider) {
        return operationStateService.find(idempotencyId)
                .map(operationState -> getOperationResult(operationState, operationResultProvider))
                .orElseGet(() -> executeOperation(idempotencyId, operation));
    }

    private <T> T getOperationResult(IdempotentOperationState operationState, Supplier<T> operationResultProvider) {
        if (operationState.isInProgress()) {
            throw new IdempotencyException.OperationIsInProgress();
        }
        if (operationState.isFailed()) {
            throw new IdempotencyException.OperationFailed(operationState.getFailureDetails());
        }
        return operationResultProvider.get();
    }

    private <T> T executeOperation(String idempotencyId, Supplier<T> operation) {
        operationStateService.save(IdempotentOperationState.inProgress(idempotencyId));
        var operationState = IdempotentOperationState.succeeded(idempotencyId);
        try {
            return operation.get();
        } catch (Exception exception) {
            var details = failureDetailsProvider.getDetails(exception);
            operationState = IdempotentOperationState.failed(idempotencyId, details);
            throw exception;
        } finally {
            operationStateService.save(operationState);
        }
    }
}
