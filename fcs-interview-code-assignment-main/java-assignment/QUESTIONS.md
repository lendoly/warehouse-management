# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
The codebase currently has four distinct approaches as fare as I could check:

  - On Warehouse: Full hexagonal architecture — domain model (Warehouse) is separate from the JPA entity
    (DbWarehouse), WarehouseRepository implements the WarehouseStore port, and business logic lives
    in dedicated use cases.

  - On Product: Minimal repository (ProductRepository is essentially empty), with quries and
    logic co-located in the REST resource.

  - On Store: there is a more advance version of what is in the Product, making the entity act as its own DAO (Active Record
    pattern). Queries are called directly on the class (Store.listAll(), Store.findById()).

  - On FulfillmentUnit: No repository abstraction at all — queries are written inline inside the REST
    resource handler, mixed with business rule validation.

Inconsistency is the core problem. Developers jumping between modules face a different mental model each time, which slows onboarding, increases the chance of bugs, and makes testing harder.

So in short, yes I will propose to refactor this repo. What I would do:

  1. Standardize on the Warehouse pattern (hexagonal / port-adapter) for any module that has non-trivial business logic. The FulfillmentUnit is the most urgent candidate: complex capacity and assignment constraints are currently embedded in a REST handler, making it way more difficult to test without an HTTP stack. 

  2. For genuinely simple CRUD modules (Product, Store), a lightweight repository interface is sufficient — no need to go full hexagonal. The key win is having a typed boundary between the persistence layer and the rest of the code.

  3. Enforce the chosen pattern via a lightweight architectural fitness function (e.g. ArchUnit rules) so the inconsistency does not creep back in as the team grows.

The goal is not purity for its own sake, but reducing cognitive overhead and making the codebase predictably testable at every layer.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Spec-first (OpenAPI YAML -> code generation, as used by Warehouse):

  Pros:
  - The contract is the single source of truth. Consumers (other teams, frontend, external clients) can rely on the YAML file without reading implementation code. Can also be used as documentation.
  - Enables parallel development: consumers generate client stubs from the spec while the backend is still being built.
  - Validation of requests/responses against the schema is automatic.
  - API versioning and breaking-change detection are straightforward — diff the YAML.
  - Documentation is always in sync with what was committed, not what someone remembered to annotate.

  Cons:
  - More upfront ceremony: writing the YAML, configuring code generation, and keeping the generator output out of version control (or deliberately in it) requires tooling discipline.
  - Generated interfaces can feel disconnected from the implementation.
  - Minor changes to the spec trigger regeneration, which can cascade into compile errors if not managed carefully.

Code-first (annotations + hand-written handlers, as used by Product and Store):

  Pros:
  - Faster to get started, write the resource class, run the app, done.
  - The code is the spec, there is no risk of them diverging because there is only one artifact.
  - Easier for developers who are less familiar with OpenAPI tooling.

  Cons:
  - Documentation (if generated from annotations like @Operation) tends to drift because it is optional and easy to skip.
  - No formal contract means consumers must either read the source or reverse-engineer the API from traffic — both are fragile. We can mantain an external documation but will make everything more work
  - Harder to coordinate with teams consuming the API before the implementation exists.
  - Harder to enforce consistency in error responses, pagination, naming conventions, etc.

What I will choose:

  Spec-first for any API with external consumers or that crosses a team boundary. A committed YAML file makes the contract reviewable in pull requests and lets consumers validate integration independently of the server.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Priority order, from highest return on investment to lowest:

1. Unit tests for domain logic and use cases (highest priority)
   The Warehouse use cases (CreateWarehouseUseCase, ReplaceWarehouseUseCase, ArchiveWarehouseUseCase) and WarehouseValidator contain the most business-critical rules: capacity limits, location constraints, uniqueness, archival logic. Because the hexagonal architecture isolates these from infrastructure, they can be tested with plain JUnit — no database, no HTTP stack, millisecond feedback. This is where I would strat working on.

   The FulfillmentUnit constraint logic (max 2 warehouses per product per store, max 3 warehouses per store, etc.) is currently embedded in the REST resource and therefore untestable without HTTP. After the refactor described in the first question, those rules would also be covered here.

2. Integration tests for the persistence layer
   Repository implementations need to be verified against a real database (Test/Sraging enviroment database) to catch query mistakes, constraint violations, and transaction behaviour. One integration test suite per repository is sufficient — focus on the non-trivial queries (soft-delete filters, location-count queries, composite unique constraints).

3. API / component tests for the REST layer
   A small set of @QuarkusTest tests that exercise the full HTTP stack (request -> resource -> use case -> repository -> DB -> response) against the most important happy paths and error cases per endpoint. These tests are slower but catch wiring mistakes (e.g. wrong HTTP status codes, missing validation   annotations, serialisation issues). For the Warehouse API, the generated OpenAPI interface makes it straightforward to validate that the implementation matches the contract.

4. Contract tests (if there are external consumers)
   If other services call these APIs, consumer-driven contract tests (e.g. Pact) ensure that changes to the server do not silently break consumers. Given the OpenAPI spec already exists for Warehouse,  generating a mock server from it and running consumer tests against it is low-effort.

What I would not prioritize without a specific reason:
   - End-to-end tests that spin up the entire system including the legacy store manager — too slow and  brittle for a first pass.
   - 100% line coverage — it is a vanity metric. Untested glue code is less risky than untested   business rules.

Keeping coverage effective over time:
   - Enforce a minimum branch/mutation coverage threshold in CI for the domain package specifically (not the entire codebase), so the most valuable tests cannot regress.
   - Run mutation testing (e.g. PIT / Pitest) periodically to verify that tests actually fail when  the code is wrong — line coverage alone does not tell you that.
   - Require tests for any bug fix: the fix is not merged without a test that would have caught the bug. This grows the suite organically toward real-world failure modes.
   - Keep integration tests in a separate Maven profile so the fast unit tests remain a sub-second feedback loop during development.
```
