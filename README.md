<a href="https://fireflysoftware.io/openplatform/">
<picture>
  <source media="(max-width: 640px) and (prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-dark.svg">
  <source media="(max-width: 640px)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-light.svg">
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-dark.svg">
  <img src="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-light.svg" alt="Firefly OpenPlatform - Domain Lending - Delinquency Collections" width="100%">
</picture>
</a>

# Firefly OpenPlatform - Domain Lending - Delinquency Collections

Developed by the Firefly Software Foundation. [dev@fireflysoftware.io](mailto:dev@fireflysoftware.io)

[Website](https://fireflysoftware.io/) · [OpenPlatform](https://fireflysoftware.io/openplatform/) · [Module catalogue](https://fireflysoftware.io/openplatform/modules/)

Domain layer microservice for collection cases within the Firefly platform. This service orchestrates create, update, status change, close, and query against `core-lending-delinquency-collections` with compensating saga steps.

Repository: [https://github.com/fireflysoftwaresolutions/domain-lending-delinquency-collections](https://github.com/fireflysoftwaresolutions/domain-lending-delinquency-collections)

---

## Overview

`domain-lending-delinquency-collections` sits between experience services and `core-lending-delinquency-collections`. Write operations run through the FireflyFramework saga engine. Queries use the CQRS query bus. The domain depends on `core-lending-delinquency-collections-sdk` only. It does not import another core's internal packages. Identifiers that point at parties, contracts, or products stay UUIDs owned by those cores.

It creates, updates, changes status, and closes collection cases through `core-lending-delinquency-collections`. Promises, actions, and escalations stay on the core. Status history is written by the create and status sagas.

### Key features

- **Create** -- register a collection case through `core-lending-delinquency-collections`, with compensation that deletes the core write
- **Update** -- read the current collection case and write the new field values through `update` on `core-lending-delinquency-collections`. Status is left unchanged. Compensation writes the previous record back.
- **Status change** -- read the current collection case and update its status through `core-lending-delinquency-collections`. The create and status-change sagas also append a status history row on `core-lending-delinquency-collections`. Compensation restores the previous status and, when a history row was added, deletes that row.
- **Close** -- Close runs the status saga and writes CLOSED. The core has no cancel status. Remove deletes the case, which the core allows for every status. **Close** writes `CLOSED` when the core lifecycle allows that move.
- **Query** -- read one collection case by identifier
- **Compensation** -- a failed later step asks the core to undo the work already accepted
- **CQRS** -- commands and queries travel on separate buses
- **Reactive I/O** -- Spring WebFlux with virtual threads enabled

## Architecture

### Module structure

| Module | Purpose |
|--------|---------|
| `domain-lending-delinquency-collections-core` | Saga workflows, commands, handlers, and the application service |
| `domain-lending-delinquency-collections-interfaces` | Request contracts for a status change and a closing reason |
| `domain-lending-delinquency-collections-infra` | SDK client factory and downstream base-path configuration |
| `domain-lending-delinquency-collections-web` | Spring Boot application, REST API, actuator, and OpenAPI |
| `domain-lending-delinquency-collections-sdk` | Java client generated from the OpenAPI document |

### Dependency flow

```
web --> interfaces --> core --> infra --> core-lending-delinquency-collections-sdk
sdk (generated from openapi.yml)
```

### Saga map

| Saga | Core calls | Compensation |
|------|------------|--------------|
| `RegisterCollectionCaseSaga` | `create` on `CollectionCaseApi`, then `create1` on `CollectionStatusHistoryApi` | Deletes the status history row, then deletes the record |
| `UpdateCollectionCaseSaga` | Reads the current record, then writes field changes through `update` on `CollectionCaseApi`. Status is left unchanged. | Writes the previous record back through `update` |
| `UpdateCollectionCaseStatusSaga` | Reads the current record, updates its status through `update`, then appends status history through `create1` | Deletes the new history row and writes the previous status back through the core |
| Query | `getById` on `CollectionCaseApi` | None. A read has nothing to undo. |

Status values accepted by the core: `OPEN`, `CLOSED`, `LEGAL`, `RESTRUCTURED`, `SETTLED`.

## Setup

### Prerequisites

- Java 25
- Apache Maven 3.9+
- `core-lending-delinquency-collections-sdk` 26.10.3
- Apache Kafka on `localhost:9092` when event publishing is left at the default

### Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_ADDRESS` | `localhost` | Bind address |
| `SERVER_PORT` | `8048` | HTTP port |
| `ENDPOINT_CORE_LENDING_COLLECTIONS` | `http://localhost:8080` | Base URL of `core-lending-delinquency-collections` |

### Build and test

```bash
mvn clean verify
```

### Run

```bash
ENDPOINT_CORE_LENDING_COLLECTIONS=http://localhost:8080 mvn spring-boot:run -pl domain-lending-delinquency-collections-web
```

Swagger UI (non-production): [http://localhost:8048/swagger-ui.html](http://localhost:8048/swagger-ui.html)

## API

| Method | Path | Behavior |
|--------|------|----------|
| `POST` | `/api/v1/collection-cases` | Runs `RegisterCollectionCaseSaga` |
| `GET` | `/api/v1/collection-cases/{collectionCaseId}` | Queries the core |
| `PUT` | `/api/v1/collection-cases/{collectionCaseId}/status` | Runs `UpdateCollectionCaseStatusSaga` |
| `PUT` | `/api/v1/collection-cases/{collectionCaseId}` | Runs `UpdateCollectionCaseSaga` |
| `POST` | `/api/v1/collection-cases/{collectionCaseId}/close` | Close collection case. Runs the status saga when `core-lending-delinquency-collections` allows `CLOSED`. |
| `DELETE` | `/api/v1/collection-cases/{collectionCaseId}` | Deletes the record through `core-lending-delinquency-collections` when that core allows removal |

The status request body is `status` plus an optional `statusReason`.

Close writes CLOSED. The collections core does not restrict which status can follow another and has no cancel status. Remove deletes any case because the core delete has no status guard. A history reason uses the caller's statusReason when it is one of PROMISE_BROKEN, LEGAL_REFERRAL, PAYMENT_RECEIVED, or NO_CONTACT. Otherwise the domain records NO_CONTACT, which the core requires.
A closing command body is an optional `statusReason`.
