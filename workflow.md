# ElectiSelect — Production Workflow Specification

> **This document is a binding contract between frontend, backend, and database layers.**
> All rules marked ⛔ NON-NEGOTIABLE must be enforced unconditionally.

---

## 1. Project Overview

ElectiSelect is a college elective selection portal that replaces manual Google Forms and spreadsheets with a secure, real-time, and concurrency-safe system.

- Supports two elective types: **Department Electives** (ISE-only, internal) and **Open/Institutional Electives** (cross-department, seat-limited)
- Access is gated by **session-based control**: semester, time window, and eligibility must all be satisfied simultaneously
- Seat allocation for open electives uses **atomic DB transactions** to prevent overbooking
- **Database constraints are the final safety layer** — backend logic alone is not sufficient
- Admin roles control all structural data; students only interact with what is explicitly made available to them

---

## 2. System Architecture / Modules

### Module A — Department Elective (ISE Only)
- Internal to the Information Science & Engineering (ISE) department
- Admin defines **categories** (e.g., "Professional Elective", "Lab Elective")
- Each category contains multiple subjects
- Students must select **exactly one subject per category**
- **No seat limits** apply
- **No seat-based concurrency required** (no seat contention). DB UNIQUE constraints still handle duplicate submissions safely under concurrent requests.

### Module B — Open / Institutional Elective
- Cross-department elective selection
- Subjects have **fixed seat limits**
- **First-come-first-served** allocation
- Department-based **restriction lists** determine eligibility per subject
- **Concurrency-safe seat allocation is mandatory** (atomic transactions)

---

## 3. Roles & Authentication

### Authentication
- **Method:** Google OAuth exclusively
- **Domain enforcement:**
  - Students: `*@dsce.edu.in`
  - Staff: `*@dayanandasagar.edu`
- No other email domains are permitted

### Role Definitions

| Role | Email Pattern | Permissions |
|---|---|---|
| Student | `*@dsce.edu.in` | Select electives within active sessions |
| Staff | `*@dayanandasagar.edu` | View dashboards and exports (read-only) |
| Super Admin | Hardcoded email | Full system control: sessions, uploads, promotions |
| ISE Admin | Promoted staff (by Super Admin) | Manage department categories and subjects |

### Role Rules
- Super Admin email is hardcoded in the system — not stored as a promotable field
- ISE Admin is a promoted Staff member — promotion is performed by Super Admin
- Role elevation is controlled exclusively at the backend; frontend must never infer or grant roles

---

## 4. Data Model

### `users` — Static Identity Table
| Field | Description |
|---|---|
| `id` | Primary key |
| `name` | Full name |
| `email` | College email (unique) |
| `usn` | University Seat Number (unique) |
| `department` | Student's home department |
| `role` | Enum: STUDENT, STAFF, ISE_ADMIN, SUPER_ADMIN |
| `admission_year` | Year of admission |

### `student_academic` — Dynamic Academic State
| Field | Description |
|---|---|
| `student_id` | FK → users |
| `current_semester` | Integer (1–8), manually managed |
| `is_eligible` | Boolean — eligibility for current session |

> ⛔ NON-NEGOTIABLE: Semester is **NEVER** derived from USN or admission year. It is stored explicitly and updated manually by admins only.

### `sessions` — Access Control Records
| Field | Description |
|---|---|
| `id` | Primary key |
| `type` | Enum: OPEN, DEPARTMENT |
| `semester` | Target semester for this session |
| `academic_year` | e.g., "2024-25" |
| `is_active` | Boolean |
| `start_time` | Datetime — window open |
| `end_time` | Datetime — window close |

### `subjects_open` — Open Elective Subjects
| Field | Description |
|---|---|
| `id` | Primary key |
| `course_code` | Unique course code |
| `title` | Subject name |
| `department` | Offering department |
| `max_seats` | Total allowed seats |
| `filled_seats` | Current count (updated atomically) |
| `allowed_departments` | Array/JSON of department codes exclusively permitted to select (optional) |
| `restricted_departments` | Array/JSON of department codes blocked from selecting (used only if `allowed_departments` is null/empty) |
| `session_id` | FK → sessions |
| `is_deleted` | Boolean (soft delete) |

