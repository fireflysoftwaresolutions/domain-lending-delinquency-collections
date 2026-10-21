/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.domain.lending.collections.core.workflows;

import io.fireflysoftware.core.lending.collections.sdk.model.CollectionCaseDTO;
import io.fireflysoftware.domain.lending.collections.core.commands.ApplyCollectionCaseStatusCommand;
import io.fireflysoftware.domain.lending.collections.core.commands.UpdateCollectionCaseStatusCommand;
import io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants;

import io.fireflysoftware.core.lending.collections.sdk.model.CollectionStatusHistoryDTO;
import io.fireflysoftware.domain.lending.collections.core.commands.RecordCollectionCaseStatusHistoryCommand;

import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies a status change is written through the core and can be restored.
 */
@ExtendWith(MockitoExtension.class)
class UpdateCollectionCaseStatusSagaTest {

    @Mock
    private CommandBus commandBus;

    @Mock
    private QueryBus queryBus;

    private UpdateCollectionCaseStatusSaga saga;

    @BeforeEach
    void setUp() {
        saga = new UpdateCollectionCaseStatusSaga(commandBus, queryBus);
    }

    @Test
    void updateStatus_sendsTheRequestedStatusToTheCore() {
        UUID id = UUID.randomUUID();
        CollectionCaseDTO current = new CollectionCaseDTO(id);
        current.setCollectionStatus(CollectionCaseDTO.CollectionStatusEnum.OPEN);
        when(commandBus.send(any(ApplyCollectionCaseStatusCommand.class))).thenReturn(Mono.just(current));

        ExecutionContext ctx = ExecutionContext.forSaga(CollectionCaseSagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(CollectionCaseSagaConstants.CTX_RECORD, current);

        StepVerifier.create(saga.updateCollectionCaseStatus(new UpdateCollectionCaseStatusCommand(id, "LEGAL", "Caller request"), ctx))
                .expectNext(current)
                .verifyComplete();

        ArgumentCaptor<ApplyCollectionCaseStatusCommand> captor = ArgumentCaptor.forClass(ApplyCollectionCaseStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().collectionCaseId()).isEqualTo(id);
        assertThat(captor.getValue().record().getCollectionStatus()).isEqualTo(CollectionCaseDTO.CollectionStatusEnum.LEGAL);
        assertThat(ctx.getVariableAs(CollectionCaseSagaConstants.CTX_PREVIOUS_STATUS, String.class)).isEqualTo("OPEN");
    }

    @Test
    void restore_writesThePreviousStatusBack() {
        UUID id = UUID.randomUUID();
        CollectionCaseDTO updated = new CollectionCaseDTO(id);
        updated.setCollectionStatus(CollectionCaseDTO.CollectionStatusEnum.LEGAL);
        when(commandBus.send(any(ApplyCollectionCaseStatusCommand.class))).thenReturn(Mono.just(updated));

        ExecutionContext ctx = ExecutionContext.forSaga(CollectionCaseSagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(CollectionCaseSagaConstants.CTX_PREVIOUS_STATUS, "OPEN");

        StepVerifier.create(saga.restoreCollectionCaseStatus(updated, ctx))
                .verifyComplete();

        ArgumentCaptor<ApplyCollectionCaseStatusCommand> captor = ArgumentCaptor.forClass(ApplyCollectionCaseStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().record().getCollectionStatus()).isEqualTo(CollectionCaseDTO.CollectionStatusEnum.OPEN);
    }

    @Test
    void recordStatusHistory_usesACoreReasonCodeWhenTheCallerSendsOne() {
        UUID id = UUID.randomUUID();
        CollectionCaseDTO current = new CollectionCaseDTO(id);
        current.setCollectionStatus(CollectionCaseDTO.CollectionStatusEnum.LEGAL);
        CollectionStatusHistoryDTO saved = new CollectionStatusHistoryDTO(UUID.randomUUID());
        when(commandBus.send(any(RecordCollectionCaseStatusHistoryCommand.class))).thenReturn(Mono.just(saved));

        ExecutionContext ctx = ExecutionContext.forSaga(CollectionCaseSagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(CollectionCaseSagaConstants.CTX_RECORD, current);
        ctx.putVariable(CollectionCaseSagaConstants.CTX_PREVIOUS_STATUS, "OPEN");

        StepVerifier.create(saga.recordStatusHistory(new UpdateCollectionCaseStatusCommand(id, "LEGAL", "PAYMENT_RECEIVED"), ctx))
                .expectNext(saved)
                .verifyComplete();

        ArgumentCaptor<RecordCollectionCaseStatusHistoryCommand> captor = ArgumentCaptor.forClass(RecordCollectionCaseStatusHistoryCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().history().getReasonCode().getValue()).isEqualTo("PAYMENT_RECEIVED");
        assertThat(captor.getValue().history().getOldStatus().getValue()).isEqualTo("OPEN");
    }

}
