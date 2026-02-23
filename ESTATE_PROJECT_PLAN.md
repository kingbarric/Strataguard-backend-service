# Estate Management & Security SaaS Platform — Phased Build Plan

## Context
Building a production-grade, multi-tenant Estate Management and Security SaaS platform from scratch (greenfield). Targeted at the Nigeria/Africa market with features like WhatsApp notifications, USSD access, mobile money payments, utility tracking, and low-bandwidth mobile optimization.

## Tech Stack
- **Backend:** Spring Boot (Java 21), Maven, PostgreSQL, Keycloak (auth)
- **Web Dashboard:** React (UI library TBD)
- **Mobile Apps:** React Native bare workflow (Android + iOS)
- **Cloud:** Google Cloud Platform (GCP)
- **Multi-tenancy:** Shared schema with `tenant_id` discrimination
- **Edge Sync:** REST + polling with local event queue
- **Repos:** 3 separate repos (backend, web, mobile)

---

## Phase 1: Foundation + Auth + Estate/Unit CRUD (~5 days)

### 1A. Backend Project Setup (Day 1)
- Initialize Spring Boot 3.x project with Maven multi-module structure:
  - `estate-core` — shared entities, DTOs, exceptions, tenant filter
  - `estate-api` — REST controllers, Spring Security config
  - `estate-service` — business logic services
  - `estate-infrastructure` — persistence (JPA repos, Flyway migrations)
  - `estate-app` — main application module, Docker config
- Configure:
  - PostgreSQL with Flyway for migrations
  - Tenant discriminator filter (Hibernate `@Filter` or `@TenantId`)
  - Global exception handleryes
  - Request/response logging
  - Base audit entity (`createdBy`, `createdAt`, `updatedBy`, `updatedAt`)
  - Docker Compose (PostgreSQL, Keycloak, app)
  - Test infrastructure (Testcontainers for PostgreSQL + Keycloak)
  - API versioning strategy (`/api/v1/...`)
  - OpenAPI/Swagger documentation setup

### 1B. Keycloak Integration & Auth Endpoints (Day 2)
- Keycloak realm configuration for multi-tenant support
- Spring Security OAuth2 Resource Server config (JWT validation)
- Role mapping: `SUPER_ADMIN`, `ESTATE_ADMIN`, `RESIDENT`, `SECURITY_GUARD`, `FACILITY_MANAGER`
- Tenant extraction from JWT claims (custom claim or realm)
- Keycloak admin client for programmatic user provisioning
- **Auth REST Endpoints:**
  - `POST /api/v1/auth/register` — Register new admin user (creates Keycloak user with role + generates tenant_id)
  - `POST /api/v1/auth/login` — Login (proxies to Keycloak token endpoint, returns JWT access + refresh tokens)
  - `POST /api/v1/auth/refresh` — Refresh expired access token using refresh token
- **Auth Flow:** Register admin → Login → Receive JWT with `tenant_id` claim → Use token to create estates/units
- **Keycloak Realm Setup:** Protocol mapper for `tenant_id` user attribute → JWT claim
- Token refresh flow
- Unit tests for security config and role-based access

### 1C. Estate & Unit Management CRUD (Days 3-4)
- **Estate entity:** name, address, type (gated community, estate, apartment complex), tenant_id, settings (JSON), logo, contact info
- **Unit entity:** unit number, block/zone, type (flat, duplex, terrace, detached), floor, status (occupied, vacant, under maintenance), estate_id
- Full CRUD REST endpoints for both entities
- Pagination, sorting, filtering
- Validation (Bean Validation)
- Unit + integration tests (Testcontainers)
- Flyway migration scripts

### 1D. Phase 1 Testing & Polish (Day 5)
- End-to-end flow: create estate → create units → assign roles → verify tenant isolation
- API documentation review
- Postman/Insomnia collection for all endpoints
- README with setup instructions

---

## Phase 2: Residents, Tenancy & Vehicles (~5 days)

### 2A. Resident & Tenancy Module (Days 6-8)
- **Resident entity:** user_id (Keycloak ref), first_name, last_name, phone, email, emergency_contact, profile_photo, status
- **Tenancy entity:** resident_id, unit_id, type (owner, tenant, dependent), start_date, end_date, status (active, expired, terminated), lease_reference
- Resident onboarding flow (link Keycloak user to resident profile)
- Tenancy assignment and transfer
- Resident directory (searchable by estate admins)
- Move-in / move-out workflow
- Unit + integration tests

