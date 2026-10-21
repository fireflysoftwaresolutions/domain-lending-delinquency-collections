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

package io.fireflysoftware.domain.lending.collections.web.controllers;

import io.fireflysoftware.core.lending.collections.sdk.model.CollectionCaseDTO;
import io.fireflysoftware.domain.lending.collections.core.commands.CreateCollectionCaseCommand;
import io.fireflysoftware.domain.lending.collections.core.commands.UpdateCollectionCaseCommand;
import io.fireflysoftware.domain.lending.collections.core.commands.UpdateCollectionCaseStatusCommand;
import io.fireflysoftware.domain.lending.collections.core.services.CollectionCasesService;
import io.fireflysoftware.domain.lending.collections.interfaces.dtos.ReasonRequest;
import io.fireflysoftware.domain.lending.collections.interfaces.dtos.StatusChangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Domain API for collection cases.
 */
@RestController
@RequestMapping("/api/v1/collection-cases")
@Tag(name = "CollectionCases", description = "Domain orchestration for collection cases")
public class CollectionCasesController {

    private final CollectionCasesService service;

    public CollectionCasesController(CollectionCasesService service) {
        this.service = service;
    }

    @Operation(summary = "Create collection case", description = "Runs the register saga against core-lending-delinquency-collections.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CollectionCaseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<CollectionCaseDTO>> create(@RequestBody CollectionCaseDTO body) {
        return service.create(new CreateCollectionCaseCommand(body))
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
    }

    @Operation(summary = "Get collection case", description = "Reads one record from core-lending-delinquency-collections.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CollectionCaseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @GetMapping(value = "/{collectionCaseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<CollectionCaseDTO>> get(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID collectionCaseId) {
        return service.get(collectionCaseId)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Update status", description = "Runs the status saga against core-lending-delinquency-collections.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CollectionCaseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PutMapping(value = "/{collectionCaseId}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<CollectionCaseDTO>> updateStatus(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID collectionCaseId,
            @RequestBody StatusChangeRequest request) {
        return service.updateStatus(new UpdateCollectionCaseStatusCommand(collectionCaseId, request.getStatus(), request.getStatusReason()))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Update collection case", description = "Runs the update saga against core-lending-delinquency-collections. Status is left unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CollectionCaseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PutMapping(value = "/{collectionCaseId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<CollectionCaseDTO>> update(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID collectionCaseId,
            @RequestBody CollectionCaseDTO body) {
        return service.update(new UpdateCollectionCaseCommand(collectionCaseId, body))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Close collection case", description = "Closes a collection case by writing CLOSED when the delinquency collections core allows that status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CollectionCaseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PostMapping(value = "/{collectionCaseId}/close", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<CollectionCaseDTO>> close(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID collectionCaseId,
            @RequestBody ReasonRequest request) {
        return service.close(collectionCaseId, request.getStatusReason())
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Remove collection case", description = "Deletes the record through core-lending-delinquency-collections when that core allows removal.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Removed"),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @DeleteMapping("/{collectionCaseId}")
    public Mono<ResponseEntity<Void>> remove(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID collectionCaseId) {
        return service.remove(collectionCaseId)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
