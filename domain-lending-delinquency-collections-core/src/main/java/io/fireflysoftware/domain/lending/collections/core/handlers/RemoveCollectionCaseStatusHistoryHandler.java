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

package io.fireflysoftware.domain.lending.collections.core.handlers;

import io.fireflysoftware.core.lending.collections.sdk.api.CollectionStatusHistoryApi;
import io.fireflysoftware.domain.lending.collections.core.commands.RemoveCollectionCaseStatusHistoryCommand;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Deletes a status history row during saga compensation.
 */
@Slf4j
@CommandHandlerComponent
public class RemoveCollectionCaseStatusHistoryHandler extends CommandHandler<RemoveCollectionCaseStatusHistoryCommand, Void> {

    private final CollectionStatusHistoryApi historyApi;

    public RemoveCollectionCaseStatusHistoryHandler(CollectionStatusHistoryApi historyApi) {
        this.historyApi = historyApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveCollectionCaseStatusHistoryCommand command) {
        log.debug("Removing collection case status history");
        return historyApi.delete1(
                command.collectionCaseId(),
                command.collectionStatusHistoryId(),
                UUID.randomUUID().toString()).then();
    }
}
