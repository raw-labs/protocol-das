# DAS Protocol

[![License](https://img.shields.io/badge/License-BSL%201.1-blue.svg)](./licenses/BSL.txt)

The **DAS Protocol** is a set of Protocol Buffers (protobuf) and gRPC service definitions that describe a **Data Access Service (DAS)** API used by [RAW Labs](https://www.raw-labs.com). It provides a unified, schema-aware interface for querying and managing data across multiple data sources.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Services](#services)
  - [RegistrationService](#registrationservice)
  - [TablesService](#tablesservice)
  - [FunctionsService](#functionsservice)
  - [HealthCheckService](#healthcheckservice)
- [File Organization](#file-organization)
- [Building](#building)
- [Using the Generated Code](#using-the-generated-code)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

The DAS Protocol acts as a standard contract for both clients and servers to communicate with data sources via gRPC. It allows:

- **Dynamic discovery** of tables and functions.
- **Schema-aware queries** with typed columns and constraints.
- **CRUD operations** on tables (insert, update, delete).
- **Function execution** with typed parameters and return values.
- **Health checks** to monitor the service availability.

By defining these capabilities in protobuf, we enable language-agnostic integration, so developers in different ecosystems can leverage the DAS functionality without being tied to a single framework or language.

---

## Features

1. **Schema Discovery**  
   Query table or function definitions to understand the shape and semantics of the data.

2. **Typed Data Model**  
   The protocol includes comprehensive type definitions in [`types.proto`](./src/main/protobuf/com/rawlabs/protocol/das/v1/types/types.proto) and [`values.proto`](./src/main/protobuf/com/rawlabs/protocol/das/v1/types/values.proto), supporting everything from basic scalar types to complex records and lists.

3. **Rich Query Language**  
   Build queries with operators like `EQUALS`, `LESS_THAN`, `LIKE`, etc. Define sorting, path keys, and retrieve row estimates.

4. **CRUD Operations**  
   Perform create, read, update, and delete operations on DAS-managed tables.

5. **Dynamic Function Invocation**  
   Fetch function definitions and execute them with named or positional arguments.

6. **Configurable Environment**  
   Pass environment variables or metadata to the service through the [Environment](./src/main/protobuf/com/rawlabs/protocol/das/v1/common/environment.proto) messages.

7. **Health Checks**  
   Use the `HealthCheckService` to ensure the service is running properly.

---

## Services

### RegistrationService

- **Purpose:** Register or unregister a DAS instance.
- **Key RPCs:**
  - `Register`
  - `Unregister`

```proto
service RegistrationService {
    rpc Register (RegisterRequest) returns (RegisterResponse);
    rpc Unregister (DASId) returns (UnregisterResponse);
}
```

- **Example Use Case:** Initialize a new DAS with configuration options, or tear it down from the registry.

---

### TablesService

- **Purpose:** Interact with data tables through a uniform interface.
- **Key Operations:**
  - `GetTableDefinitions` – retrieve metadata (columns, descriptions, etc.)
  - `ExecuteTable` – perform a query and stream back result rows
  - `InsertTable`, `UpdateTable`, `DeleteTable` – CRUD operations
  - `GetTableEstimate` – estimate row counts before running queries
- **Example Protos:**
  - [`tables_service.proto`](./src/main/protobuf/com/rawlabs/protocol/das/v1/services/tables_service.proto)
  - [`tables.proto`](./src/main/protobuf/com/rawlabs/protocol/das/v1/tables/tables.proto)

```proto
service TablesService {
    rpc GetTableDefinitions (GetTableDefinitionsRequest) returns (GetTableDefinitionsResponse);
    rpc ExecuteTable (ExecuteTableRequest) returns (stream Rows);
    // ...
}
```

---

### FunctionsService

- **Purpose:** Discover and execute user-defined functions available within DAS.
- **Key Operations:**
  - `GetFunctionDefinitions` – list function signatures and metadata
  - `ExecuteFunction` – call a function by name with typed parameters

```proto
service FunctionsService {
    rpc GetFunctionDefinitions (GetFunctionDefinitionsRequest) returns (GetFunctionDefinitionsResponse);
    rpc ExecuteFunction (ExecuteFunctionRequest) returns (ExecuteFunctionResponse);
}
```

---

### HealthCheckService

- **Purpose:** Provide basic health status checks for a DAS instance.
- **Key RPC:**
  - `Check` – returns a simple `SERVING` or `NOT_SERVING` status

```proto
service HealthCheckService {
  rpc Check (HealthCheckRequest) returns (HealthCheckResponse);
}
```

---

## gRPC Status Codes

The following gRPC Status Code must be handled by clients of the DAS Protocol:
* `UNAVAILABLE`: When a DAS server is not available temporarily, which can occur during a rolling upgrade, the DAS client should retry the call for a period of time.
* `UNAUTHENTICATED` or `PERMISSION_DENIED`: When a DAS server is not able to access a data source, it may return an unauthenticated or permission denied response, indicating that additional options configuration is needed.
* `NOT_FOUND`: When a DAS server does not find the requested "DAS id", which can occur after a service restart, this response can be sent; the client should then re-register the DAS.

---

## File Organization

All protobuf definitions live under [`src/main/protobuf/com/rawlabs/protocol/das/v1/`](./src/main/protobuf/com/rawlabs/protocol/das/v1/).

- **`common/`**: Common messages like [`DASId`](./src/main/protobuf/com/rawlabs/protocol/das/v1/common/das.proto) and [`Environment`](./src/main/protobuf/com/rawlabs/protocol/das/v1/common/environment.proto).
- **`tables/`**: Table-related messages ([`tables.proto`](./src/main/protobuf/com/rawlabs/protocol/das/v1/tables/tables.proto)).
- **`functions/`**: Function-related messages ([`functions.proto`](./src/main/protobuf/com/rawlabs/protocol/das/v1/functions/functions.proto)).
- **`query/`**: Query-related messages for operators, sorting, path keys, etc. ([`query.proto`](./src/main/protobuf/com/rawlabs/protocol/das/v1/query/query.proto)).
- **`services/`**: gRPC service definitions ([`registration_service.proto`](./src/main/protobuf/com/rawlabs/protocol/das/v1/services/registration_service.proto), [`tables_service.proto`](./src/main/protobuf/com/rawlabs/protocol/das/v1/services/tables_service.proto), etc.).
- **`types/`**: Core type system definitions (`types.proto` and `values.proto`).

---

## Building for Scala

This repository uses [sbt](https://www.scala-sbt.org/) to:

1. **Compile the protobuf files**
2. **Generate Scala/Java gRPC stubs**
3. Optionally **publish** artifacts locally or to a repository

### Prerequisites

- [sbt](https://www.scala-sbt.org/) (1.x or later)
- Protobuf compiler (if you plan to manually compile `.proto` files in other languages)

### Steps

1. **Clone** this repository:
   ```bash
   git clone https://github.com/raw-labs/protocol-das.git
   cd protocol-das
   ```
2. **Publish locally**:
   ```bash
   sbt publishLocal
   ```
   This compiles the protobuf files and generates Scala/Java classes. You can then reference the published artifacts from another sbt-based project by adding the corresponding coordinates to your dependencies.

---

## Using the Generated Code in Scala

If you publish the generated artifacts to your local or remote repository:

1. Add a dependency in your `build.sbt` (example coordinates shown—adjust as needed):
   ```scala
   libraryDependencies ++= Seq(
     "com.raw-labs" %% "protocol-das" % "0.1.0-SNAPSHOT"
   )
   ```
2. Once included, you can import classes like `com.rawlabs.protocol.das.v1.services.TablesServiceGrpc` and `com.rawlabs.protocol.das.v1.services.RegistrationServiceGrpc` in your Scala/Java code.

### Other Languages

Because these services and messages are defined via protobuf, you can also generate client/server stubs in many other languages (e.g., Python, C#, Go). You’ll need:

- The `.proto` files found in this repo (or from your local build).
- The relevant protobuf/gRPC code generators in your language of choice.

---

## License

Use of this software is governed by the [Business Source License 1.1](./licenses/BSL.txt). As of the **Change Date** specified in that file, this software will be governed by the [Apache License, Version 2.0](./licenses/APL.txt).

For detailed information, see the [BSL License file](./licenses/BSL.txt).

---

**Questions?**  
If you have any questions or need support, please open an issue in this repository or [reach out to us](mailto:hello@raw-labs.com). We look forward to your feedback and contributions!
