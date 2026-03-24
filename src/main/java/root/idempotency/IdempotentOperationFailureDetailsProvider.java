package root.idempotency;

import java.util.Map;

public interface IdempotentOperationFailureDetailsProvider {

    Map<String, Object> getDetails(Exception exception);
}
