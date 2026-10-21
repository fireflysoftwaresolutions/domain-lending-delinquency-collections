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

package io.fireflysoftware.domain.lending.collections.core.constants;

/**
 * Names shared by the collection case sagas, their steps, and the saga engine.
 */
public final class CollectionCaseSagaConstants {

    public static final String SAGA_REGISTER = "RegisterCollectionCaseSaga";
    public static final String SAGA_UPDATE_STATUS = "UpdateCollectionCaseStatusSaga";
    public static final String SAGA_UPDATE_RECORD = "UpdateCollectionCaseSaga";

    public static final String STEP_REGISTER = "registerCollectionCase";
    public static final String STEP_RETRIEVE = "retrieveCollectionCase";
    public static final String STEP_UPDATE_STATUS = "updateCollectionCaseStatus";
    public static final String STEP_RETRIEVE_FOR_UPDATE = "retrieveCollectionCaseForUpdate";
    public static final String STEP_UPDATE_RECORD = "updateCollectionCase";

    public static final String COMPENSATE_REMOVE = "removeCollectionCase";
    public static final String COMPENSATE_RESTORE = "restoreCollectionCaseStatus";
    public static final String COMPENSATE_RESTORE_RECORD = "restoreCollectionCase";

    public static final String EVENT_REGISTERED = "collection-cases.registered";
    public static final String EVENT_RETRIEVED = "collection-cases.retrieved";
    public static final String EVENT_STATUS_UPDATED = "collection-cases.status.updated";
    public static final String EVENT_RECORD_UPDATED = "collection-cases.updated";

    public static final String CTX_RECORD = "collection-casesRecord";
    public static final String CTX_PREVIOUS_STATUS = "previousCollectionCaseStatus";
    public static final String CTX_PREVIOUS_RECORD = "previousCollectionCaseRecord";

    public static final String STEP_RECORD_INITIAL_STATUS = "recordInitialCollectionCaseStatus";
    public static final String STEP_RECORD_STATUS_HISTORY = "recordCollectionCaseStatusHistory";
    public static final String COMPENSATE_REMOVE_INITIAL_STATUS = "removeInitialCollectionCaseStatus";
    public static final String COMPENSATE_REMOVE_STATUS_HISTORY = "removeCollectionCaseStatusHistory";
    public static final String EVENT_INITIAL_STATUS_RECORDED = "collection-cases.status.recorded";
    public static final String EVENT_STATUS_HISTORY_RECORDED = "collection-cases.status.history.recorded";

    private CollectionCaseSagaConstants() {
    }
}