### `subjects_dept` — Department Elective Subjects
| Field | Description |
|---|---|
| `id` | Primary key |
| `title` | Subject name |
| `category_id` | FK → `categories` |
| `session_id` | FK → sessions |
| `is_deleted` | Boolean (soft delete) |

### `categories` — Department Elective Categories
| Field | Description |
|---|---|
| `id` | Primary key |
| `name` | Category name (e.g., "Professional Elective") |
| `session_id` | FK → sessions |

### `open_elective_selections` — Student Open Elective Choices
| Field | Description |
|---|---|
| `id` | Primary key |
| `student_id` | FK → users |
| `subject_id` | FK → subjects_open |
| `session_id` | FK → sessions |
| `selected_at` | Timestamp |
| **UNIQUE** | `(student_id, session_id)` |

### `dept_elective_selections` — Student Department Elective Choices
| Field | Description |
|---|---|
| `id` | Primary key |
| `student_id` | FK → users |
| `subject_id` | FK → subjects_dept |
| `category_id` | FK → categories |
| `session_id` | FK → sessions |
| `selected_at` | Timestamp |
| **UNIQUE** | `(student_id, category_id, session_id)` |

---

## 5. Session System

> ⛔ NON-NEGOTIABLE: The session system is the **primary access control gate**. All elective visibility and submission must pass session checks.

### Session Access Rule
A student may access electives **only when ALL of the following are true simultaneously:**

1. `student.current_semester == session.semester`
2. `session.is_active == true`
3. `current_time >= session.start_time AND current_time <= session.end_time`
4. `student.is_eligible == true`

Failure of **any one condition** must block access entirely.

### Session Lifecycle
- Only one active session of each type (OPEN / DEPARTMENT) is permitted at a time
- A session is created by Super Admin before any student interaction begins
- Session is deactivated manually by Super Admin or automatically when `end_time` is passed
- Subjects are linked to a specific session — they are not reused across sessions

### Session Uniqueness Enforcement
> ⛔ NON-NEGOTIABLE: The backend must prevent two simultaneous active sessions of the same type.

**Implementation — choose exactly one of the following approaches and apply consistently:**

- **Option A (DB Constraint — Preferred):** Add a partial unique index:
  ```sql
  CREATE UNIQUE INDEX one_active_session_per_type
  ON sessions (type)
  WHERE is_active = true;
  ```
  This prevents any second active session of the same type at the database level regardless of application logic.

- **Option B (Transactional Validation):** Before activating a session, execute within a transaction:
  1. `SELECT COUNT(*) FROM sessions WHERE type = :type AND is_active = true FOR UPDATE`
  2. If count > 0 → ROLLBACK, return error: `SESSION_ALREADY_ACTIVE`
  3. Else → proceed with activation

Whichever option is implemented, this rule must be enforced server-side. Frontend must not assume it can activate a session without this check passing.

### Session Enforcement
- Backend must evaluate session validity on **every API request** — not just at login
- Frontend must not cache session state to determine access

### Session State Endpoint
- ⛔ NON-NEGOTIABLE: The backend must expose current session state to the frontend via one of the following:
  - **`GET /api/student/profile`** — must include active session metadata (type, semester, is_active, start_time, end_time) alongside student identity, OR
  - **`GET /api/session/current`** — a dedicated endpoint returning active session state for the authenticated student's semester
- Frontend must fetch session state from this backend response on every dashboard load — it must **never** derive, infer, or cache session status independently
- If no active session exists for the student's semester, the backend returns a null/empty session object — the frontend renders the locked state based solely on this response

---

## 6. API Contract

