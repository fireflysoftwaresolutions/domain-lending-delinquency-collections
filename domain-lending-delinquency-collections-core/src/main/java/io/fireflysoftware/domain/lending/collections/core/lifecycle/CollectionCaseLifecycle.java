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

package io.fireflysoftware.domain.lending.collections.core.lifecycle;

import io.fireflysoftware.core.lending.collections.sdk.model.CollectionCaseDTO;

/**
 * Status values stored by {@code core-lending-delinquency-collections}.
 * That core accepts any status on update and deletes a record in any status.
 */
public final class CollectionCaseLifecycle {

    private CollectionCaseLifecycle() {
    }

    public static boolean canRemove(CollectionCaseDTO.CollectionStatusEnum status) {
        return status == null || status.getValue() != null;
    }

    public static boolean canTransition(CollectionCaseDTO.CollectionStatusEnum from, CollectionCaseDTO.CollectionStatusEnum to) {
        return to != null;
    }
}
