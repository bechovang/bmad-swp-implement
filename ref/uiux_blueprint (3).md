You are modifying an existing Stitch project containing 21 screens for:

“StorageHub — Self-Service Storage Rental and Management System.”

Refactor the existing project into one consistent, reusable, desktop-first UI system.

Do not create another separate set of 21 screens. Audit the current screens, build shared components, and rebuild the existing screens using instances of those components.

# 1. Language requirement

The entire project must use English only.

This includes:

* Page titles.
* Navigation.
* Buttons.
* Form labels.
* Helper text.
* Validation messages.
* Status labels.
* Empty states.
* Alerts.
* Confirmation dialogs.
* Table headers.
* Tooltips.
* Notifications.
* Demo data descriptions.

Do not leave any Vietnamese text in the UI.

Internal entity names, component names, properties, and enum values must also use English.

Vietnamese personal names and Vietnamese place names may remain as proper nouns without accents when necessary, for example:

* Nguyen Minh An.
* FPT Thu Duc.
* FPT Binh Thanh.

# 2. Target platform

Design all 21 screens for desktop web browsers.

* Standard frame: 1440 × 1024.
* Main content width: approximately 1200–1280px.
* Customer Portal uses a horizontal desktop header.
* Back-office Portal uses a desktop sidebar.
* Show filters directly on the page.
* Unit grids should normally contain 3–4 columns.
* Tables must preserve important columns.
* Long forms may use two columns when fields are logically related.
* Use desktop-sized modals and drawers.
* Do not create mobile or tablet frames.
* Do not use bottom navigation, mobile filter drawers, mobile floating actions, or mobile-only sticky controls.
* The interface should remain usable in a smaller browser window, but no separate responsive mobile design is required.

# 3. Core architecture

Organize the project in this order:

Data Model
→ Basic Design Tokens
→ Primitive Components
→ Shared Components
→ Domain Components
→ Page Templates
→ 21 Business Screens.

The screens must be compositions of one shared component system.

When a master component such as `UnitCard`, `StatusBadge`, `DataTable`, or `MoneySummary` changes, all its instances must update.

If Stitch generates code, use reusable React components, props, typed data models, and one shared mock-data store.

If Stitch generates design frames, use master components, variants, properties, and component instances. Do not manually duplicate identical cards, rows, badges, forms, or navigation elements.

# 4. Visual scope

Keep the current neutral visual direction.

Do not apply a new brand style yet.

Use:

* Neutral white and gray surfaces.
* A readable system font.
* Consistent spacing.
* Simple borders.
* One neutral accent color for primary actions.
* Status colors supported by text labels and icons.

Do not add gradients, glass effects, decorative illustrations, marketing graphics, or visual elements that do not support the business workflow.

# 5. Data control rules

Do not invent business data, KPIs, prices, percentages, addresses, dimensions, facilities, amenities, policies, or statistics.

Every visible value must be one of:

1. `Stored field`: directly stored in the data model.
2. `Derived field`: calculated or inferred from stored data.
3. `Demo-only value`: explicitly defined in the shared mock-data source.

Document every derived field used by the UI.

Do not invent values such as:

* Security score.
* Customer rating.
* Number of previous renters.
* Revenue growth percentage.
* AI monitoring.
* Insurance coverage.
* Temperature or humidity.
* Discount percentage.
* Storage popularity.
* Available unit count without a valid calculation.
* Additional service fees.
* Features not included in the data model.

Do not display raw technical IDs. Display business codes and readable names:

* Reservation: `BK-`.
* Rental: `RT-`.
* Support request: `SR-`.
* Contract: `CT-`.

Components must not calculate prices or invent business rules. They only display values supplied through props or shared state.

# 6. Canonical entities

Use these entities as the UI data source:

* Role.
* User.
* Facility.
* Zone.
* StaffAssignment.
* UnitType.
* Unit.
* RentalPolicy.
* PolicyRule.
* Reservation.
* Rental.
* Extension.
* CheckoutRequest.
* Contract.
* Payment.
* Settlement.
* SettlementCharge.
* Inspection.
* SupportTicket.
* Escalation.
* Task.
* Notification.
* ActivityLog.

