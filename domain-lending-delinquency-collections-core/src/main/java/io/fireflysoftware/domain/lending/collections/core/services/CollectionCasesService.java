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
import io.fireflysoftware.core.lending.collections.sdk.model.CollectionPromiseDTO;
import io.fireflysoftware.core.lending.collections.sdk.model.PaginationResponseCollectionCaseDTO;
import io.fireflysoftware.domain.lending.collections.core.commands.CreateCollectionCaseCommand;
import io.fireflysoftware.domain.lending.collections.core.commands.RegisterCollectionPromiseCommand;
import io.fireflysoftware.domain.lending.collections.core.commands.UpdateCollectionCaseCommand;
import io.fireflysoftware.domain.lending.collections.core.commands.UpdateCollectionCaseStatusCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Application service for collection case orchestration.
 */
public interface CollectionCasesService {

    /**
     * Creates a collection case through the register saga.
     *
     * @param command the create command
     * @return the record returned by the core
     */
    /**
     * Lists collection cases stored by the delinquency collections core.
     *
     * @return the core page
     */
    Mono<PaginationResponseCollectionCaseDTO> list();

    /**
     * Persists a payment promise on a collection case.
     *
     * @param command the promise to store
     * @return the record returned by the core
     */
    Mono<CollectionPromiseDTO> registerPromise(RegisterCollectionPromiseCommand command);

    Mono<CollectionCaseDTO> create(CreateCollectionCaseCommand command);

    /**
     * Moves a collection case to a new status through the core.
     *
     * @param command the status change
     * @return the updated record
     */
    Mono<CollectionCaseDTO> updateStatus(UpdateCollectionCaseStatusCommand command);

    /**
     * Reads one collection case.
     *
     * @param collectionCaseId the core identifier
     * @return the record, or empty when the core has none
     */
    Mono<CollectionCaseDTO> get(UUID collectionCaseId);

    /**
     * Updates fields on a collection case and leaves its status unchanged.
     *
     * @param command the update
     * @return the record returned by the core
     */
    Mono<CollectionCaseDTO> update(UpdateCollectionCaseCommand command);

    /**
     * Close collection case when the core allows CLOSED.
     *
     * @param collectionCaseId the core identifier
     * @param statusReason optional reason stored by the status saga when the core keeps one
     * @return the updated record
     */
    Mono<CollectionCaseDTO> close(UUID collectionCaseId, String statusReason);

    /**
     * Deletes a collection case when the core lifecycle allows removal.
     *
     * @param collectionCaseId the core identifier
     * @return completion when the core has deleted the record
     */
    Mono<Void> remove(UUID collectionCaseId);
}
