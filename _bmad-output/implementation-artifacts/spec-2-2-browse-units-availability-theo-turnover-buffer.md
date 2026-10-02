---
title: 'Story 2.2: Browse Units + availability theo Turnover Buffer'
type: 'feature'
created: '2026-10-02'
status: 'done'
baseline_commit: '9be5aab4cc821d896191797b3c511a35a1e76f67'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/implementation-artifacts/epic-2-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Customers landing on Browse Units need to filter units by type, size, start date, and rental duration, and view real-time availability derived accurately from active turnover buffer cleaning policies, so that the dates displayed are guaranteed bookable.

**Approach:** Implement `GET /api/v1/units/browse` calculating availability on-read (AD-4) by checking unit status, active reservations, and turnover buffer policy rules (AD-6 delta), excluding `RENTED`, `MAINTENANCE`, and `RETIRED` units while displaying `PREPARING`/buffer units as bookable from their buffer-cleared date in amber. On the frontend, build the `BrowseUnitsPage` (`/units`) featuring a compact filter toolbar (Search button triggered, session-persisted), live availability counter badge ("N units available · live"), 3px left status bar cards (available green / buffer amber), tabular pricing, empty states with removable filter chips, and card navigation to `UnitDetailPage`.

## Boundaries & Constraints

**Always:**
- Availability is derived on-read at query time from existing reservations and active turnover buffer policy (AD-4, AD-11); never persist derived availability status to the database.
- Units with status `RENTED`, `MAINTENANCE`, or `RETIRED` must never appear in customer browse results.
- Units currently in turnover cleaning buffer render with an amber status dot, amber 3px left status bar, and label "Available {date} · cleaning buffer" with secondary Book CTA.
- Units immediately available render with green status dot, green 3px left status bar, and label "Available now" with primary Book CTA.
- Re-query occurs ONLY on clicking "Search" button or initial page load (no refetch per keystroke).
- Filter state persists across navigations within the session.
- Card click (except Book button) routes to `/units/{code}`.
- All pricing in cards uses `formatMoney` (`1.150.000 ₫`) with tabular numerals.
- Empty state displays factual count ("0 of N units meet all criteria"), removable mono chips echoing active filters, and exactly one CTA ("Clear filters").

**Never:**
- Never hardcode availability dates or calculate prices on the client.
- Never display `MAINTENANCE` or `RENTED` units in the customer browse catalog.
- Never trigger search requests on every input change or keystroke.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Default Browse Landing | Query `/api/v1/units/browse` without filters | Returns list of available & buffer units sorted price low→high; S-3 (buffer) and M-5 (available) included; M-2 (maintenance) excluded | N/A |
| Filter by Type S | Query `type=S` | Returns only Type S units (e.g. S-3) | N/A |
| Filter Matching No Units | Query `type=Locker` (when no lockers exist/available) | Returns empty `items: []`, `totalAvailable: 0`; FE renders factual empty state with filter chips and "Clear filters" CTA | N/A |
| Turnover Buffer Unit S-3 | S-3 in post-checkout cleaning with 2-day buffer | Card displays amber status bar, "Available {date} · cleaning buffer", secondary Book button | N/A |
| Search Trigger | User modifies filter dropdowns | No API call until user clicks "Search" button | Input validated before submit |
| Card Click | Click anywhere on unit card body | Navigates to `/units/{code}` | N/A |

</frozen-after-approval>

## Open Questions

## Code Map

- `contracts/openapi.yaml` -- Declare `GET /api/v1/units/browse` with `BrowseUnitDto` and `BrowseUnitsResponse` schema.
- `backend/src/main/java/com/storagehub/entity/Reservation.java` -- JPA Entity for `reservations` table (BK- code, Customer, Unit, dates, deposit, status).
- `backend/src/main/java/com/storagehub/repository/ReservationRepository.java` -- Repository querying active reservations for units on dates.
- `backend/src/main/java/com/storagehub/dto/BrowseUnitDto.java` & `BrowseUnitsResponse.java` -- DTOs for browse response payload.
- `backend/src/main/java/com/storagehub/service/UnitService.java` -- Implement `browseUnits` calculating on-read availability and turnover buffer dates.
- `backend/src/main/java/com/storagehub/controller/UnitController.java` -- Add `GET /api/v1/units/browse` endpoint with filtering and sort.
- `backend/src/test/java/com/storagehub/unit/UnitBrowseServiceTests.java` -- Tests verifying availability on-read, buffer date calculation, and exclusion of maintenance/rented units.
- `frontend/src/types/unit.ts` -- TypeScript interfaces for `BrowseUnit`, `BrowseFilter`, and browse response.
- `frontend/src/api/unit.ts` -- Axios client method `browseUnits(filters)`.
- `frontend/src/mocks/handlers.ts` -- MSW mock handler for `GET /api/v1/units/browse`.
- `frontend/src/pages/unit/BrowseUnitsPage.tsx` -- Main Browse Units catalog screen with filter bar, unit cards, status chips, and empty state.
- `frontend/src/components/unit/UnitCard.tsx` -- Reusable unit card primitive with 3px status bar, tabular pricing, and code chip.
- `frontend/src/components/unit/UnitFilterBar.tsx` -- Filter toolbar with explicit Search button and session state persistence.
- `frontend/src/router/routes.tsx` -- Connect `BrowseUnitsPage` to `/units`.
- `frontend/src/test/browse-units.test.tsx` -- RTL tests for Browse Units rendering, search button action, filter persistence, and card navigation.

