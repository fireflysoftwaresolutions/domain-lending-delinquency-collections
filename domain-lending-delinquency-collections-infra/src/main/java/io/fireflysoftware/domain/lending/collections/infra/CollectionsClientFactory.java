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

package io.fireflysoftware.domain.lending.collections.infra;

import io.fireflysoftware.core.lending.collections.sdk.api.CollectionCaseApi;
import io.fireflysoftware.core.lending.collections.sdk.api.CollectionPromiseApi;
import io.fireflysoftware.core.lending.collections.sdk.api.CollectionStatusHistoryApi;
import io.fireflysoftware.core.lending.collections.sdk.invoker.ApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Exposes the {@code core-lending-delinquency-collections} SDK clients used by the saga handlers.
 */
@Component
public class CollectionsClientFactory {

    private final ApiClient apiClient;

    public CollectionsClientFactory(CollectionsCoreProperties properties) {
        this.apiClient = new ApiClient();
        this.apiClient.setBasePath(properties.getBasePath());
    }

    /**
     * Creates the primary core client.
     *
     * @return the API client
     */
    @Bean
    public CollectionCaseApi collectionCaseApi() {
        return new CollectionCaseApi(apiClient);
    }

    /**
     * Creates the status history client.
     *
     * @return the status history API
     */
    @Bean
    public CollectionStatusHistoryApi collectionStatusHistoryApi() {
        return new CollectionStatusHistoryApi(apiClient);
    }

    /**
     * Creates the payment-promise client.
     *
     * @return the promise API
     */
    @Bean
    public CollectionPromiseApi collectionPromiseApi() {
        return new CollectionPromiseApi(apiClient);
    }

}
