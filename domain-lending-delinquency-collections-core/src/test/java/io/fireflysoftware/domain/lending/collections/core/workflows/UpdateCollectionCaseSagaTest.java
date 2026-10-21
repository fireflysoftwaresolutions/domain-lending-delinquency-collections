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
import io.fireflysoftware.domain.lending.collections.core.commands.UpdateCollectionCaseCommand;
import io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants;
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
 * Verifies a field update keeps the current status and can be restored.
 */
@ExtendWith(MockitoExtension.class)
class UpdateCollectionCaseSagaTest {

    @Mock
    private CommandBus commandBus;

    @Mock
    private QueryBus queryBus;

    private UpdateCollectionCaseSaga saga;

    @BeforeEach
    void setUp() {
        saga = new UpdateCollectionCaseSaga(commandBus, queryBus);
    }

    @Test
    void update_keepsTheCurrentStatus() {
        UUID id = UUID.randomUUID();
        CollectionCaseDTO current = new CollectionCaseDTO(id);
        current.setCollectionStatus(CollectionCaseDTO.CollectionStatusEnum.OPEN);
        CollectionCaseDTO incoming = new CollectionCaseDTO();
        incoming.setCollectionStatus(CollectionCaseDTO.CollectionStatusEnum.LEGAL);
        when(commandBus.send(any(ApplyCollectionCaseStatusCommand.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0, ApplyCollectionCaseStatusCommand.class).record()));

        ExecutionContext ctx = ExecutionContext.forSaga(CollectionCaseSagaConstants.SAGA_UPDATE_RECORD, "test");
        ctx.putVariable(CollectionCaseSagaConstants.CTX_RECORD, current);

        StepVerifier.create(saga.updateCollectionCase(new UpdateCollectionCaseCommand(id, incoming), ctx))
                .assertNext(saved -> assertThat(saved.getCollectionStatus()).isEqualTo(CollectionCaseDTO.CollectionStatusEnum.OPEN))
                .verifyComplete();

        ArgumentCaptor<ApplyCollectionCaseStatusCommand> captor = ArgumentCaptor.forClass(ApplyCollectionCaseStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().collectionCaseId()).isEqualTo(id);
        assertThat(captor.getValue().record().getCollectionStatus()).isEqualTo(CollectionCaseDTO.CollectionStatusEnum.OPEN);
        assertThat(ctx.getVariableAs(CollectionCaseSagaConstants.CTX_PREVIOUS_RECORD, CollectionCaseDTO.class)).isSameAs(current);
    }

    @Test
    void restore_writesThePreviousRecordBack() {
        UUID id = UUID.randomUUID();
        CollectionCaseDTO previous = new CollectionCaseDTO(id);
        previous.setCollectionStatus(CollectionCaseDTO.CollectionStatusEnum.OPEN);
        CollectionCaseDTO updated = new CollectionCaseDTO(id);
        updated.setCollectionStatus(CollectionCaseDTO.CollectionStatusEnum.LEGAL);
        when(commandBus.send(any(ApplyCollectionCaseStatusCommand.class))).thenReturn(Mono.just(previous));

        ExecutionContext ctx = ExecutionContext.forSaga(CollectionCaseSagaConstants.SAGA_UPDATE_RECORD, "test");
        ctx.putVariable(CollectionCaseSagaConstants.CTX_PREVIOUS_RECORD, previous);

        StepVerifier.create(saga.restoreCollectionCase(updated, ctx))
                .verifyComplete();

        ArgumentCaptor<ApplyCollectionCaseStatusCommand> captor = ArgumentCaptor.forClass(ApplyCollectionCaseStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().record()).isSameAs(previous);
    }
}