### Authentication
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/google` | OAuth login, returns JWT with role |
| POST | `/api/auth/logout` | Logout — see note below |

> **JWT Authentication Model:** JWT is stateless. The server does not store session state.
> **Logout Behavior — choose exactly one and apply consistently:**
> - **Option A (Client-Side Token Removal — Simple):** Logout deletes the JWT from client storage. The token remains technically valid until expiry. Suitable if token TTL is short (e.g., ≤ 1 hour).
> - **Option B (Server-Side Blacklist — Secure):** Logout adds the token's `jti` (JWT ID) to a server-side blacklist (e.g., Redis). Every authenticated request checks the blacklist. Token is rejected immediately upon logout regardless of expiry.
>
> ⛔ The chosen strategy must be documented and consistently implemented. Do not mix approaches. If Option A is used, token TTL must be configured to a short window to limit exposure.

### Student Endpoints
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/student/profile` | Return student identity + academic state |
| GET | `/api/electives/open` | List eligible open elective subjects for student |
| POST | `/api/electives/open/select` | Submit open elective selection |
| GET | `/api/electives/dept` | List department elective categories + subjects |
| POST | `/api/electives/dept/select` | Submit department elective selections (all categories) |
| GET | `/api/student/selections` | View current confirmed selections |

### Admin Endpoints — Super Admin
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/admin/session/create` | Create new session (OPEN or DEPARTMENT) |
| PATCH | `/api/admin/session/:id/activate` | Activate session |
| PATCH | `/api/admin/session/:id/deactivate` | Deactivate session |
| POST | `/api/admin/subjects/open/upload` | Bulk upload open elective subjects via Excel |
| PATCH | `/api/admin/student/:id/semester` | Override individual student semester |
| POST | `/api/admin/semester/promote` | Batch promote: semester X → X+1 |
| POST | `/api/admin/staff/:id/promote-ise` | Promote staff to ISE Admin |
| GET | `/api/admin/dashboard/open` | Open elective analytics |
| GET | `/api/admin/dashboard/dept` | Department elective analytics |
| GET | `/api/admin/export/open` | Export open elective data (CSV/Excel) |
| GET | `/api/admin/export/dept` | Export department elective data |

### Admin Endpoints — ISE Admin
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/ise/category/create` | Create a new category for a session |
| POST | `/api/ise/subjects/add` | Add subject to a category |
| POST | `/api/ise/subjects/upload` | Bulk upload department subjects via Excel |
| DELETE | `/api/ise/subject/:id` | Soft-delete a subject |

