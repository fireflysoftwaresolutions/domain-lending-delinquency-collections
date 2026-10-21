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
import io.fireflysoftware.domain.lending.collections.core.commands.GetCollectionCaseQuery;
import io.fireflysoftware.domain.lending.collections.core.commands.UpdateCollectionCaseStatusCommand;
import io.fireflysoftware.core.lending.collections.sdk.model.CollectionStatusHistoryDTO;
import io.fireflysoftware.domain.lending.collections.core.commands.RecordCollectionCaseStatusHistoryCommand;
import io.fireflysoftware.domain.lending.collections.core.commands.RemoveCollectionCaseStatusHistoryCommand;
import java.time.LocalDateTime;

import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.COMPENSATE_RESTORE;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.CTX_PREVIOUS_STATUS;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.CTX_RECORD;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.EVENT_RETRIEVED;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.EVENT_STATUS_UPDATED;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.SAGA_UPDATE_STATUS;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.STEP_RETRIEVE;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.STEP_UPDATE_STATUS;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.COMPENSATE_REMOVE_STATUS_HISTORY;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.EVENT_STATUS_HISTORY_RECORDED;
import static io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.STEP_RECORD_STATUS_HISTORY;

/**
 * Changes a collection case status by reading it and writing the new status through the core.
 */
@Saga(name = SAGA_UPDATE_STATUS)
@Service
public class UpdateCollectionCaseStatusSaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @Autowired
    public UpdateCollectionCaseStatusSaga(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @SagaStep(id = STEP_RETRIEVE)
    @StepEvent(type = EVENT_RETRIEVED)
    public Mono<CollectionCaseDTO> retrieveCollectionCase(GetCollectionCaseQuery query, ExecutionContext ctx) {
        return queryBus.query(query)
                .doOnNext(record -> ctx.putVariable(CTX_RECORD, record));
    }

    @SagaStep(id = STEP_UPDATE_STATUS, dependsOn = STEP_RETRIEVE, compensate = COMPENSATE_RESTORE)
    @StepEvent(type = EVENT_STATUS_UPDATED)
    public Mono<CollectionCaseDTO> updateCollectionCaseStatus(UpdateCollectionCaseStatusCommand command, ExecutionContext ctx) {
        CollectionCaseDTO current = ctx.getVariableAs(CTX_RECORD, CollectionCaseDTO.class);
        if (current == null || current.getCollectionCaseId() == null) {
            return Mono.error(new IllegalStateException("Collection case was not found"));
        }
        if (current.getCollectionStatus() == null) {
            return Mono.error(new IllegalStateException("Collection case has no current status"));
        }
        if (command.status() == null || command.status().isBlank()) {
            return Mono.error(new IllegalArgumentException("Status is required"));
        }
        ctx.putVariable(CTX_PREVIOUS_STATUS, current.getCollectionStatus().getValue());
        current.setCollectionStatus(CollectionCaseDTO.CollectionStatusEnum.fromValue(command.status()));
        return commandBus.send(new ApplyCollectionCaseStatusCommand(current.getCollectionCaseId(), current))
                .doOnNext(updated -> ctx.putVariable(CTX_RECORD, updated));
    }

    public Mono<Void> restoreCollectionCaseStatus(CollectionCaseDTO updated, ExecutionContext ctx) {
        if (updated == null || updated.getCollectionCaseId() == null) {
            return Mono.empty();
        }
        String previous = ctx.getVariableAs(CTX_PREVIOUS_STATUS, String.class);
        if (previous == null || previous.isBlank()) {
            return Mono.empty();
        }
        updated.setCollectionStatus(CollectionCaseDTO.CollectionStatusEnum.fromValue(previous));
        return commandBus.send(new ApplyCollectionCaseStatusCommand(updated.getCollectionCaseId(), updated)).then();
    }

    @SagaStep(id = STEP_RECORD_STATUS_HISTORY, dependsOn = STEP_UPDATE_STATUS, compensate = COMPENSATE_REMOVE_STATUS_HISTORY)
    @StepEvent(type = EVENT_STATUS_HISTORY_RECORDED)
    public Mono<CollectionStatusHistoryDTO> recordStatusHistory(UpdateCollectionCaseStatusCommand command, ExecutionContext ctx) {
        CollectionCaseDTO current = ctx.getVariableAs(CTX_RECORD, CollectionCaseDTO.class);
        if (current == null || current.getCollectionCaseId() == null || current.getCollectionStatus() == null) {
            return Mono.error(new IllegalStateException("Collection case status was not updated"));
        }
        CollectionStatusHistoryDTO history = new CollectionStatusHistoryDTO();
        history.setCollectionCaseId(current.getCollectionCaseId());
        history.setNewStatus(CollectionStatusHistoryDTO.NewStatusEnum.fromValue(current.getCollectionStatus().getValue()));
        String previous = ctx.getVariableAs(CTX_PREVIOUS_STATUS, String.class);
        if (previous != null && !previous.isBlank()) {
            history.setOldStatus(CollectionStatusHistoryDTO.OldStatusEnum.fromValue(previous));
        }
        history.setReasonCode(reasonCode(command.statusReason()));
        history.setChangedBy("domain-lending-delinquency-collections");
        history.setChangedAt(LocalDateTime.now());
        return commandBus.send(new RecordCollectionCaseStatusHistoryCommand(history));
    }

    public Mono<Void> removeStatusHistory(CollectionStatusHistoryDTO saved) {
        if (saved == null || saved.getCollectionStatusHistoryId() == null || saved.getCollectionCaseId() == null) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveCollectionCaseStatusHistoryCommand(saved.getCollectionCaseId(), saved.getCollectionStatusHistoryId()));
    }

    private static CollectionStatusHistoryDTO.ReasonCodeEnum reasonCode(String statusReason) {
        if (statusReason != null && !statusReason.isBlank()) {
            for (CollectionStatusHistoryDTO.ReasonCodeEnum code : CollectionStatusHistoryDTO.ReasonCodeEnum.values()) {
                if (code.getValue().equalsIgnoreCase(statusReason.trim())) {
                    return code;
                }
            }
        }
        return CollectionStatusHistoryDTO.ReasonCodeEnum.NO_CONTACT;
    }

}
