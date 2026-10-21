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

import io.fireflysoftware.core.lending.collections.sdk.api.CollectionCaseApi;
import io.fireflysoftware.core.lending.collections.sdk.model.CollectionCaseDTO;
import io.fireflysoftware.domain.lending.collections.core.commands.CreateCollectionCaseCommand;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Creates a collection case by calling {@code core-lending-delinquency-collections}.
 */
@Slf4j
@CommandHandlerComponent
public class CreateCollectionCaseHandler extends CommandHandler<CreateCollectionCaseCommand, CollectionCaseDTO> {

    private final CollectionCaseApi api;

    public CreateCollectionCaseHandler(CollectionCaseApi api) {
        this.api = api;
    }

    @Override
    protected Mono<CollectionCaseDTO> doHandle(CreateCollectionCaseCommand command) {
        log.debug("Creating collection case");
        return api.create(command.record(), UUID.randomUUID().toString());
    }
}
