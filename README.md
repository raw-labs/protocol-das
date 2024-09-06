# DAS Protocol

This repository contains the protobuf definitions and gRPC service interfaces for the Data Access Service (DAS) used by RAW Labs. The protocol defines a set of services and message types that allow querying, modifying, and managing data across various data sources via gRPC.

## Features

- Implements gRPC services to interact with DAS, including:
  - `RegistrationService` for DAS registration and lifecycle management.
  - `TablesService` for table-related operations including scehma discovery and execution of CRUD operations. THe API roughly follows the requirements of PostgreSQL FDW.
  - `FunctionsService` for functions-related operations like fetching definitions, executing functions, etc.
- Defines core message types used throughout the protocol.

## Key Services

### RegistrationService
The `RegistrationService` handles the registration of DAS instances. It provides endpoints to register a new DAS and manage its lifecycle. Key message types include `RegisterRequest` and `RegisterResponse`.

For more details, see [registration_service.proto](src/main/protobuf/com/rawlabs/protocol/das/services/registration_service.proto).

### TablesService
The `TablesService` provides table-related operations for DAS-compliant data sources, including:

- `GetDefinitions`: Retrieves table schema definitions.
- `Execute`: Streams results from a DAS query.
- `Insert`, `Update`, `Delete`: CRUD operations on tables.
- `GetRelSize`: Returns size and row count of a table.
- Other auxiliary operations like sorting and path keys.

For more details, see [tables_service.proto](src/main/protobuf/com/rawlabs/protocol/das/services/tables_service.proto).

## Building the Project

This repository is an [sbt](https://www.scala-sbt.org/) project because it builds the Scala packages along with the protobuf definitions. To build the project:

1. Ensure you have `sbt` installed.
2. Run the following commands to compile the protobuf files and generate the Scala sources:

```bash
sbt publishLocal
```
This will generate the necessary gRPC and protobuf classes for both Scala and Java.

## Protobuf Files

The following protobuf files are key components of this repository:

-	[das.proto](https://github.com/raw-labs/protocol-das/blob/main/src/main/protobuf/com/rawlabs/protocol/das/das.proto): Defines the core messages and enumerations used in the DAS protocol.
-	[tables.proto](https://github.com/raw-labs/protocol-das/blob/main/src/main/protobuf/com/rawlabs/protocol/das/tables.proto): Defines the structure of tables and related operations.
-	[functions.proto](https://github.com/raw-labs/protocol-das/blob/main/src/main/protobuf/com/rawlabs/protocol/das/functions.proto): Defines function-related messages used in DAS.
-	[registration_service.proto](https://github.com/raw-labs/protocol-das/blob/main/src/main/protobuf/com/rawlabs/protocol/das/services/registration_service.proto): Protobuf definitions for DAS registration services.
-	[tables.proto](https://github.com/raw-labs/protocol-das/blob/main/src/main/protobuf/com/rawlabs/protocol/das/services/tables_service.proto): Protobuf definitions for table services.
-	[functions.proto](https://github.com/raw-labs/protocol-das/blob/main/src/main/protobuf/com/rawlabs/protocol/das/services/functions_service.proto): Protobuf definitions for function services.
