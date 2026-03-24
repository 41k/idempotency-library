package root.idempotency;

import java.util.Optional;

public interface IdempotentOperationStateService {

    Optional<IdempotentOperationState> find(String idempotencyId);

    void save(IdempotentOperationState state);
}