### Staff Endpoints
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/staff/dashboard` | View-only monitoring dashboard |
| GET | `/api/staff/export` | Export selections |

### API Response Standards
- All errors return structured JSON: `{ "error": true, "code": "ERROR_CODE", "message": "..." }`
- Duplicate selection error code: `ALREADY_SELECTED`
- Session invalid error code: `SESSION_INVALID`
- Seat unavailable error code: `NO_SEATS_AVAILABLE`
- Eligibility failure error code: `NOT_ELIGIBLE`
- Session already active error code: `SESSION_ALREADY_ACTIVE`

### Idempotency & Retry Safety
- ⛔ Selection APIs (`POST /api/electives/open/select`, `POST /api/electives/dept/select`) are **idempotency-safe via DB UNIQUE constraints** — a duplicate request from the same student for the same session will always return `ALREADY_SELECTED` and never create a second record or decrement seats twice.
- The system must remain **fully consistent under client retries** — network timeouts, dropped connections, or frontend re-submissions must not produce duplicate selections or double seat decrements.
- Clients must treat an `ALREADY_SELECTED` response on retry as a **success confirmation**, not an error requiring user intervention.
- ⛔ NON-NEGOTIABLE: The combination of DB UNIQUE constraints and the atomic transaction flow (Section 10) guarantees consistency under concurrent and retry scenarios without requiring client-side deduplication logic.

---

## 7. Business Rules

> ⛔ All rules below are enforced at the backend. Frontend enforces none of these as primary validation.

1. A student may select **only one open elective per session**
2. A student must select **one subject per category** in department electives — all categories must be completed
3. Selections are **final** — no editing or cancellation after submission
4. A subject restricted for a student's department must **never appear** in that student's subject list (filtered at query level)
5. Semester is **never** inferred from USN, admission year, or any other derived field
6. A student whose `is_eligible == false` is blocked from all selection endpoints regardless of session state
7. Only one active session per type (OPEN / DEPARTMENT) may exist at any time
8. Subjects use **soft delete only** — hard deletes are prohibited to preserve historical records
9. A new academic year or semester batch promotion does not auto-clear past selections
10. Staff role has **zero write access** to any data — all staff interactions are strictly read-only, including dashboard monitoring and export operations. Staff may not create, update, or delete any record in any table.
11. **Post-selection lock:** Once a student has a confirmed selection for a session, the backend must reject any further calls to selection endpoints for that session — returning `ALREADY_SELECTED`. The student may only access `GET /api/student/selections` to view their confirmed choice. No edit, re-select, or cancel endpoint exists. This restriction is enforced at the backend regardless of frontend state.

> ⛔ NON-NEGOTIABLE: A student with an existing selection for a session must be blocked from submitting another selection for the same session at the backend layer. The frontend locking of buttons is supplementary only.

---

## 8. Department Elective Logic (ISE-Specific)

### Structure
- ISE Admin creates **categories** (e.g., "Professional Elective 1", "Open Lab")
- Each category contains **N subjects**
- Categories and subjects are linked to a specific `session_id`

### Selection Rules
- Student must select **exactly one subject per category**
- Student submission must include **one selection for every category** in the session — partial submissions are rejected
- No seat limits exist on department elective subjects
- **No seat-based concurrency required.** DB UNIQUE constraints still handle duplicate submissions safely under concurrent requests — two simultaneous submissions from the same student for the same category will result in exactly one succeeding and the other receiving `ALREADY_SELECTED`.

### Submission Validation (Backend)
1. Verify session is active, within time window, and matches student semester
2. Verify student is eligible (`is_eligible == true`)
3. **Pre-insert payload validation (enforced BEFORE any DB write):**
   - Reject if the submission contains **duplicate `category_id` values** — each category must appear exactly once
   - Reject if the **number of submitted selections does not equal the number of categories defined for the session** — partial and over-submitted payloads are both invalid
4. Verify the submitted category IDs exactly match all categories defined for the session — no extra, no missing
5. Verify each submitted subject belongs to the corresponding submitted category and is not soft-deleted
6. Verify student has no existing selections for this session (DB constraint will block, but backend should pre-check)
7. Insert all selections atomically — reject the entire batch if any insert fails

### Admin Capabilities
- Create categories per session
- Add subjects to categories individually or via Excel upload
- Soft-delete subjects (existing selections referencing deleted subjects are preserved)

---

## 9. Open Elective Logic

### Subject Visibility Filter
Before returning subject list to student, backend must:
1. Filter by active session matching student's semester
2. Apply department access rule (evaluated in order):
   - If `subject.allowed_departments` is set (non-null, non-empty) → **ONLY** students whose department appears in `allowed_departments` may see and select this subject
   - Else → exclude subjects where `student.department` appears in `subject.restricted_departments`
   - A subject with neither field set is visible to all departments
3. Exclude subjects with `is_deleted == true`
4. Include remaining seat count (`max_seats - filled_seats`) in response

> ⛔ NON-NEGOTIABLE: `allowed_departments` takes precedence over `restricted_departments`. If both are present, `allowed_departments` is the authoritative filter and `restricted_departments` is ignored.

### Selection Rules
- One selection per student per session — enforced at DB level by `UNIQUE(student_id, session_id)`
- No modification after selection
- Student cannot select a restricted or non-permitted subject even if they manipulate the request directly

### Restriction Logic
- `allowed_departments` and `restricted_departments` are array/JSON fields on each subject
- Backend applies the department access rule **before returning subjects** — ineligible subjects are never sent to the student client
- Backend re-applies the **same department access rule at selection time** in case of direct API calls — visibility filtering alone is not sufficient

---

## 10. Concurrency Handling — Open Elective Seat Allocation

> ⛔ NON-NEGOTIABLE: All seat allocations MUST follow this exact transaction flow.

### Step-by-Step Transaction Flow

```
BEGIN TRANSACTION

1. SELECT subject WHERE id = :subject_id FOR UPDATE
   -- Locks the row exclusively, preventing any concurrent modification
   -- No other transaction can read-modify-write this row until COMMIT or ROLLBACK

2. IF subject.filled_seats >= subject.max_seats THEN
   ROLLBACK
   RETURN error: NO_SEATS_AVAILABLE

3. IF subject.is_deleted == true THEN
   ROLLBACK
   RETURN error: SUBJECT_UNAVAILABLE

