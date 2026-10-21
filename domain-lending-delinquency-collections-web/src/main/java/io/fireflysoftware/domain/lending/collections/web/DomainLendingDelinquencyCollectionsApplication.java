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

package io.fireflysoftware.domain.lending.collections.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * Entry point for domain-lending-delinquency-collections.
 */
@SpringBootApplication(scanBasePackages = {
        "io.fireflysoftware.domain.lending.collections",
        "org.fireflyframework.web"
})
@EnableWebFlux
@ConfigurationPropertiesScan(basePackages = "io.fireflysoftware.domain.lending.collections")
@OpenAPIDefinition(
info = @Info(
        title = "Domain Lending Delinquency Collections API",
        version = "1.0.0",
        description = "Orchestrates collection cases through core-lending-delinquency-collections. It creates, updates, changes status, and closes collection cases through the delinquency collections core. Party, contract, and product identifiers stay UUIDs.",
        contact = @Contact(
                name = "Firefly Software Foundation",
                email = "dev@fireflysoftware.io"
        )
),
servers = {
        @Server(
                url = "https://openplatform.fireflysoftware.io/domain-lending-delinquency-collections",
                description = "Development Environment"
        ),
        @Server(
                url = "/",
                description = "Local Development Environment"
        )
}
)

public class DomainLendingDelinquencyCollectionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DomainLendingDelinquencyCollectionsApplication.class, args);
    }
}
