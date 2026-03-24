package root.idempotency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import root.idempotency.IdempotentOperationState.Status;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IdempotentOperationExecutorTest {

    private static final String RESOURCE_ID = "resource-1-id";
    private static final Integer VALUE_1 = 10;
    private static final Integer VALUE_2 = 30;
    private static final Map<String, Integer> RESOURCES_STORE = Map.of(RESOURCE_ID, VALUE_1);

    private static final String IDEMPOTENCY_ID = "00000000-0000-0000-0000-000000000001";
    private static final String EXCEPTION_MESSAGE = "error";
    private static final RuntimeException EXECUTION_EXCEPTION = new RuntimeException(EXCEPTION_MESSAGE);
    private static final Map<String, Object> FAILURE_DETAILS = Map.of("exceptionMessage", EXCEPTION_MESSAGE);
    private static final IdempotentOperationState IN_PROGRESS_OPERATION_STATE =
            IdempotentOperationState.builder().idempotencyId(IDEMPOTENCY_ID).status(Status.IN_PROGRESS).build();
    private static final IdempotentOperationState SUCCEEDED_OPERATION_STATE =
            IdempotentOperationState.builder().idempotencyId(IDEMPOTENCY_ID).status(Status.SUCCEEDED).build();
    private static final IdempotentOperationState FAILED_OPERATION_STATE =
            IdempotentOperationState.builder().idempotencyId(IDEMPOTENCY_ID).status(Status.FAILED).failureDetails(FAILURE_DETAILS).build();
    private static final Supplier<Integer> OPERATION_THROWING_EXCEPTION = () -> { throw EXECUTION_EXCEPTION; };

    @Mock
    private IdempotentOperationStateService operationStateService;
    @Mock
    private IdempotentOperationFailureDetailsProvider failureDetailsProvider;
    @InjectMocks
    private IdempotentOperationExecutor idempotentOperationExecutor;

    private Map<String, Integer> resourcesStore;
    private final Supplier<Integer> operation = () -> resourcesStore.computeIfPresent(RESOURCE_ID, (key, value) -> value + 20);
    private final Supplier<Integer> operationResultProvider = () -> resourcesStore.get(RESOURCE_ID);

    @BeforeEach
    void setup() {
        resourcesStore = new HashMap<>(RESOURCES_STORE);
    }

    @AfterEach
    void afterEach() {
        verifyNoMoreInteractions(operationStateService, failureDetailsProvider);
    }

    @Test
    void executeOperation_firstExecution_operationSucceeded() {
        // given
        when(operationStateService.find(IDEMPOTENCY_ID)).thenReturn(Optional.empty());

        // when
        Integer operationResult = idempotentOperationExecutor.executeOperation(IDEMPOTENCY_ID, operation, operationResultProvider);

        // then
        assertThat(operationResult).isEqualTo(VALUE_2);
        assertThat(resourcesStore.get(RESOURCE_ID)).isEqualTo(VALUE_2);

        // and
        verify(operationStateService).find(IDEMPOTENCY_ID);
        verify(operationStateService).save(IN_PROGRESS_OPERATION_STATE);
        verify(operationStateService).save(SUCCEEDED_OPERATION_STATE);
    }

    @Test
    void executeOperation_firstExecution_operationFailed() {
        // given
        when(operationStateService.find(IDEMPOTENCY_ID)).thenReturn(Optional.empty());
        when(failureDetailsProvider.getDetails(EXECUTION_EXCEPTION)).thenReturn(FAILURE_DETAILS);

        // expect
        assertThatThrownBy(() ->
                idempotentOperationExecutor.executeOperation(IDEMPOTENCY_ID, OPERATION_THROWING_EXCEPTION, operationResultProvider))
                .isEqualTo(EXECUTION_EXCEPTION);

        // and
        verify(operationStateService).find(IDEMPOTENCY_ID);
        verify(operationStateService).save(IN_PROGRESS_OPERATION_STATE);
        verify(failureDetailsProvider).getDetails(EXECUTION_EXCEPTION);
        verify(operationStateService).save(FAILED_OPERATION_STATE);
    }

    @Test
    void executeOperation_repeatedExecution_operationSucceeded() {
        // given
        when(operationStateService.find(IDEMPOTENCY_ID)).thenReturn(Optional.of(SUCCEEDED_OPERATION_STATE));

        // when
        Integer operationResult = idempotentOperationExecutor.executeOperation(IDEMPOTENCY_ID, operation, operationResultProvider);

        // then
        assertThat(operationResult).isEqualTo(VALUE_1);

        // and
        verify(operationStateService).find(IDEMPOTENCY_ID);
    }

    @Test
    void executeOperation_repeatedExecution_operationIsInProgress() {
        // given
        when(operationStateService.find(IDEMPOTENCY_ID)).thenReturn(Optional.of(IN_PROGRESS_OPERATION_STATE));

        // expect
        assertThatThrownBy(() ->
                idempotentOperationExecutor.executeOperation(IDEMPOTENCY_ID, operation, operationResultProvider))
                .isInstanceOf(IdempotencyException.OperationIsInProgress.class);

        // and
        verify(operationStateService).find(IDEMPOTENCY_ID);
    }

    @Test
    void executeOperation_repeatedExecution_operationFailed() {
        // given
        when(operationStateService.find(IDEMPOTENCY_ID)).thenReturn(Optional.of(FAILED_OPERATION_STATE));

        // expect
        assertThatThrownBy(() ->
                idempotentOperationExecutor.executeOperation(IDEMPOTENCY_ID, operation, operationResultProvider))
                .isEqualTo(new IdempotencyException.OperationFailed(FAILURE_DETAILS));

        // and
        verify(operationStateService).find(IDEMPOTENCY_ID);
    }
}