4. APPLY department access rule:
   IF subject.allowed_departments IS SET THEN
     IF student.department NOT IN subject.allowed_departments THEN
       ROLLBACK
       RETURN error: DEPARTMENT_RESTRICTED
   ELSE
     IF student.department IN subject.restricted_departments THEN
       ROLLBACK
       RETURN error: DEPARTMENT_RESTRICTED

5. RE-VALIDATE session state inside transaction:
   SELECT session WHERE id = :session_id
   IF session.is_active == false THEN
     ROLLBACK
     RETURN error: SESSION_INVALID
   IF current_time > session.end_time THEN
     ROLLBACK
     RETURN error: SESSION_INVALID
   IF student.current_semester != session.semester THEN
     ROLLBACK
     RETURN error: SESSION_INVALID
   -- WHY: A session may be deactivated, expired, or the student's semester may
   -- have been updated by an admin between the time the student fetched the
   -- subject list (GET) and submitted the selection (POST). The outer pre-request
   -- check is not sufficient — session state must be confirmed inside the
   -- transaction while the subject row lock is held, ensuring no race condition
   -- between session expiry and seat allocation.

6. UPDATE subjects_open SET filled_seats = filled_seats + 1 WHERE id = :subject_id
   -- Seat is claimed BEFORE the selection record is inserted
   -- This ensures seat count integrity even if the subsequent INSERT fails;
   -- any failure after this step triggers a full rollback, reverting the count

7. PRE-INSERT DUPLICATE CHECK:
   SELECT COUNT(*) FROM open_elective_selections
   WHERE student_id = :student_id AND session_id = :session_id
   IF count > 0 THEN
     ROLLBACK
     RETURN error: ALREADY_SELECTED
   -- WHY: The DB UNIQUE constraint (step 8) is the final guard and will always
   -- catch duplicates. This pre-check exists solely for controlled, predictable
   -- error handling — catching the duplicate before the INSERT attempt allows
   -- the backend to return a clean ALREADY_SELECTED response rather than
   -- propagating a raw DB constraint violation. Do NOT remove the DB constraint
   -- on the assumption that this pre-check is sufficient; both must coexist.

8. INSERT INTO open_elective_selections (student_id, subject_id, session_id, selected_at)
   -- DB UNIQUE constraint catches any duplicate that bypasses the pre-check
   -- (e.g. concurrent requests that pass step 7 simultaneously)
   -- If this INSERT fails (e.g. duplicate), the entire transaction rolls back,
   -- including the filled_seats increment from step 6 — no phantom seat loss

