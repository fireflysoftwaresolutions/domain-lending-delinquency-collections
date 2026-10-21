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
import io.fireflysoftware.core.lending.collections.sdk.model.CollectionStatusHistoryDTO;
import io.fireflysoftware.domain.lending.collections.core.commands.RecordCollectionCaseStatusHistoryCommand;
import io.fireflysoftware.domain.lending.collections.core.commands.RemoveCollectionCaseStatusHistoryCommand;
import java.time.LocalDateTime;

import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.COMPENSATE_REMOVE;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.CTX_RECORD;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.EVENT_REGISTERED;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.SAGA_REGISTER;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.STEP_REGISTER;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.COMPENSATE_REMOVE_INITIAL_STATUS;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.EVENT_INITIAL_STATUS_RECORDED;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.STEP_RECORD_INITIAL_STATUS;

/**
 * Creates a collection case on {@code core-lending-delinquency-collections} and compensates by deleting it.
 */
@Saga(name = SAGA_REGISTER)
@Service
public class RegisterCollectionCaseSaga {

    private final CommandBus commandBus;

    @Autowired
    public RegisterCollectionCaseSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_REGISTER, compensate = COMPENSATE_REMOVE)
    @StepEvent(type = EVENT_REGISTERED)
    public Mono<CollectionCaseDTO> registerCollectionCase(CreateCollectionCaseCommand command, ExecutionContext ctx) {
        return commandBus.send(command)
                .doOnNext(created -> ctx.putVariable(CTX_RECORD, created));
    }

    public Mono<Void> removeCollectionCase(CollectionCaseDTO created) {
        if (created == null || created.getCollectionCaseId() == null) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveCollectionCaseCommand(created.getCollectionCaseId()));
    }

    @SagaStep(id = STEP_RECORD_INITIAL_STATUS, dependsOn = STEP_REGISTER, compensate = COMPENSATE_REMOVE_INITIAL_STATUS)
    @StepEvent(type = EVENT_INITIAL_STATUS_RECORDED)
    public Mono<CollectionStatusHistoryDTO> recordInitialStatus(CreateCollectionCaseCommand command, ExecutionContext ctx) {
        CollectionCaseDTO created = ctx.getVariableAs(CTX_RECORD, CollectionCaseDTO.class);
        if (created == null || created.getCollectionCaseId() == null) {
            return Mono.error(new IllegalStateException("Collection case was not created"));
        }
        if (created.getCollectionStatus() == null) {
            return Mono.error(new IllegalStateException("Collection case status is required before history can be recorded"));
        }
        CollectionStatusHistoryDTO history = new CollectionStatusHistoryDTO();
        history.setCollectionCaseId(created.getCollectionCaseId());
        history.setNewStatus(CollectionStatusHistoryDTO.NewStatusEnum.fromValue(created.getCollectionStatus().getValue()));
        history.setReasonCode(CollectionStatusHistoryDTO.ReasonCodeEnum.NO_CONTACT);
        history.setChangedBy("domain-lending-delinquency-collections");
        history.setChangedAt(LocalDateTime.now());
        return commandBus.send(new RecordCollectionCaseStatusHistoryCommand(history));
    }

    public Mono<Void> removeInitialStatus(CollectionStatusHistoryDTO saved) {
        if (saved == null || saved.getCollectionStatusHistoryId() == null || saved.getCollectionCaseId() == null) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveCollectionCaseStatusHistoryCommand(saved.getCollectionCaseId(), saved.getCollectionStatusHistoryId()));
    }

}