Use exactly five roles:

* CUSTOMER.
* STAFF.
* FACILITY_MANAGER.
* BUSINESS_OPERATIONS_MANAGER.
* SYSTEM_ADMINISTRATOR.

Important model rules:

* A Facility contains Zones.
* A Zone contains Units.
* Staff members are assigned by work date, shift, and zone.
* Unit type defines the storage category and turnover buffer.
* Availability is determined from reservations, rentals, and turnover buffer.
* Reservation start date remains the customer’s real rental start date.
* “Available soon” is a derived UI label, not a stored Unit status.
* Pricing values come from a versioned RentalPolicy.
* A Reservation creates no more than one Rental.
* An Extension belongs to one Rental.
* A successful extension may create a Contract addendum.
* A Settlement contains multiple SettlementCharge records.
* A SupportTicket may have one Escalation.
* ActivityLog is append-only.
* Important state changes require an audit reason.
* Login actions do not require a business reason.

# 7. Canonical statuses

## UnitStatus

* AVAILABLE → Available.
* RESERVED → Reserved.
* RENTED → Rented.
* PREPARING → Preparing.
* MAINTENANCE → Under Maintenance.
* RETIRED → Retired.

## ReservationStatus

* PENDING_PAYMENT → Pending Payment.
* RESERVED → Reserved.
* CHECKED_IN → Checked In.
* EXPIRED → Expired.
* CANCELLED → Cancelled.

## RentalStatus

* ACTIVE → Active.
* CHECKOUT_REQUESTED → Checkout Requested.
* CLOSED → Closed.

## ExtensionStatus

* PENDING_PAYMENT → Pending Payment.
* APPLIED → Applied.
* CANCELLED → Cancelled.

## PaymentStatus

* PENDING → Pending.
* PROCESSING → Processing.
* SUCCEEDED → Successful.
* FAILED → Failed.
* EXPIRED → Expired.

## TicketStatus

* OPEN → Open.
* IN_PROGRESS → In Progress.
* ESCALATED → Escalated.
* RESOLVED → Resolved.

## TaskStatus

* TODO → To Do.
* IN_PROGRESS → In Progress.
* DONE → Done.

Create one shared `statusDictionary`. Do not write status labels manually inside individual pages.

If the same internal value such as `ACTIVE` exists in different entities, the dictionary must consider the entity type.

# 8. Design system page

Create one project section named:

`00 — Design System & Components`

For each component, define:

* Component name.
* Purpose.
* Props or properties.
* Variants.
* States.
* Entity and field mapping.
* Screens using the component.
* One master component.
* Example instances.

# 9. Primitive components

Create reusable primitives:

* Button.
* IconButton.
* TextInput.
* PasswordInput.
* NumberInput.
* TextArea.
* Select.
* MultiSelect.
* DatePicker.
* DateTimePicker.
* DateRangePicker.
* Checkbox.
* RadioGroup.
* SearchInput.
* FormField.
* FieldError.
* StatusBadge.
* Tag.
* Tooltip.
* Divider.
* Tabs.
* Pagination.
* Breadcrumb.
* Toast.
* InlineAlert.
* Skeleton.
* Spinner.
* EmptyState.
* ErrorState.
* ConfirmDialog.
* FormModal.
* DetailDrawer.
* DropdownMenu.

All form controls must use the same `FormField` structure:

* Label.
* Required indicator.
* Input control.
* Helper text.
* Validation message.

# 10. Layout components

Create:

## CustomerAppShell

* CustomerHeader.
* MainNavigation.
* NotificationBell.
* AccountMenu.
* MainContent.
* Breadcrumb area.
* Minimal CustomerFooter.

## BackOfficeAppShell

* RoleBasedSidebar.
* BackOfficeHeader.
* FacilityScopeIndicator.
* NotificationBell.
* AccountMenu.
* Breadcrumb.
* PageContent.

