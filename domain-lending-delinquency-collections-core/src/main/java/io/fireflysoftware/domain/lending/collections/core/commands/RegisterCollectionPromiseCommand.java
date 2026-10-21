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

package io.fireflysoftware.domain.lending.collections.core.commands;

import io.fireflysoftware.core.lending.collections.sdk.model.CollectionPromiseDTO;
import org.fireflyframework.cqrs.command.Command;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persists a payment promise on a collection case.
 */
public record RegisterCollectionPromiseCommand(
        UUID collectionCaseId,
        BigDecimal promisedAmount,
        LocalDateTime promisedDate
) implements Command<CollectionPromiseDTO> {
}