## Tasks & Acceptance

**Execution:**
- [x] `contracts/openapi.yaml` -- Add `GET /api/v1/units/browse` endpoint specification and DTO schemas -- Maintain contract-first API.
- [x] `backend/src/main/java/com/storagehub/entity/Reservation.java` -- Create `Reservation` JPA entity and `ReservationStatus` enum -- Map reservations schema.
- [x] `backend/src/main/java/com/storagehub/repository/ReservationRepository.java` -- Create repository to query reservations by unit and status -- Support availability checking.
- [x] `backend/src/main/java/com/storagehub/service/UnitService.java` -- Implement browse logic with on-read availability and turnover buffer evaluation -- Enforce on-read rule (AD-4).
- [x] `backend/src/main/java/com/storagehub/controller/UnitController.java` -- Expose `GET /api/v1/units/browse` with query parameter binding -- Deliver browse API.
- [x] `backend/src/test/java/com/storagehub/unit/UnitBrowseTests.java` -- Backend tests verifying seed availability, buffer dates, and exclusions -- Validate business logic.
- [x] `frontend/src/types/unit.ts` & `frontend/src/api/unit.ts` -- Add browse types and API client functions -- Establish frontend client layer.
- [x] `frontend/src/mocks/handlers.ts` -- Add MSW handler for `/api/v1/units/browse` -- Enable mock and test workflows.
- [x] `frontend/src/components/unit/UnitCard.tsx` & `UnitFilterBar.tsx` -- Implement unit card with 3px status bar and filter toolbar -- Deliver Control Room components.
- [x] `frontend/src/pages/unit/BrowseUnitsPage.tsx` -- Implement full Browse Units page replacing placeholder -- Deliver flagship Customer discovery screen (F1-03).
- [x] `frontend/src/test/browse-units.test.tsx` -- RTL test suite covering filters, search submit, unit cards, empty state, and navigation -- Ensure UI quality.

**Acceptance Criteria:**
- Given customer on `/units`, when page loads, then live count chip ("N units available · live"), unit cards with 3px status bar (green available / amber buffer), tabular pricing, and filter bar are rendered.
- Given seed data, when browsing units, then M-5 appears as Available (green bar), S-3 appears as Buffer (amber bar "Available Oct 5 · cleaning buffer"), and M-2 (maintenance) does not appear.
- Given filter bar modifications, when changing inputs, then no API call is made until the user clicks the "Search" button.
- Given no matching units for a filter, when search is submitted, then a factual empty state is displayed with removable filter chips and a "Clear filters" CTA.

## Implementation Notes
- Added `Reservation` entity, `ReservationStatus` enum, and `ReservationRepository` to query reservation timelines.
- Implemented `UnitService.browseUnits` deriving on-read availability and turnover buffer dates (AD-4, AD-11), omitting `RENTED`, `MAINTENANCE`, and `RETIRED` units while calculating pricing via `PricingEngine`.
- Exposed `GET /api/v1/units/browse` with type, size, startDate, and durationMonths filtering.
- Implemented `BrowseUnitsPage` (`/units`) with session filter persistence, explicit "Search" trigger button, live count badge, 3px status bar `UnitCard`s, and empty state with removable filter chips.
- Verified 128 backend tests and 116 frontend tests passing cleanly.

## Spec Change Log

## Review Triage Log
| Finding | Verdict | Route | Evidence / Action |
|---------|---------|-------|-------------------|
| Missing `startDate` filtering in `UnitService.java` | medium | patch | Added check to filter out units with `availableFromDate > startDate`. |
| Potential past availability date for `PREPARING` units | medium | patch | Added fallback to `today.plusDays(turnoverBufferDays)` if computed buffer date is before today. |
| Input state desynchronization in `UnitFilterBar.tsx` when filter chips cleared | medium | patch | Added `useEffect` to synchronize input state with `initialFilters` prop. |
| Corrupted unicode characters (`·`, `m²`) in strings and assertions | medium | patch | Cleaned up all unicode literals across BE, FE, and test files. |
| Missing removable filter chip for `durationMonths` in `BrowseUnitsPage.tsx` | low | patch | Added duration filter chip when `durationMonths > 1`. |
| Missing total catalog units count (`totalUnits`) in `BrowseUnitsResponse` | low | patch | Added `totalUnits` to DTO and openapi schema, and updated empty state display. |

## Design Notes

Browse Units layout follows F1-03 specification:
- Top: Page header with title "Browse Storage Units", subtitle, and live badge chip ("N units available · live").
- Filter toolbar: Horizontal compact card with Type dropdown, Size dropdown, Start Date picker, Duration selector, and prominent primary "Search" button.
- Grid: Responsive 3-column grid of `UnitCard` components.
- Unit card anatomy:
  - Top: Image thumbnail with mono code chip in upper-left and status badge in upper-right.
  - Left edge: 3px accent bar (green `#10B981` for Available, amber `#F59E0B` for Buffer).
  - Body: Type and size badges, feature line (Floor · Access · Security), and availability status line with matching dot.
  - Footer: Tabular monthly price (`345.000 ₫ / mo`) and Book CTA button (primary for available, secondary for buffer).

## Verification

**Commands:**
- `mvn test -Dtest=UnitBrowseTests,PricingEngineTests` -- expected: All unit browsing and availability tests pass.
- `npm run test` (in `frontend/`) -- expected: All frontend tests pass including `browse-units.test.tsx`.
- `npm run build` (in `frontend/`) -- expected: Frontend build succeeds without TypeScript or bundling errors.
