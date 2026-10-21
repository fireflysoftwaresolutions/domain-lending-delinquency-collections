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

package io.fireflysoftware.domain.lending.collections.core.services;

import io.fireflysoftware.core.lending.collections.sdk.model.CollectionCaseDTO;
import io.fireflysoftware.domain.lending.collections.core.commands.CreateCollectionCaseCommand;
import io.fireflysoftware.domain.lending.collections.core.commands.GetCollectionCaseQuery;
import io.fireflysoftware.domain.lending.collections.core.commands.RemoveCollectionCaseCommand;
import io.fireflysoftware.domain.lending.collections.core.commands.UpdateCollectionCaseCommand;
import io.fireflysoftware.domain.lending.collections.core.commands.UpdateCollectionCaseStatusCommand;
import io.fireflysoftware.domain.lending.collections.core.lifecycle.CollectionCaseLifecycle;
import io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Runs the collection case sagas and reads through the query bus.
 */
@Service
public class CollectionCasesServiceImpl implements CollectionCasesService {

    private final SagaEngine sagaEngine;
    private final QueryBus queryBus;
    private final CommandBus commandBus;

    public CollectionCasesServiceImpl(SagaEngine sagaEngine, QueryBus queryBus, CommandBus commandBus) {
        this.sagaEngine = sagaEngine;
        this.queryBus = queryBus;
        this.commandBus = commandBus;
    }

    @Override
    public Mono<CollectionCaseDTO> create(CreateCollectionCaseCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.STEP_REGISTER, command)
                .forStepId(io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.STEP_RECORD_INITIAL_STATUS, command)
                .build();
        return sagaEngine.execute(io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.SAGA_REGISTER, inputs)
                .flatMap(result -> outcome(result, io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.STEP_REGISTER, CollectionCaseDTO.class));
    }

    @Override
    public Mono<CollectionCaseDTO> updateStatus(UpdateCollectionCaseStatusCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.STEP_RETRIEVE, new GetCollectionCaseQuery(command.collectionCaseId()))
                .forStepId(io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.STEP_UPDATE_STATUS, command)
                .forStepId(io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.STEP_RECORD_STATUS_HISTORY, command)
                .build();
        return sagaEngine.execute(io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.SAGA_UPDATE_STATUS, inputs)
                .flatMap(result -> outcome(result, io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.STEP_UPDATE_STATUS, CollectionCaseDTO.class));
    }

    @Override
    public Mono<CollectionCaseDTO> get(UUID collectionCaseId) {
        return queryBus.query(new GetCollectionCaseQuery(collectionCaseId));
    }

    @Override
    public Mono<CollectionCaseDTO> update(UpdateCollectionCaseCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.STEP_RETRIEVE_FOR_UPDATE, new GetCollectionCaseQuery(command.collectionCaseId()))
                .forStepId(io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.STEP_UPDATE_RECORD, command)
                .build();
        return sagaEngine.execute(io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.SAGA_UPDATE_RECORD, inputs)
                .flatMap(result -> outcome(result, io.fireflysoftware.domain.lending.collections.core.constants.CollectionCaseSagaConstants.STEP_UPDATE_RECORD, CollectionCaseDTO.class));
    }

    @Override
    public Mono<CollectionCaseDTO> close(UUID collectionCaseId, String statusReason) {
        return transition(collectionCaseId, CollectionCaseDTO.CollectionStatusEnum.CLOSED, statusReason);
    }

    private Mono<CollectionCaseDTO> transition(UUID collectionCaseId, CollectionCaseDTO.CollectionStatusEnum target, String statusReason) {
        return get(collectionCaseId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Collection case was not found")))
                .flatMap(current -> {
                    if (!CollectionCaseLifecycle.canTransition(current.getCollectionStatus(), target)) {
                        String from = current.getCollectionStatus() == null
                                ? "unset"
                                : current.getCollectionStatus().getValue();
                        return Mono.<CollectionCaseDTO>error(new IllegalStateException(
                                "Collection case " + collectionCaseId + " cannot move from " + from + " to " + target.getValue()));
                    }
                    return updateStatus(new UpdateCollectionCaseStatusCommand(collectionCaseId, target.getValue(), statusReason));
                });
    }

    @Override
    public Mono<Void> remove(UUID collectionCaseId) {
        return get(collectionCaseId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Collection case was not found")))
                .flatMap(current -> {
                    if (!CollectionCaseLifecycle.canRemove(current.getCollectionStatus())) {
                        String statusValue = current.getCollectionStatus() == null
                                ? "unset"
                                : current.getCollectionStatus().getValue();
                        return Mono.error(new IllegalStateException(
                                "Collection case " + collectionCaseId + " is " + statusValue
                                        + " and cannot be deleted; close it so the case is kept"));
                    }
                    return commandBus.send(new RemoveCollectionCaseCommand(collectionCaseId));
                });
    }

    private static <T> Mono<T> outcome(SagaResult result, String stepId, Class<T> type) {
        if (result.isFailed()) {
            Throwable error = result.error().orElseGet(() ->
                    new IllegalStateException("Saga " + result.sagaName() + " failed"));
            return Mono.error(error);
        }
        return Mono.justOrEmpty(result.resultOf(stepId, type))
                .switchIfEmpty(Mono.error(new IllegalStateException("Saga step " + stepId + " produced no result")));
    }
}