### 2B. Vehicle Registry (Days 9-10)
- **Vehicle entity:** resident_id, plate_number, make, model, color, type (car, motorcycle, truck), sticker_number, status (active, removed), photo
- CRUD endpoints
- Plate number normalization (handle Nigerian plate formats)
- Vehicle-to-resident association
- Bulk vehicle import (CSV)
- Unit + integration tests

---

## Phase 3: Vehicle Gate Control System (~5 days) ✅ COMPLETED

### 3A. Vehicle QR Sticker & Gate Sessions (Days 11-13)
- **QR Sticker Code:** Auto-generated `"VEH-" + UUID` on vehicle registration, immutable, unique per tenant
- **GateSession entity:** vehicleId, residentId, plateNumber, status (OPEN/CLOSED), entryTime, exitTime, entryGuardId, exitGuardId, notes
- **GateAccessLog entity:** sessionId, vehicleId, residentId, eventType, guardId, details, success — full audit trail
- Entry flow: Guard scans vehicle QR → validate ACTIVE status, no duplicate open session → create OPEN session → return vehicle + resident details
- Partial unique index ensures only ONE open session per vehicle
- Existing vehicles backfilled with QR codes via V3 migration
- Gate access logs record every event (ENTRY_SCAN, EXIT_SCAN, EXIT_PASS_VALIDATED, EXIT_PASS_FAILED, etc.)

### 3B. Two-Factor Exit & Remote Approval (Days 14-16)
- **Exit Pass (HMAC-SHA256):** Payload `vehicleId|residentId|tenantId|nonce|expiresAtMillis`, short expiry (120s), signed token
- **ExitApprovalRequest entity:** sessionId, vehicleId, residentId, guardId, status (PENDING/APPROVED/DENIED/EXPIRED), expiresAt
- Two-factor exit: Guard scans vehicle QR + resident's dynamic Exit Pass QR → validate signature, expiry, vehicle match → close session
- Remote approval: Guard creates request → resident polls pending list → approves/denies → guard completes exit
- Auto-expiry on read (compare expiresAt to now)
- Invalid/expired token rejection with audit logging
- **Endpoints:** `POST /gate/entry`, `POST /gate/exit`, `POST /gate/exit/remote/{sessionId}`, `GET /exit-pass/vehicle/{id}`, `POST /exit-approvals`, `GET /exit-approvals/pending`, `POST /exit-approvals/{id}/approve|deny`, `GET /exit-approvals/{id}/status`
- Session/log query endpoints for admins and guards

---

## Phase 3.5: Visitor Management & QR Passes (~6 days)

### 3.5A. Visitor Invitation System
- **Visitor entity:** name, phone, email, purpose, invited_by (resident_id), null values allowed. status (pending, checked_in, checked_out, expired, denied)
- **VisitPass entity:** visitor_id, pass_code (UUID), qr_data (encrypted), valid_from, valid_to, single_use/multi_use, max_entries
- Resident creates visitor invitation → system generates QR pass
- QR code generation (ZXing library)
- Pass validation endpoint (for gate scanning)
- Visitor pre-registration with vehicle plate/ or person
- Recurring visitor passes (e.g., nanny, cleaner — weekly schedule)
- SMS code from occupant to guard and visitor for manual verification (optional)
- Unit + integration tests

### 3.5B. Gate Decision Engine
- Integrate visitor passes with existing gate control system
- Decision logic (priority order):
  1. Check blacklist → DENY
  2. Check plate number against resident vehicles → ALLOW (via existing QR sticker flow)
  3. Check plate number against pre-registered visitor vehicles → ALLOW (within time window)
  4. Check QR pass validity → ALLOW (within time window, check use count)
  5. Manual security override → ALLOW/DENY with reason
- Blacklist management (plate numbers, individuals, reasons)
- Unit + integration tests for every decision path

---

## Phase 4: Billing, Payments & Dues (~6 days) — REFACTORED

### 4A. Charge Model (Separated into TenantCharge & EstateCharge)

The old unified `LevyType` model has been **replaced** with two distinct charge types:

#### TenantCharge (per-tenancy charges)
- **Table:** `tenant_charges`
- **Purpose:** Charges specific to a single tenancy (resident+unit combo) — rent, deposit, move-in fee, etc.
- **Fields:** name, description, amount, frequency, tenancy_id, estate_id, category, reminder_days_before (JSONB array of integers), active
- **API:** `/api/v1/tenant-charges`
  - `POST /` — Create tenant charge (ESTATE_ADMIN, SUPER_ADMIN)
  - `GET /` — List all (paginated)
  - `GET /{id}` — Get by ID
  - `PUT /{id}` — Update
  - `DELETE /{id}` — Soft delete
  - `GET /tenancy/{tenancyId}` — Get charges for a specific tenancy
  - `GET /estate/{estateId}` — Get all tenant charges in an estate