COMMIT TRANSACTION
```

> **Rollback Safety:** Every failure path (steps 2–8) issues an explicit ROLLBACK. Because `filled_seats` is incremented inside the same transaction as the INSERT, any failure at any step leaves the database in its pre-transaction state. No partial updates are possible.

### Error Handling
- Duplicate detected at pre-check (step 7) → return `ALREADY_SELECTED` (controlled path)
- Duplicate insert bypassing pre-check (UNIQUE constraint violation at step 8) → return `ALREADY_SELECTED` (final guard path)
- Seat overflow caught at step 2, not by constraint — constraint is supplementary
- Session expiry or invalidation caught at step 5 — returns `SESSION_INVALID`
- Any failure in steps 1–8 triggers full rollback — `filled_seats` is never permanently incremented without a matching selection record
- Frontend receives a single structured error response — no partial state

---

## 11. Frontend vs Backend Responsibilities

### Frontend Responsibilities (UI Only)
- Render elective lists received from backend — no filtering logic
- Disable submit button after first click (prevent double-submission UI)
- Display error messages returned by backend — do not generate custom validation messages
- Show seat counts as returned by backend — do not compute locally
- ⛔ Seat counts displayed to the user are **NON-AUTHORITATIVE** — they are point-in-time snapshots and may be stale by the time a student submits. The backend transaction is the **only source of truth** for seat availability. Frontend must never gate or allow a submission based on the displayed seat count.
- Do not store or cache session state for access control decisions
- Redirect to appropriate dashboard based on `role` returned in auth response

### Backend Responsibilities (All Business Logic)
- Enforce all session validity checks on every request
- Evaluate student eligibility on every selection request
- Filter subjects by department access rule (allowed/restricted) before returning to frontend
- Validate semester match between student and session
- Execute atomic transactions for seat allocation
- Catch and return DB constraint violations as structured errors
- Enforce role-based access on every endpoint
- Validate that department elective submissions are complete, non-duplicate, and correctly matched to categories
- Reject any submission outside the session time window

> ⛔ NON-NEGOTIABLE: The backend must never trust frontend-provided data for eligibility, session status, seat count, or role.

> ⛔ NON-NEGOTIABLE: **The backend is the single source of truth.** Frontend state must always be treated as potentially stale, manipulated, or incorrect. Every state-affecting request must be fully re-validated on the backend regardless of what the frontend claims or displays.

---

## 12. Expected System Flow

### Super Admin Flow
1. Login via Google OAuth (hardcoded email matched)
2. Create a session (type: OPEN or DEPARTMENT, semester, academic year, start/end time)
3. Upload subjects (Open: via Excel with seats and restrictions; Dept: ISE Admin handles)
4. Activate session
5. Monitor dashboard in real time
6. Deactivate session after window closes
7. Export results

### ISE Admin Flow
1. Login via Google OAuth (promoted staff account)
2. Create categories for an active DEPARTMENT session
3. Add subjects to each category (individually or via Excel)
4. Monitor department elective dashboard

### Student Flow
1. Login via Google OAuth (`@dsce.edu.in` domain)
2. Register profile (USN, department — stored in `users`)
3. Navigate to dashboard — backend returns session state and available electives
4. If no active session or ineligible: dashboard shows locked state with reason
5. Select electives (open or department) within allowed time window
6. Confirmation screen shown after successful submission — selections are permanently locked
7. All selection endpoints are now blocked for this session — student may only access `GET /api/student/selections` to view confirmed choices. Backend enforces this regardless of frontend state.

### Staff Flow
1. Login via Google OAuth (`@dayanandasagar.edu` domain)
2. View monitoring dashboard (read-only)
3. Export selection data if permitted

---

## 13. Edge Cases

| Scenario | Handling |
|---|---|
| Student submits twice (double-click) | Frontend disables button; backend pre-checks; DB UNIQUE constraint blocks |
| Two students claim last seat simultaneously | Row-level lock in transaction ensures only one succeeds; second gets `NO_SEATS_AVAILABLE` |
| Diploma/lateral entry student semester mismatch | Admin manually overrides `current_semester` for individual student |
| Student with backlog in a different semester | Admin manually overrides `current_semester` to correct value |
| Subject deleted after session starts | Soft delete preserves existing selections; subject removed from future queries |
| Student submits department electives missing a category | Backend rejects entire submission — all categories must be covered |
| Multiple active sessions of same type | System must enforce: only one active session per type; creation of second blocks or deactivates first |
| API called directly with tampered subject_id | Backend re-validates restriction, seat count, session, and eligibility — selection is rejected |
| Student attempts to change selection after submission | Backend has no edit endpoint; all selection endpoints insert-only; post-selection calls return `ALREADY_SELECTED` |
| Student calls selection endpoint after confirming (direct API) | Backend pre-check detects existing selection for session → ROLLBACK → returns `ALREADY_SELECTED`; DB UNIQUE constraint is final guard |
| Session time window expires mid-selection | Backend validates `current_time <= session.end_time` at submission time; late submissions rejected |

---

## 14. Database Constraints

### Open Elective Uniqueness
```sql
UNIQUE(student_id, session_id) ON open_elective_selections
```
- Ensures each student selects at most one subject per session
- Prevents: double-click duplicates, API replay attacks, race condition duplicates

### Department Elective Uniqueness
```sql
UNIQUE(student_id, category_id, session_id) ON dept_elective_selections
```
- Ensures one subject per category per student per session
- Allows multiple category selections (one row per category)

### Subject Seat Integrity
- `filled_seats` must never exceed `max_seats` — enforced via transaction logic (step 2 in concurrency flow)
- A CHECK constraint may be added: `CHECK (filled_seats <= max_seats)` as supplementary guard

### Soft Delete Enforcement
- `is_deleted BOOLEAN DEFAULT false` on all subject tables
- No physical row deletion is permitted in production

### General Constraints
- `current_semester` on `student_academic`: `CHECK (current_semester BETWEEN 1 AND 8)`
- `session.type`: Enum constraint — only `OPEN` or `DEPARTMENT`
- `users.role`: Enum constraint — only `STUDENT`, `STAFF`, `ISE_ADMIN`, `SUPER_ADMIN`

> ⛔ NON-NEGOTIABLE: Do NOT use database triggers for any business logic. UNIQUE constraints and CHECK constraints are the only DB-level enforcement mechanisms used.

---

## 15. Validation Layers

### Layer 1 — Frontend (UI Hardening Only)
| Check | Action |
|---|---|
| Button clicked | Disable immediately after first click |
| Submission attempted | Show loading state, await backend response |
| Error received | Display backend error message verbatim |

Frontend performs **zero business logic validation**. It only prevents accidental duplicate UI actions.

### Layer 2 — Backend (Primary Enforcement)
All of the following are checked on every relevant request:

| Check | On Which Requests |
|---|---|
| JWT valid and not expired | All authenticated requests |
| Role authorization | All role-restricted endpoints |
| Session exists and is active | All elective list and selection endpoints |
| Session re-validated inside transaction (is_active, end_time, semester) | Open elective selection submit — inside DB transaction at step 5 |
| Current time within session window | All selection submission endpoints |
| Student semester == session semester | All selection submission endpoints |
| Student is_eligible == true | All selection submission endpoints |
| Department access rule check (allowed/restricted) | Open elective list fetch + selection submit |
| Seat availability check | Open elective selection submit |
| No duplicate category_id in payload | Department elective selection submit |
| Submission count equals category count | Department elective selection submit |
| All categories present in dept submission | Department elective selection submit |
| Subject belongs to submitted category | Department elective selection submit |
| Idempotency — duplicate detected via UNIQUE constraint | All selection submission endpoints |

### Layer 3 — Database (Final Guard)
| Constraint | Purpose |
|---|---|
| `UNIQUE(student_id, session_id)` | Blocks duplicate open elective selections at DB level |
| `UNIQUE(student_id, category_id, session_id)` | Blocks duplicate dept selections per category |
| Enum constraints on role and session type | Prevents invalid state storage |
| `CHECK (filled_seats <= max_seats)` | Supplementary seat overflow guard |
| FK constraints | Prevents orphaned records across all related tables |

---

## 16. Integration Requirements

### Excel Upload Format — Open Electives
| Column | Description |
|---|---|
| `course_code` | Unique course identifier |
| `title` | Subject name |
| `department` | Offering department |
| `max_seats` | Integer |
| `allowed_departments` | Comma-separated department codes — if set, ONLY these departments may select (takes precedence over restricted_departments) |
| `restricted_departments` | Comma-separated department codes — blocked from selecting (used only if allowed_departments is empty) |
| `session_id` | Must match an existing session ID |

### Excel Upload Format — Department Subjects
| Column | Description |
|---|---|
| `category_name` | Must match an existing category for the session |
| `subject_title` | Subject name |
| `session_id` | Must match an existing session ID |

### Export Format — All Selections
| Column |
|---|
| Student Name |
| USN |
| Department |
| Semester |
| Subject Name |
| Course Code |
| Selected At |

### Export Options
- Full dataset (all students for a session)
- Subject-wise dataset (all students who selected a given subject)
- Format: CSV or Excel

### Dashboard Metrics (Real-Time)
- Total students eligible for session
- Count: students who have selected
- Count: students who have NOT yet selected
- Per-subject: seats filled, seats remaining

### AI Tool Integration Notes (e.g., Antigravity)
- All endpoints are RESTful JSON — no GraphQL
- JWT is the sole auth mechanism — pass in `Authorization: Bearer <token>` header
- Session state is not managed client-side — always fetch from `/api/student/profile` or session endpoint
- Seat counts in subject list responses are real-time snapshots — do not cache
- Error codes are stable enums — build error handling logic against codes, not message strings
- All timestamps are ISO 8601 UTC
- Excel uploads use multipart/form-data with a single `file` field
- Soft-deleted subjects (`is_deleted: true`) must never appear in any student-facing response

---

*End of ElectiSelect Workflow Specification*