## Shared page structures

* PageHeader.
* FilterToolbar.
* DataTable.
* CardGrid.
* EntityListPageTemplate.
* DashboardPageTemplate.
* TransactionPageTemplate.
* Stepper.
* MetricCard.
* ChartPanel.
* PrimaryActionBar.

Each page must configure shared components through props. Do not rebuild tables, filters, headers, or shells manually.

# 11. Domain component contracts

## FacilityCard

Visible attributes:

* Facility name.
* Address.
* Phone.
* Opening and closing time.
* Status.

Do not show unit counts or occupancy unless provided as a derived prop.

## UnitCard

Visible attributes:

* Unit code.
* Unit type.
* Size in square meters.
* Facility name.
* Zone code.
* Floor.
* Access type.
* Unit status.
* Rental rate from the active policy when required.

Optional derived attributes:

* Availability label.
* Available-from date.
* Next reservation date.

Page-specific actions are passed through props. The component must not decide which actions the user can perform.

## ReservationCard

Visible attributes:

* Reservation code.
* Unit code.
* Facility name.
* Start date.
* End date.
* Deposit amount.
* Reservation status.

Do not show an access code because access codes belong to Rentals.

## RentalCard

Visible attributes:

* Rental code.
* Unit code.
* Facility name.
* End date.
* Deposit held.
* Rental status.
* Access code through a `SensitiveValue` component.

Optional sections:

* Contract summary.
* Pending checkout request.
* Latest extension.

## ContractCard

Visible attributes:

* Contract code.
* Contract type.
* Contract status.
* Signature due date.
* Parent contract code for an addendum.

Actions:

* View.
* Print.
* Upload signed copy.
* Confirm signature.

Do not create a new main Contract screen. Use Contract components inside existing screens.

## MoneySummary

Supported line items:

* Rental amount.
* Deposit.
* Extension fee.
* Settlement charge.
* Refund amount.
* Additional amount due.
* Total payable.

Only show lines received from data. Do not create new charges.

## PaymentSummary

Visible attributes:

* Payment purpose.
* Amount.
* Method.
* Status.
* Receipt code after successful payment.

## TicketCard

Visible attributes:

* Ticket code.
* Unit code.
* Incident type.
* Priority.
* Status.
* Assigned staff.
* Created time.
* Updated time.

Full descriptions belong in `TicketDetailDrawer`.

## TaskCard

Visible attributes:

* Task type.
* Reference code.
* Unit code.
* Zone.
* Work date.
* Shift.
* Status.

Instructions and results belong in `TaskDetailDrawer`.

## UserRow

Visible attributes:

* Full name.
* Email.
* Role.
* Facility or zone scope.
* Account status.

## ActivityLogRow

Visible attributes:

* Created time.
* Actor.
* Action.
* Entity type.
* Entity business code.
* Result.

Previous value, new value, and reason belong in `ActivityLogDrawer`.

# 12. Shared component usage

Use the same components across these screens:

| Component          | Screens                                             |
| ------------------ | --------------------------------------------------- |
| CustomerAppShell   | C-01 to C-07                                        |
| BackOfficeAppShell | S-01 to A-02                                        |
| UnitCard           | C-01, C-02, C-04, M-01, M-03                        |
| ReservationCard    | C-03, C-04, S-01                                    |
| RentalCard         | C-04, C-05, C-06, S-02                              |
| ContractCard       | C-04, S-01, S-03                                    |
| MoneySummary       | C-02, C-03, C-05, S-01, S-02                        |
| PaymentSummary     | C-03, C-05, S-01, S-02, B-03                        |
| TicketCard         | C-07, S-03, M-03                                    |
| TaskCard           | S-03, M-02, M-04                                    |
| UserRow            | M-02, A-01, A-02                                    |
| StatusBadge        | Every screen with a status                          |
| FilterToolbar      | Every searchable or filterable list                 |
| DataTable          | Internal list screens                               |
| ConfirmDialog      | Financial and destructive actions                   |
| DetailDrawer       | Unit, ticket, task, user, facility, and log details |
| EmptyState         | Every list                                          |
| NotificationBell   | Both application shells                             |