- **DTOs:**
  - `CreateTenantChargeRequest` — name (required), description, amount (required), frequency (required), tenancyId (required), estateId (required), category, reminderDaysBefore (List<Integer>)
  - `UpdateTenantChargeRequest` — name, description, amount, frequency, category, reminderDaysBefore (all optional)
  - `TenantChargeResponse` — id, name, description, amount, frequency, tenancyId, estateId, estateName, category, reminderDaysBefore, active, createdAt, updatedAt

#### EstateCharge (estate-wide charges)
- **Table:** `estate_charges` (renamed from `levy_types`)
- **Purpose:** Charges that apply to all tenancies in an estate — maintenance levy, security levy, cleaning, etc.
- **Fields:** name, description, amount, frequency, estate_id, category, reminder_days_before (JSONB), active
- **Exclusions:** Specific tenancies can be excluded from an estate charge via `estate_charge_exclusions` table (tenancy_id + reason)
- **API:** `/api/v1/estate-charges`
  - `POST /` — Create estate charge (ESTATE_ADMIN, SUPER_ADMIN)
  - `GET /` — List all (paginated)
  - `GET /{id}` — Get by ID
  - `PUT /{id}` — Update
  - `DELETE /{id}` — Soft delete
  - `GET /estate/{estateId}` — Get charges for an estate
  - `POST /{id}/exclusions` — Exclude a tenancy from this charge
  - `DELETE /{id}/exclusions/{exclusionId}` — Remove exclusion
  - `GET /{id}/exclusions` — List all exclusions for this charge
- **DTOs:**
  - `CreateEstateChargeRequest` — name (required), description, amount (required), frequency (required), estateId (required), category, reminderDaysBefore (List<Integer>)
  - `UpdateEstateChargeRequest` — name, description, amount, frequency, category, reminderDaysBefore (all optional)
  - `EstateChargeResponse` — id, name, description, amount, frequency, estateId, estateName, category, reminderDaysBefore, active, createdAt, updatedAt
  - `CreateExclusionRequest` — tenancyId (required), reason (optional)
  - `ExclusionResponse` — id, estateChargeId, tenancyId, reason, createdAt

#### ChargeInvoice (unified invoice — renamed from LevyInvoice)
- **Table:** `charge_invoices` (renamed from `levy_invoices`)
- **Key changes:** `levy_type_id` → `charge_id`, added `charge_type` field
- **ChargeType enum:** `TENANT_CHARGE`, `ESTATE_CHARGE`, `UTILITY`
- **API:** `/api/v1/invoices` (unchanged endpoints)
- **Updated DTOs:**
  - `CreateInvoiceRequest` — chargeId (was levyTypeId), chargeType (new), unitId, dueDate, billingPeriodStart, billingPeriodEnd, notes
  - `BulkInvoiceRequest` — chargeId (was levyTypeId), chargeType (new, optional), billingPeriodStart, billingPeriodEnd, dueDate
  - `InvoiceResponse` — chargeId (was levyTypeId), chargeName (was levyTypeName), chargeType (new), plus all existing fields

#### Due-Date Reminders
- **ChargeReminderScheduler** — Daily cron job (default 8 AM, configurable via `BILLING_REMINDER_CRON`)
- Each charge has `reminderDaysBefore` (e.g., `[7, 3, 1]` = reminders 7, 3, and 1 day before due)
- Falls back to estate-level default if charge has no reminders set (from `estate.settings` JSON `defaultReminderDays`)
- Overdue invoices get a one-time `CHARGE_OVERDUE` notification
- `charge_reminder_logs` table prevents duplicate notifications (unique on invoice_id + days_before)
- Notification types: `CHARGE_DUE_REMINDER`, `CHARGE_OVERDUE`

### 4B. Payment Integration (Days 20-22)
- **Payment entity:** invoice_id, amount, method (card, bank_transfer, ussd, mobile_money), reference, provider_reference, status, metadata
- Paystack integration (card, bank transfer, USSD)
- Flutterwave as fallback provider
- Webhook handlers for payment confirmation
- Receipt generation
- Payment reconciliation
- Wallet/credit system (overpayment applied to next invoice)
- Unit + integration tests

---

## Phase 5: Notifications & Communication (~4 days)

### 5A. Notification Service (Days 23-24)
- **Notification entity:** recipient_id, channel (whatsapp, sms, push, in_app, email), type, title, body, status, metadata
- Notification preference management per resident
- Template engine for notification content
- Async processing (Spring Events or message queue)

