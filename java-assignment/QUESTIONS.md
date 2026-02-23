# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, I have consolidated the database access strategy. The codebase currently mixes two 
approaches side by side: the Store and Product entities use the Active Record pattern 
(PanacheEntity, with persistence logic embedded in the entity class itself), while the 
Warehouse domain uses the Repository pattern (PanacheRepository + a separate DbWarehouse 
entity + a WarehouseStore port).

The Warehouse approach is clearly superior for a production codebase and I would migrate 
Store and Product to match it. Here is my thoughts on the advantages of the Repository pattern in this context:

1. Testability: Active Record bakes persistence into the entity. Testing StoreResource 
   requires a running database or elaborate mocking of static Panache methods. The 
   Repository pattern, by contrast, decouples the domain object (Warehouse) from JPA 
   concerns (DbWarehouse), making every use case trivially unit-testable with a mocked 
   WarehouseStore interface.

2. Separation of Concerns / Hexagonal Architecture: The Warehouse module already follows 
   the ports-and-adapters structure (domain models, ports/interfaces, adapters/implementations). 
   This makes swapping the persistence layer (e.g., switching to a document store or 
   event store) a contained change. Active Record ties the domain model to the ORM 
   framework permanently.

3. Clear Domain Model: Having a separate `Warehouse` (domain) vs `DbWarehouse` (persistence) 
   allows the domain object to evolve independently of database schema constraints 
   (column types, indexes, etc.).

The one trade-off i would like to mention is verbosity: for truly simple CRUD entities with no business rules, 
Active Record is faster to write. But at any meaningful scale the Repository pattern 
pays for itself in maintainability.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
OpenAPI-First (Warehouse):
  Pros:
  - The API contract is the single source of truth and is independent of any 
    implementation language or framework. Frontend, mobile, and partner teams can 
    consume and mock the spec before the server is built.
  - Breaking-change detection is automated: diffs on the YAML immediately flag 
    contract violations.
  - Generated server stubs (WarehouseResource interface) enforce that every endpoint 
    defined in the spec is implemented; forgetting to implement one is a compile error.
  - Consistent documentation: the spec doubles as living documentation and can be 
    published to a developer portal (e.g., Stoplight, Redoc) with no extra effort.

  Cons:
  - Upfront tooling setup and generator configuration.
  - Generated code can be verbose and less idiomatic; customising it requires 
    Mustache template overrides.
  - The YAML and the implementation can drift if engineers edit the generated 
    interfaces directly instead of updating the spec first.

Code-First (Product, Store):
  Pros:
  - Faster to start: write a JAX-RS class and annotations, deploy, done.
  - Less tooling overhead.

  Cons:
  - The API contract lives only implicitly in annotations scattered across classes.
  - Consumer teams have no contractual artifact to depend on until someone runs a 
    doc generator (e.g., Swagger-UI via the quarkus-smallrye-openapi extension).
  - Refactoring an endpoint silently breaks consumers without any build-time warning.
  - Testing endpoint contracts requires running the application.

My choice: OpenAPI-First for any externally-consumed or team-shared API. The 
up-front cost is low and the long-term benefits (contract clarity, consumer 
autonomy, automated validation) are substantial. For internal, single-consumer 
endpoints with genuinely simple CRUD logic and low change frequency, code-first is 
a pragmatic choice — but the moment an external team depends on that endpoint it 
should be promoted to a spec-first design.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Given the constraint of limited time and the nature of this codebase, I would 
prioritise in this order:

1. Unit Tests for Domain Use Cases (highest ROI):
   Use cases encode all business rules (capacity limits, stock matching, location 
   validation). These are pure Java classes with no framework dependencies, so tests 
   run in milliseconds without a container or database. Mocking the ports (WarehouseStore, 
   LocationResolver) keeps them fully isolated. This is where the bugs are — and where 
   they are cheapest to catch.

2. Integration Tests for REST Endpoints (high confidence):
   QuarkusTest / QuarkusIntegrationTest spins up an in-memory H2 or dev-services 
   Postgres container and exercises the full HTTP → Use Case → DB path. These validate 
   that the DI wiring, transaction boundaries, and serialisation all work end-to-end. 
   I'd cover: the happy path for each verb, each validation error returning the correct 
   status code, and the archive/replace lifecycle.

3. Unit Tests for Adapter Classes:
   WarehouseRepository query correctness (filtering by archivedAt, findByBusinessUnitCode) 
   can be tested cheaply with @QuarkusTest + TestTransaction. StoreEventObserver's 
   routing logic (CREATED vs UPDATED) is a trivial pure-Java unit test.

4. Contract Tests (stretch goal):
   If this API were consumed by external teams I would add Pact or Spring Cloud Contract 
   tests to prevent silent breaking changes.

To maintain coverage over time:
- Enforce JaCoCo line coverage ≥ 80 % as a maven build gate (already configured).
- Require tests for every pull request touching use-case or adapter code.
- Treat a failing test as a higher-priority task than new feature work.
- Review coverage reports in CI/CD and address newly uncovered paths before each release.
```