# 13. Shared mock data

Create one shared mock-data source. Do not define separate data inside each page.

Demo users:

* Nguyen Minh An — Customer.
* Tran Thi Lan — Staff.
* Pham Quoc Huy — Facility Manager.
* Le Thu Ha — Business Operations Manager.
* Vo Minh Khoa — System Administrator.

Demo facilities:

* FPT Thu Duc.
* FPT Binh Thanh.

Demo units:

* `TD-A-101` — AVAILABLE.
* `TD-A-102` — RESERVED.
* `TD-B-201` — RENTED.
* `BT-A-101` — PREPARING.
* `BT-A-102` — MAINTENANCE.
* `BT-B-201` — RETIRED.

Demo records:

* `BK-2026-0001` — paid deposit and waiting for check-in.
* `RT-2026-0001` — active rental.
* `CT-2026-0001` — active original contract.
* `SR-2026-0001` — support request in progress.
* One active pricing-policy version.
* One morning staff assignment at Zone TD-A.
* One check-in task.
* One cleaning task.

All demo amounts must be defined once in the shared mock data.

# 14. Required 21 screens

Keep exactly these 21 main screens.

## Customer Portal

* C-01 — Storage Search.
* C-02 — Unit Details and Reservation Configuration.
* C-03 — Deposit Payment and Reservation Confirmation.
* C-04 — My Storage.
* C-05 — Rental Extension.
* C-06 — Checkout Request.
* C-07 — Support Requests.

## Facility Staff

* S-01 — Check-in and Unit Handover.
* S-02 — Checkout and Unit Return.
* S-03 — My Tasks.

## Facility Manager

* M-01 — Unit Floor Plan and Unit Management.
* M-02 — Staff Shift Management.
* M-03 — Escalated Issues.
* M-04 — Facility Operations Report.

## Business Operations Manager

* B-01 — Facility Management.
* B-02 — Pricing and Policy Management.
* B-03 — System-wide Revenue Report.

## System Administrator

* A-01 — User and Role Management.
* A-02 — Activity Logs.

## Shared

* AUTH-01 — Login, Customer Registration, and Forgot Password.
* SYS-01 — 403, 404, and 500 Error States.

C-05 and C-06 must remain separate frames while using shared components.

AUTH-01 and SYS-01 use variants. Their variants do not count as additional main screens.

# 15. Screen composition

## C-01

Use:

* CustomerAppShell.
* PageHeader.
* FilterToolbar.
* FacilitySelect.
* UnitTypeSelect.
* SizeRangeInput.
* DateRangePicker.
* UnitCardGrid.
* EmptyState.
* Pagination.

Unit type options come from UnitType data.

## C-02

Use:

* Breadcrumb.
* UnitDetailPanel.
* BookingDateForm.
* AvailabilityPanel.
* MoneySummary.
* PolicyReference.
* PrimaryActionBar.

Deposit rate comes from the active policy. Use 10% in demo data.

## C-03

Use:

* TransactionPageTemplate.
* Stepper.
* ReservationCard.
* PaymentMethodSelector.
* PaymentSummary.
* PaymentStatusPanel.
* ReservationConfirmationPanel.

## C-04

Use:

* Tabs.
* ReservationCard.
* RentalCard.
* ContractCard.
* SensitiveValue.
* EmptyState.

Tabs:

* Reservations.
* Active Rentals.
* History.

## C-05

Use:

* RentalCard.
* ExtensionForm.
* AvailabilityPanel.
* MoneySummary.
* PaymentMethodSelector.
* PaymentStatusPanel.
* ContractCard for the addendum.

The UI must not invent the extension formula.

## C-06

Use:

* RentalCard.
* CheckoutRequestForm.
* DateTimePicker.
* ConfirmationChecklist.
* RequestResultPanel.

