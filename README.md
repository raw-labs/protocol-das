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
- [Type System](#type-system)
- [gRPC Status Codes](#grpc-status-codes)
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

## Type System

The DAS Protocol defines a set of types under [`types.proto`](./src/main/protobuf/com/rawlabs/protocol/das/v1/types/types.proto). At the root is the `Type` message, which uses a `oneof` to represent different categories of data (e.g., `IntType`, `StringType`, `RecordType`, etc.). Each specific type message includes a `nullable` flag indicating whether values of that type can be `NULL`.

### Primitive Types

- **ByteType / ShortType / IntType / LongType**  
  Integer types of varying precision.  
- **FloatType / DoubleType / DecimalType**  
  Floating-point and arbitrary precision decimal numbers.  
- **BoolType**  
  Boolean type.  
- **StringType / BinaryType**  
  Textual and binary data.  
- **DateType / TimeType / TimestampType / IntervalType**  
  Common date/time representations.

### RecordType

- **RecordType** is designed to hold a collection of named fields, each of which has its own `Type`. Fields are described in `AttrType`, which includes `name` and `tipe` (i.e., type).

  ```proto
  message RecordType {
    repeated AttrType atts = 1;
    bool nullable = 2;
  }

  message AttrType {
    string name = 1;
    Type tipe = 2;
  }
  ```

- **Empty Fields as an “Open” Record**  
  While a typical `RecordType` explicitly enumerates all of its fields, **a `RecordType` with zero fields (`atts` repeated field is empty) is treated as a special “open” or “dynamic” record type**. In other words, the record may contain *any* number of fields (including none), with arbitrary names and types—much like a map of `[string => any]`.  
  This approach reuses the empty record concept to represent a flexible schema. It can be used when the exact structure of the data is unknown or highly variable.

### ListType

- **ListType** represents a homogeneously typed collection:
  ```proto
  message ListType {
    Type inner_type = 1;
    bool nullable = 2;
  }
  ```
  For example, a list of `IntType` or a list of `RecordType` elements.

By defining these types in protobuf, DAS ensures a language-agnostic, strongly typed model for data exchange. This type system underlies the DAS schema-discovery features, query operations, and function signatures, offering consistent typing across different clients and servers.

---

## gRPC Status Codes

The DAS Protocol relies on specific gRPC status codes to convey outcomes. Below are guidelines on what each code means, when the DAS server should return it, and how the client should respond.

1. `UNAVAILABLE`
  * *Server*: Return `UNAVAILABLE` if the DAS server is temporarily unable to handle requests (e.g., during a rolling upgrade or transient failure).
  * *Client*: On receiving `UNAVAILABLE`, the client should retry the call for a limited period. Consider implementing exponential backoff or a retry strategy to handle this condition gracefully.

2. `NOT_FOUND`
  * *Server*: Return `NOT_FOUND` if the DAS server cannot find a requested resource (e.g., the DAS ID is unrecognized, possibly after a server restart).
  * *Client*: On receiving `NOT_FOUND`, re-register the DAS or recreate the resource if appropriate. For example, if the server has lost the DAS registration, the client should invoke RegistrationService.Register again.

4. `UNAUTHENTICATED` / `PERMISSION_DENIED`
  * *Server*:
    * `UNAUTHENTICATED` if the client fails to provide valid credentials or the server cannot authenticate the request.
    * `PERMISSION_DENIED` if the client’s credentials are valid but lack sufficient privileges to access the requested resource.
  *	*Client*:
    *	For `UNAUTHENTICATED`, re-authenticate with valid credentials or tokens.
    *	For `PERMISSION_DENIED`, request additional permissions or contact an administrator to obtain the necessary access rights.

5. `INVALID_ARGUMENT`
  *	*Server*: Return `INVALID_ARGUMENT` if the request is syntactically correct but contains invalid data (e.g., invalid field values, improper query operators, or out-of-range parameters).
  *	*Client*: On `INVALID_ARGUMENT`, correct the input and reissue the request. The error details (if provided in the RPC metadata) can help identify which argument caused the failure.

6. `UNIMPLEMENTED`
  *	*Server*: Return `UNIMPLEMENTED` if a particular functionality is not supported by the DAS server (e.g., if INSERT or UPDATE operations are disabled in the server’s configuration or not implemented at all).
  *	*Client*: Upon receiving `UNIMPLEMENTED`, fall back to alternative methods or notify the user that the operation is not available. Clients should not retry the same request unless there is a reason to believe support might be dynamically enabled.

By adhering to these conventions, both server and client implementations of the DAS Protocol can handle edge cases and transient failures more predictably, ensuring a more robust and user-friendly experience.

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