### 5B. Channel Integrations (Days 25-26)
- WhatsApp Business API (via Termii or Meta Cloud API)
- SMS (Termii as primary)
- Push notifications (Firebase Cloud Messaging)
- In-app notification feed
- Email (for admin-level notifications, optional)
- Retry and failure handling
- Unit + integration tests

---

## Phase 6: Amenities, Maintenance & Utilities (~6 days)

### 6A. Amenities Booking (Days 27-28)
- **Amenity entity:** name, type (football field, swimming pool, event hall, gym), capacity, pricing, booking_rules, estate_id
- **Booking entity:** amenity_id, resident_id, start_time, end_time, status, amount_paid
- Availability calendar
- Conflict detection (double-booking prevention)
- Payment integration for paid amenities
- Cancellation policy enforcement
- Estate dues payment like for cleaning/cutting grass

### 6B. Maintenance Requests (Days 29-30)
- **MaintenanceRequest entity:** unit_id, resident_id, category (plumbing, electrical, structural, general), description, priority, status (open, assigned, in_progress, resolved, closed), assigned_to, photos
- Request lifecycle workflow
- Assignment to artisan/vendor from registry
- Photo upload support
- Status tracking and resident notification

### 6C. Utility Tracking (Days 31-32)
- **UtilityReading entity:** unit_id, type (electricity, water, diesel_contribution), reading, period, cost
- Diesel/generator contribution tracking and splitting
- Shared utility cost allocation per unit
- Monthly utility statement generation

---

## Phase 7: Governance & Community (~4 days)

### 7A. Artisan/Vendor Registry (Day 33)
- **Artisan entity:** name, phone, category (plumber, electrician, DSTV, painter), rating, verified, estate_id
- Resident can request artisan → security logs entry/exit
- Rating system after job completion

### 7B. Community Governance (Days 34-36)
- **Announcement entity:** title, body, audience (all, block, unit_type), posted_by, estate_id
- **Poll/Vote entity:** question, options, eligible_voters, deadline, results
- **Violation entity:** unit_id, rule_violated, description, fine_amount, status
- AGM voting (proxy support)
- Estate rule violations and fines
- Complaint/petition tracking
- Announcement board with push notifications

---

## Phase 8: Audit & Reporting (~3 days)

### 8A. Audit Logging (Day 37)
- **AuditLog entity:** actor_id, action, entity_type, entity_id, old_value (JSON), new_value (JSON), ip_address, timestamp, tenant_id
- Tamper-evident design (hash chain — each log entry contains hash of previous entry)
- Aspect-based or event-based automatic capture
- Immutable storage (append-only)
- Audit log viewer in admin dashboard

### 8B. Reporting & Analytics (Days 38-39)
- Occupancy rates and trends
- Revenue dashboard (levy collection rate, outstanding, projected)
- Visitor traffic analytics (daily/weekly/monthly)
- Security incident reports
- Exportable reports (PDF, Excel)
- Configurable date ranges and filters

---

## Phase 9: Web Admin Dashboard (~10 days)

### 9A. Dashboard Foundation (Days 40-42)
- React project setup with routing, React Query + Zustand for state management, auth (Keycloak JS adapter)
- Layout: sidebar navigation, header with tenant context, responsive design
- Role-based route protection
- API client layer (Axios/fetch with JWT interceptor)
- Shared components: tables, forms, modals, toasts, loading states

### 9B. Module UIs (Days 43-47)
- Dashboard home (KPI cards, charts, recent activity)
- Estate & unit management screens
- Resident directory and profiles
- Vehicle registry
- Visitor management (invitations, pass generation, gate log)
- Billing & payments (invoices, payment history, statements)
- Amenity booking calendar
- Maintenance request board (Kanban-style)
- Utility tracking views
- Community (announcements, voting, violations)
- Audit log viewer
- Reports & analytics pages

### 9C. Polish & Responsive (Days 48-49)
- SaaS-level UI polish (consistent spacing, typography, color system)
- Mobile-responsive layouts for all screens
- Dark mode support
- Loading skeletons, empty states, error boundaries
- Accessibility basics (keyboard nav, ARIA labels)

---

## Phase 10: Mobile Apps (~10 days)

### 10A. Mobile Foundation (Days 50-52)
- React Native project setup (bare workflow)
- Navigation structure (bottom tabs + stack)
- Keycloak auth flow (PKCE for mobile)
- API client with offline caching
- Push notification setup (FCM)
- Low-bandwidth optimization (compressed payloads, image optimization)