## C-07

Use:

* FilterToolbar.
* TicketCardList.
* TicketFormModal.
* TicketDetailDrawer.
* TicketTimeline.

## S-01

Use:

* ReservationLookup.
* ReservationCard.
* CustomerSummary.
* Stepper.
* PaymentSummary.
* ContractSigningPanel.
* HandoverChecklist.
* AccessCredentialPanel.

Show the deposit and the full rental payment as separate values.

## S-02

Use:

* CheckoutRequestTable.
* RentalCard.
* InspectionChecklist.
* SettlementChargeTable.
* MoneySummary.
* SettlementConfirmation.
* PaymentStatusPanel.

Support:

* Full deposit refund.
* Partial refund after charges.
* Additional payment when charges exceed the deposit.

## S-03

Use:

* ShiftSummary.
* FilterToolbar.
* TaskBoard or shared DataTable.
* TaskCard.
* TaskDetailDrawer.
* TicketDetailDrawer.
* ContractCard.

## M-01

Use:

* FilterToolbar.
* ViewSwitcher.
* UnitGrid.
* UnitCard.
* DataTable.
* UnitDetailDrawer.
* UnitFormModal.
* MergeUnitDialog.
* ChangeStatusDialog.
* ConfirmDialog.

Grid and table must use the same Unit collection.

## M-02

Use:

* ScheduleView.
* FilterToolbar.
* AssignmentCard.
* AssignmentFormModal.
* ConflictAlert.
* ConfirmDialog.

## M-03

Use:

* TicketTable.
* TicketDetailDrawer.
* EscalationDecisionPanel.
* Compact UnitCards for the current and replacement units.
* AssignmentSelector.
* ConfirmDialog.

## M-04

Use:

* DashboardPageTemplate.
* DashboardFilterBar.
* MetricCard.
* ChartPanel.
* UnitTable.
* TaskTable.

Metrics must be calculated from shared demo data. Do not invent trend percentages.

## B-01

Use:

* EntityListPageTemplate.
* FacilityTable.
* FacilityDetailDrawer.
* FacilityFormModal.

## B-02

Use:

* PolicyVersionHeader.
* Tabs.
* PolicyRuleTable.
* PolicyRuleFormModal.
* ChangeSummary.
* ConfirmDialog.

Do not directly edit an active policy. Create a new draft version before activation.

## B-03

Use:

* DashboardPageTemplate.
* DashboardFilterBar.
* MetricCard.
* ChartPanel.
* FacilityComparisonTable.
* ExportMenu.

Separate:

* Rental revenue.
* Surcharge revenue.
* Deposit collected.
* Deposit currently held.
* Deposit refunded.

Held deposits are not automatically counted as revenue.

## A-01

Use:

* EntityListPageTemplate.
* UserTable.
* UserDetailDrawer.
* UserFormModal.
* RoleScopeEditor.
* ConfirmDialog.

Public registration may only create Customer accounts.

## A-02

Use:

* FilterToolbar.
* ActivityLogTable.
* ActivityLogDrawer.
* Pagination.

This screen is read-only.

## AUTH-01

Use:

* AuthShell.
* AuthCard.
* LoginForm.
* CustomerRegistrationForm.
* ForgotPasswordForm.
* InlineAlert.

## SYS-01

Use:

* SystemMessageLayout.
* ErrorCode.
* ErrorDescription.
* ContextualActions.

Variants:

* 403 Forbidden.
* 404 Page Not Found.
* 500 Server Error.

# 16. Role-based navigation

Customer:

* Find Storage.
* My Storage.
* Support.
* Notifications.
* Account.

Staff:

* Check-in.
* Checkout.
* My Tasks.

Facility Manager:

* Unit Management.
* Shift Management.
* Escalated Issues.
* Facility Reports.

Business Operations Manager:

* Facilities.
* Pricing and Policies.
* System Reports.

System Administrator:

* Users and Roles.
* Activity Logs.

Show only the navigation allowed for the current role.

