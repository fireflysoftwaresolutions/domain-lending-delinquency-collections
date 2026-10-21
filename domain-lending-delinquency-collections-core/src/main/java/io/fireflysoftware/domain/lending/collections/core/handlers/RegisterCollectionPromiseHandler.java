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

import io.fireflysoftware.core.lending.collections.sdk.api.CollectionPromiseApi;
import io.fireflysoftware.core.lending.collections.sdk.model.CollectionPromiseDTO;
import io.fireflysoftware.domain.lending.collections.core.commands.RegisterCollectionPromiseCommand;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Writes a payment promise through the delinquency collections core.
 */
@Slf4j
@CommandHandlerComponent
public class RegisterCollectionPromiseHandler extends CommandHandler<RegisterCollectionPromiseCommand, CollectionPromiseDTO> {

    private final CollectionPromiseApi api;

    public RegisterCollectionPromiseHandler(CollectionPromiseApi api) {
        this.api = api;
    }

    @Override
    protected Mono<CollectionPromiseDTO> doHandle(RegisterCollectionPromiseCommand command) {
        log.debug("Registering payment promise for collectionCaseId={}", command.collectionCaseId());
        CollectionPromiseDTO body = new CollectionPromiseDTO()
                .collectionCaseId(command.collectionCaseId())
                .promisedAmount(command.promisedAmount())
                .promisedDate(command.promisedDate());
        return api.create2(command.collectionCaseId(), body, UUID.randomUUID().toString());
    }
}
