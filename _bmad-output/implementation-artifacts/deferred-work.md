# Deferred Work

Entries appended by the bmad-build review loop. Do not modify existing entries.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-monorepo-starter-template-theo-structural-seed.md`
  summary: No CI pipeline runs `mvn package`/`mvn test`, `npm run build`/`npm test`/`npm run lint` automatically.
  evidence: Repo has no `.github/` or other workflow config; the story's core promise is a from-zero buildable+migratable scaffold, and nothing guards that promise on change. A CI job with a service MySQL running both profiles would also automate matrix rows 1, 2 and 4 (fresh-DB reproducibility).

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-monorepo-starter-template-theo-structural-seed.md`
  summary: The Vite `/api`→:8080 proxy pairing with `server.port` has no automated check — verified only by the documented manual curl.
  evidence: `grep 8080 frontend` hits only `vite.config.ts` and prose in `App.tsx`; jsdom tests never traverse the dev-server proxy and backend tests never reference 5173. Change `server.port` to 8081 and every suite stays green while all dev `/api/v1` calls break. Revisit when epic-1 endpoints make FE→BE calls real.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-monorepo-starter-template-theo-structural-seed.md`
  summary: Nothing verifies the MSW "dev + opt-in only" gate keeps mocks out of production builds.
  evidence: No test boots `main.tsx` (App.test imports App directly) and `vite build` type-checks without evaluating `import.meta.env.DEV`; dropping the DEV condition ships the worker in the prod bundle with both suites green. Revisit when real handlers/queries land (broken gate becomes observable as mock data in real usage).

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-monorepo-starter-template-theo-structural-seed.md`
  summary: V1 schema lacks extra integrity guards — no UNIQUE (PolicyID, TypeID, RuleType) on `policy_rules`, no CHECKs for `reservations.EndDate >= StartDate`, `extensions.NewEndDate > OldEndDate`, `units.SizeM2 > 0`.
  evidence: Verified in `V1__init_schema.sql`; the frozen intent pins V1 to the dbml model + exactly 7 AD-6 deltas, so these guards are an 8th delta the intent excludes. Epic 8 (policy editing, story 8.1 validate-before-save) and the epic 2/4 services are the natural enforcement points; decide service-layer validation vs a later delta migration there.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-monorepo-starter-template-theo-structural-seed.md`
  summary: `activity_logs` has no timestamp column, leaving epic 9 story 9.2 (Login History) without a time dimension — ordering rests on auto-increment LogID.
  evidence: Column list verified faithful to the dbml (frozen V1); every seeded LOGIN/LOGIN_FAILED row carries no time. Renegotiate the schema (new delta migration + AD-6 amendment) when story 9.1/9.2 is planned.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-monorepo-starter-template-theo-structural-seed.md`
  summary: `contracts/openapi.yaml` has no reusable `components.responses` (Unauthorized/Forbidden/Conflict/ValidationError) and MSW has no Error-envelope handler example.
  evidence: Verified — skeleton ships Error/ListEnvelope schemas only, as the spec required; the response catalog and error-mock pattern pay off when the first sprint PR adds paths (epic 1 auth endpoints), which is when to add them.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-2-be-foundations-error-list-envelope-logservice-append-only.md`
  summary: `@ExceptionHandler(Exception.class)` will swallow `AccessDeniedException` raised by method security (`@PreAuthorize`) as 500 instead of 403 once story 1.3 adds the permission matrix.
  evidence: Verified in GlobalExceptionHandler — the catch-all maps every Exception to INTERNAL_ERROR; no production path triggers it today (the real SecurityConfig chain has no role rule), but 1.3's matrix makes it certain. Fix there: add a rethrow handler for AccessDeniedException (letting the filter chain's EnvelopeAccessDeniedHandler render 403) or authorize only via authorizeHttpRequests; add a MockMvc test driving it through the real chain.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-2-be-foundations-error-list-envelope-logservice-append-only.md`
  summary: `ListQuery`/`pageSize` has no upper bound — the resolver accepts `pageSize=10000000` and the openapi ListEnvelope declares no `maximum`, so the first real list endpoint gets an unbounded LIMIT plus a `(page-1)*pageSize` int-overflow risk.
  evidence: Verified — ListQuery enforces only >= 1 and openapi pageSize carries `minimum: 1, default: 25` alone; no SQL consumer exists yet so nothing breaks today. Cap value is a product decision: pick it (plus mirror as `maximum` in the contract) in the first story that adds a real list endpoint (epic 2 browse/lists).

- source_spec: `_bmad-output/implementation-artifacts/spec-1-2-be-foundations-error-list-envelope-logservice-append-only.md`
  summary: Client-origin framework exceptions outside the frozen matrix rows — `HttpMediaTypeNotAcceptableException` (406, Accept header excludes JSON), missing `@RequestHeader`/`@RequestCookie` bindings, async timeout — fall into the catch-all and surface as 500 instead of a 4xx envelope.
  evidence: Verified in GlobalExceptionHandler — no handlers exist for them; nothing triggers these today (no endpoint reads headers, FE always accepts JSON). Note 406 handling needs care: writing the JSON envelope against a non-JSON Accept requires the ErrorEnvelopeWriter path, and a new NOT_ACCEPTABLE enum value would amend the openapi catalog — design it when a real endpoint needs it.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-2-be-foundations-error-list-envelope-logservice-append-only.md`
  summary: Class-level Bean Validation constraints (cross-field rules) map to VALIDATION_FAILED with no fieldErrors and the constraint message dropped — `onValidation` reads only `getFieldErrors()`.
  evidence: Verified in GlobalExceptionHandler; no DTO uses class-level constraints yet, but epic 2/4 cross-field rules (EndDate >= StartDate, extension bounds) are certain to. Representation of a field-less error is a shape decision (extend FieldError vs a new envelope field) the spec's frozen matrix ("fieldErrors đủ từng field") does not settle — decide in the first story that adds a cross-field DTO.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-3-auth-backend-jwt-permission-matrix-login-audit.md`
  summary: Backend SecurityConfig lacks CORS configuration and OPTIONS preflight permit, which will block cross-origin browser fetch calls from frontend dev server.
  evidence: Verified in SecurityConfig — no cors() is configured on HttpSecurity and requestMatchers only permit HttpMethod.POST on auth endpoints; browser preflight OPTIONS requests fail with 401 UNAUTHENTICATED. Defer to story 1.5 when frontend auth connects to backend.