Display the current facility and zone scope in the back-office header.

Unauthorized access opens SYS-01 with the 403 variant.

# 17. Shared prototype state

Use one shared state source so that actions on one screen update related screens.

Required demo behavior:

* C-01 selects a Unit and opens C-02.
* C-02 creates a pending Reservation and opens C-03.
* Successful deposit payment creates a confirmed Reservation and draft Contract.
* S-01 check-in creates a Rental and changes the Unit to RENTED.
* C-04 displays the newly created Rental.
* C-05 creates an Extension and Contract addendum.
* C-06 creates a CheckoutRequest and changes the Rental to CHECKOUT_REQUESTED.
* S-02 creates a Settlement and Inspections, closes the Rental, and changes the Unit to PREPARING.
* S-03 completes cleaning and checks the turnover buffer before availability changes.
* C-07 creates a SupportTicket.
* S-03 accepts, resolves, or escalates the ticket.
* M-03 processes the Escalation.
* M-01 Unit changes create ActivityLog records.
* M-02 creates StaffAssignment records.
* Important actions create Notifications.

No real backend is required. Unconfirmed formulas must use fixed mock responses.

# 18. Mutation pattern

Every data-changing action must include:

1. Clear trigger.
2. Form or confirmation.
3. Validation.
4. Loading state.
5. Success or error result.
6. Shared-state update.
7. Activity log when applicable.
8. Notification when another user is affected.

Example: changing a Unit to MAINTENANCE must:

* Open ChangeStatusDialog.
* Require a reason.
* Check active reservations and rentals.
* Show the blocking record if the action is invalid.
* Update the shared Unit record when valid.
* Create an ActivityLog.
* Update every UnitCard and UnitRow displaying that Unit.

# 19. Consistency rules

* Use the same entity field name everywhere.
* Use the same English label for each status.
* Format all monetary values consistently as VND.
* Format dates consistently.
* Use the same business-code prefix everywhere.
* Use “Storage Unit” in page titles and formal descriptions.
* Use “Unit” in compact labels and component names.
* Do not use multiple terms for the same entity.
* Do not duplicate component markup inside pages.
* Do not hard-code status labels.
* Do not hard-code policy values inside components.
* Do not create page-specific mock data.
* Do not directly edit data by changing text inside a card.
* Use tables for dense administrative data.
* Use cards only when users need to compare a small number of key attributes.

# 20. Required output

Provide:

1. `00 — Design System & Components`.
2. Component inventory.
3. Component variants and states.
4. Component usage matrix.
5. UI data dictionary.
6. Status dictionary.
7. Shared mock-data source.
8. Exactly 21 rebuilt desktop screens.
9. Working prototype navigation.
10. Shared demo state between roles.
11. Derived-field inventory.
12. Demo-value inventory.
13. A list of visible fields that still lack a valid data source.

# 21. Execution order

Perform the work in this order:

## Step 1 — Audit

Identify:

* Duplicated elements.
* Fields missing from the data model.
* Invalid statuses.
* Uncontrolled values.
* Inconsistent terminology.
* Similar components with different structures.

## Step 2 — Data contract

Create the data dictionary and status dictionary.

## Step 3 — Component library

Create primitives, layouts, shared components, and domain components.

## Step 4 — Rebuild

Replace duplicated UI elements across all 21 screens with component instances.

## Step 5 — Connect

Connect navigation and shared prototype state.

## Step 6 — Verify

Verify that:

* Exactly 21 main screens exist.
* There are no duplicate screen sets.
* Repeated cards and rows are component instances.
* Master component changes update every instance.
* No visible field lacks a known source.
* No custom statuses were invented.
* One entity update appears on every related screen.
* Role-based navigation is correct.
* All visible UI text is English.
* No mobile or tablet frames exist.
* Important buttons perform a prototype action.
* No Vietnamese interface text remains.

First provide a short audit of the current project and the component structure you will create. Then perform the refactoring directly. Do not stop after presenting a plan.
