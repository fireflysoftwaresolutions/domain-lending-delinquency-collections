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
import io.fireflysoftware.domain.lending.collections.core.commands.CreateCollectionCaseCommand;
import io.fireflysoftware.domain.lending.collections.core.commands.RemoveCollectionCaseCommand;
import io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants;

import io.fireflysoftware.core.lending.collections.sdk.model.CollectionStatusHistoryDTO;
import io.fireflysoftware.domain.lending.collections.core.commands.RecordCollectionCaseStatusHistoryCommand;

import org.fireflyframework.cqrs.command.CommandBus;
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
 * Verifies the register saga stores the core result and can compensate by deleting it.
 */
@ExtendWith(MockitoExtension.class)
class RegisterCollectionCaseSagaTest {

    @Mock
    private CommandBus commandBus;

    private RegisterCollectionCaseSaga saga;

    @BeforeEach
    void setUp() {
        saga = new RegisterCollectionCaseSaga(commandBus);
    }

    @Test
    void register_storesTheRecordReturnedByTheCore() {
        UUID id = UUID.randomUUID();
        CollectionCaseDTO created = new CollectionCaseDTO(id);
        created.setCollectionStatus(CollectionCaseDTO.CollectionStatusEnum.OPEN);
        when(commandBus.send(any(CreateCollectionCaseCommand.class))).thenReturn(Mono.just(created));

        ExecutionContext ctx = ExecutionContext.forSaga(CollectionCaseSagaConstants.SAGA_REGISTER, "test");
        CollectionCaseDTO request = new CollectionCaseDTO();
        request.setCollectionStatus(CollectionCaseDTO.CollectionStatusEnum.OPEN);

        StepVerifier.create(saga.registerCollectionCase(new CreateCollectionCaseCommand(request), ctx))
                .expectNext(created)
                .verifyComplete();

        assertThat(ctx.getVariableAs(CollectionCaseSagaConstants.CTX_RECORD, CollectionCaseDTO.class).getCollectionCaseId()).isEqualTo(id);
    }

    @Test
    void remove_sendsDeleteForTheCreatedRecord() {
        UUID id = UUID.randomUUID();
        when(commandBus.send(any(RemoveCollectionCaseCommand.class))).thenReturn(Mono.empty());

        StepVerifier.create(saga.removeCollectionCase(new CollectionCaseDTO(id)))
                .verifyComplete();

        ArgumentCaptor<RemoveCollectionCaseCommand> captor = ArgumentCaptor.forClass(RemoveCollectionCaseCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().collectionCaseId()).isEqualTo(id);
    }

    @Test
    void recordInitialStatus_copiesIdAndStatusFromTheCreatedRecord() {
        UUID id = UUID.randomUUID();
        CollectionCaseDTO created = new CollectionCaseDTO(id);
        created.setCollectionStatus(CollectionCaseDTO.CollectionStatusEnum.OPEN);
        CollectionStatusHistoryDTO saved = new CollectionStatusHistoryDTO(UUID.randomUUID());
        when(commandBus.send(any(RecordCollectionCaseStatusHistoryCommand.class))).thenReturn(Mono.just(saved));

        ExecutionContext ctx = ExecutionContext.forSaga(CollectionCaseSagaConstants.SAGA_REGISTER, "test");
        ctx.putVariable(CollectionCaseSagaConstants.CTX_RECORD, created);

        StepVerifier.create(saga.recordInitialStatus(new CreateCollectionCaseCommand(created), ctx))
                .expectNext(saved)
                .verifyComplete();

        ArgumentCaptor<RecordCollectionCaseStatusHistoryCommand> captor = ArgumentCaptor.forClass(RecordCollectionCaseStatusHistoryCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().history().getCollectionCaseId()).isEqualTo(id);
        assertThat(captor.getValue().history().getNewStatus().getValue()).isEqualTo("OPEN");
        assertThat(captor.getValue().history().getReasonCode()).isEqualTo(CollectionStatusHistoryDTO.ReasonCodeEnum.NO_CONTACT);
    }

}