### 10B. Resident App Screens (Days 53-56)
- Home dashboard (upcoming visitors, pending payments, announcements)
- Visitor invitation + QR pass sharing (WhatsApp share)
- Vehicle management
- Levy payment (in-app Paystack/Flutterwave)
- Amenity booking
- Maintenance request submission with camera
- Community feed (announcements, polls)
- Emergency/panic button (sends alert to security with GPS location)
- Profile management

### 10C. Security Guard App Screens (Days 57-59)
- Gate dashboard (expected visitors, recent events)
- QR scanner for visitor pass validation
- Plate number input + lookup
- Manual entry/exit logging
- Blacklist check
- Visitor check-in/check-out workflow
- Emergency alert receiver
- Low-bandwidth optimized (minimal data, works on 2G/3G)

---

## Phase 11: Core Security Module & Edge Service (~8 days)

### 11A. Edge Service Architecture (Days 60-62)
- Standalone Spring Boot edge service (deployable on-prem)
- Local SQLite/H2 database for offline operation
- Local cache of: resident vehicles, active visitor passes, blacklist
- Event queue for offline events (persisted to disk)
- REST sync with cloud (pull latest data, push queued events)
- Conflict resolution strategy (cloud wins for reference data, edge wins for events)
- Health monitoring and heartbeat to cloud

### 11B. LPR & Gate Integration (Days 63-65)
- LPR integration API (receive plate reads from camera system)
- Plate normalization and matching engine
- Gate control API (open/close barrier — hardware abstraction layer)
- Integration with gate decision engine (same logic as Phase 3B, running locally)
- Manual override interface for security guards
- Event logging with full context (plate image reference, timestamp, decision)

### 11C. Advanced Security Features (Days 66-67)
- Facial recognition integration hooks (API contract for FR system)
- Drone intervention hooks (event-triggered API calls — configurable per event type)
- License/activation system for edge deployment
- Encrypted communication (mTLS between edge and cloud)
- Tamper-evident local audit logs (hash chain)
- 24-48 hour offline operation capability

---

## Phase 12: Integration Testing, USSD & Final Polish (~5 days)

### 12A. USSD Integration (Day 68)
- Africa's Talking or Termii USSD integration
- USSD flows: check levy balance, pre-register visitor, get gate code
- Session management for USSD flows

### 12B. End-to-End Testing (Days 69-70)
- Full flow testing across all modules
- Multi-tenant isolation verification
- Payment webhook end-to-end
- Notification delivery verification
- Edge sync testing (simulate offline/online transitions)
- Performance testing (basic load test with k6 or Gatling)

### 12C. Deployment & DevOps (Days 71-72)
- GCP infrastructure setup (Cloud Run or GKE, Cloud SQL, Cloud Storage)
- CI/CD pipelines (GitHub Actions) for all 3 repos
- Environment configuration (dev, staging, prod)
- Monitoring (Cloud Monitoring + structured logging)
- Database backup strategy
- SSL/TLS setup
- Edge service Docker image for on-prem deployment

---
Addtions
### 13: Add tenacy form for residents to fill or Landlords can upload a documents then can can download
    - Tenants will upload the documents for admin to see.
    - Admin can download the documents for reference or for any other use.
    - 

## Estimated Total: ~72 working days

## Verification Strategy
After each phase:
1. All unit tests passing
2. Integration tests passing (Testcontainers)
3. Manual API testing via Postman collection
4. For UI phases: visual review of all screens
5. For mobile phases: test on Android emulator + iOS simulator
6. Final phase: full end-to-end smoke test of complete system

## Key Risks & Mitigations
- **Keycloak complexity:** Start with simple realm config, iterate as needed
- **Multi-tenancy bugs:** Tenant filter tested in every integration test
- **Payment integration:** Use Paystack sandbox extensively before production
- **Edge sync conflicts:** Keep conflict resolution simple (cloud wins for reference data)
- **Mobile performance on low-end devices:** Profile early on budget Android phones

##  Addon: WebSocket Chat Module 
- **Start app, test WebSocket connection with a STOMP client                                                                                                                                              │
- **Test REST endpoints: create conversation, send message, get history, mark as read                                                                                                                     │
- **Test real-time: two browser tabs (admin + resident) exchanging messages via WebSocket  

##  Addon: WebSocket Chat Module 
- **Start app, test WebSocket connection with a STOMP client                                                                                                                                              │
- **Test REST endpoints: create conversation, send message, get history, mark as read                                                                                                                     │
- **Test real-time: two browser tabs (admin + resident) exchanging messages via WebSocket  
- **Test WebSocket disconnection handling: simulate network loss, verify message persistence and reconnection behavior


-





