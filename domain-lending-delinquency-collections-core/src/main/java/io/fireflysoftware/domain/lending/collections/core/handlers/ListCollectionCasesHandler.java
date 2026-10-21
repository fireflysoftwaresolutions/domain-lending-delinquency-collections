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
import io.fireflysoftware.core.lending.collections.sdk.model.FilterRequestCollectionCaseDTO;
import io.fireflysoftware.core.lending.collections.sdk.model.PaginationResponseCollectionCaseDTO;
import io.fireflysoftware.domain.lending.collections.core.commands.ListCollectionCasesQuery;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Lists collection cases from the delinquency collections core.
 */
@Slf4j
@QueryHandlerComponent
public class ListCollectionCasesHandler extends QueryHandler<ListCollectionCasesQuery, PaginationResponseCollectionCaseDTO> {

    private final CollectionCaseApi api;

    public ListCollectionCasesHandler(CollectionCaseApi api) {
        this.api = api;
    }

    @Override
    protected Mono<PaginationResponseCollectionCaseDTO> doHandle(ListCollectionCasesQuery query) {
        log.debug("Listing collection cases");
        return api.findAll(new FilterRequestCollectionCaseDTO(), UUID.randomUUID().toString());
    }
}
