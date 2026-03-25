# Idempotency library

This library provides capability for idempotent operation execution.

## Dependencies

It is necessary to provide implementations of the next 2 interfaces to `IdempotentOperationExecutor`:

- `IdempotentOperationStateService` - which is responsible for persistence of `IdempotentOperationState`. It is reasonable to provide implementation which is based on distributed cache (e.g. Redis) or NoSQL database with high performance and availability
- `IdempotentOperationFailureDetailsProvider` - which provides `Map<String, Object> failureDetails` based on `Exception` thrown during idempotent operation execution. So such operation may throw custom domain-specific exceptions and implementation of the interface may build `failureDetails` based on data provided in such exceptions.

## Usage

See `IdempotentOperationExecutorTest` for more details.