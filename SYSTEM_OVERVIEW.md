# Tax Records Portal — Full System Overview

> This document describes the entire system from a product/interaction perspective. It is intended for any agent building the frontend. Backend implementation details are omitted — only data shapes, flows, roles, and behaviors are described.

---

## Roles

| Role Key | Display Name | Description |
|----------|-------------|-------------|
| MANAGER | Manager | Full admin access. Creates all internal user accounts, manages system configuration, views analytics. |
| OOS | Onboarding, Offboarding & Support | Onboards new clients — fills in their profile, submits for QA review, handles handoff. Can also be assigned as an accountant post-handoff. |
| QTD | Quality, Training & Development | Reviews submitted client profiles and tasks. Creates and assigns tax record tasks to accountants. Approves or rejects work. |
| CSD | Client Service Delivery | Assigned accountant role. Executes tasks (uploads files, submits work). Sends notices to clients. |
| BILLING | Internal Accounting / Billing | Internal accounting role. Details TBD. |
| CLIENT | Client | View-only access to their own finalized records, documents, and billing. Cannot upload, edit, or create anything. |

---

## User Model

- **Fields**: firstName, lastName, email, profileUrl, status, position, titles
- **Status**: PENDING → ACTIVE (activated via email link with token)
- **Position**: nullable lookup (e.g., "Jr Accountant III", "Sr Accountant I", "Operations MRE") — only for internal users, not clients
- **Titles**: list of `{ prefix: boolean, title: string }`
  - `prefix: true` → title before name (e.g., "CPA John Doe")
  - `prefix: false` → title after name (e.g., "John Doe, CPA")
- **Password**: not set at account creation. User sets it via activation link.

---

## Client Lifecycle

### Statuses
```
ONBOARDING → APPROVED → ACTIVE
```

### Flow
1. **OOS creates the client** — sets the client's email, status starts as `ONBOARDING`
2. **OOS fills in the client info** (the full profile form — see Client Info Sections below)
3. **OOS submits for QTD review** — Client Info Task status: OPEN → SUBMITTED
4. **QTD reviews** and either:
   - **Approves** → client info is saved as the live profile, client status → `APPROVED`, activation email sent to client
   - **Rejects** → task goes back to OOS with a comment, OOS makes corrections, resubmits
5. **Client receives activation email**, clicks link, sets password, account is fully activated
6. **OOS performs handoff** — assigns accountants (CSD/OOS) to the client, client status → `ACTIVE`
7. **After handoff**: OOS only retains a frozen snapshot of the client info at handoff time — no further access to the client's ongoing work (unless also assigned as an accountant)

### Handoff Rules
- OOS assigns one or more accountants (from CSD or OOS roles) to the client
- OOS can assign themselves if they'll continue post-onboarding
- Only Manager can reassign accountants after handoff
- A frozen snapshot of client info is saved at handoff time

---

## Client Info — The Full Profile

The client info profile is a large structured form with **7 top-level sections**, each stored as a **single JSONB column**. Sub-sections are nested within their parent section — they are NOT separate columns.

All sections are stored as structured JSON. The backend provides a **template** (the empty/default structure) and returns/accepts the same shape for the live data.

### Reusable Data Types
- **DateField**: `{ date: date, isImportant: boolean }` — ALL date fields throughout the entire form use this wrapper
- **FileReference**: `{ id: UUID, name: string }` — represents an uploaded file
- **LinkReference**: `{ url: string, label: string }` — represents a clickable URL
- **PermitDetails**: `{ number: string, expirationDate: DateField }` — permit info (uses DateField)
- **Rich Text**: TipTap/ProseMirror JSON document format — the frontend owns the editor schema and rendering, the backend just stores and returns the JSON blob

---

### Section 1: Main Details (`mainDetails`)
Single object with:
- `mreCode` (string)
- `commencementOfWork` (DateField)
- `engagementStatus` (dropdown): ACTIVE, INACTIVE

**Computed fields** (not stored — derived in the API response):
- Client display name (from `clientInformation.registeredName` + `tradeName`)
- Taxpayer category (derived from gross sales in Section 2B)
- Assigned CSD/OOS Accountants (from client-accountant relationship)
- Assigned QTD Accountant (from client relationship)

---

### Section 2: Client Information (`clientInformation`)
This is a **single JSONB column** containing header fields and **9 sub-sections (A–I)**.

**Header fields:**
- `registeredName` (string)
- `tradeName` (string)
- `numberOfBranches` (int)
- `organizationType` (dropdown): SOLE_PROPRIETORSHIP, PROFESSIONAL, PARTNERSHIP, OPC, REGULAR_CORPORATION

#### A. BIR Details — List (per branch)
Each branch entry:
- `businessTradeName`, `tin`, `rdo`, `completeRegisteredAddress` (strings)
- `birRegistrationNumber`, `mannerOfFilingTaxReturn`, `typeOfBusiness`, `classification` (strings)
- `dateOfBirRegistration` (DateField)
- 9 file uploads: birCertificateOfRegistration, birForm1901, birForm1921Atp, birForm1905, sampleInvoiceReceipts, niriPoster, birBookOfAccountsStamp, birForm2000Dst, contractOfLease

Default: 1 empty branch entry. User can add more.

#### B. BIR Tax Compliance — Single object
- `grossSales`: list of `{ year: int, amount: decimal }` — user can add year entries
- `topWithholding` (string)
- `dateClassifiedTopWithholding` (DateField)
- `incomeTaxRegime` (string)

Taxpayer Classification is computed from grossSales (not stored).

#### C. BIR Compliance Breakdown — Checklist
A pre-seeded list of **29 BIR tax return items** across 8 categories. The OOS checks which ones are applicable to the client.

Each item:
- `category` (string) — grouping header
- `taxReturnName` (string) — the specific form/filing name
- `deadline` (string) — human-readable deadline text
- `applicable` (boolean) — checkbox, default false
- `notes` (string) — optional notes per item

**Categories and items:**

| Category | Items |
|----------|-------|
| Income Tax Return (ITR) | BIR Form 1701 (Annual, Individuals), BIR Form 1701Q (Quarterly, Individuals), BIR Form 1702 (Annual, Corporation), BIR Form 1702Q (Quarterly, Corporation), SAWT |
| Value Added Tax (VAT) | BIR Form 2550Q, SLSPI |
| Percentage Tax (PT) | BIR Form 2551Q |
| Withholding Tax Compensation | BIR Form 1601-C, BIR Form 1604C |
| Withholding Tax Expanded | BIR Form 0619-E, BIR Form 1601EQ, QAP-E, BIR Form 1604E, Alpha-Payees (E) |
| Withholding Tax Final | BIR Form 0619-F, BIR Form 1601FQ (quarterly), QAP-F (quarterly), BIR Form 1601FQ (annual), QAP-F (annual) |
| Withholding Tax - VAT Remittance | 1600-VT |
| e-Sales Report Submission | Monthly eSales Reporting |
| Other BIR Filings | Alphalist & 2316, Inventory List, Sworn Statement (Online Seller), Sworn Statement (Professional), LIS, BIR Form 2307/2304, BIR Form 2000 DST |

Plus an `othersSpecify` (string) field for anything not in the list.

#### D. DTI Details — Single object
- DTI Registration: registrationNo, dateOfRegistration (DateField), dateOfExpiration (DateField)
- DTI files: businessRegistrationCertificate, bnrsUndertakingForm, officialReceipt (all FileReference)
- BMBE Compliance: totalAssets, bmbeNo, dateOfRegistration (DateField), dateOfExpiration (DateField)
- BMBE file: officialReceipt (FileReference)
- others (string)

#### E. SEC Details — Single object
- dateOfIncorporation (DateField), secRegistrationNumber, dateOfActualMeetingPerBylaws (DateField), primaryPurposePerArticles, corporationCategory
- 10 file uploads: certificateOfIncorporation, articlesOfIncorporation, bylawsOfCorporation, certificateOfAuthentication, authorizeFilerSecretaryCertificate, officialReceipts, latestGisOrAppointmentOfOfficer, stockAndTransferBook, boardResolutionsSecretaryCertificate, previousYearAfsAndItr
- others (string)

#### F. SSS — Single object (GovernmentAgencyDetails)
#### G. Philhealth — Single object (GovernmentAgencyDetails)
#### H. HDMF — Single object (GovernmentAgencyDetails)
All three share the same structure:
- `dateOfRegistration` (DateField)
- `registrationNumber` (string)
- `certificatesAndDocuments` (FileReference)
- `others` (string)

#### I. City Hall — List (per city)
Each city entry:
- `businessPermitCity`, `businessPermitNumber` (strings)
- `dateOfRegistration` (DateField)
- `renewalBasis` (string)
- `quarterlyDeadlineQ2`, `quarterlyDeadlineQ3`, `quarterlyDeadlineQ4` (DateField)
- `permitExpirationDate` (DateField)
- 3 permit sub-objects: firePermit, sanitaryPermit, otherPermit — each `{ number: string, expirationDate: DateField }`
- 11 file uploads: mayorBusinessPermit, businessPermitPlate, billingAssessment, officialReceiptOfPayment, sanitaryPermitFile, firePermitFile, barangayPermit, communityTaxCertificate, locationalClearance, environmentalClearance, comprehensiveGeneralLiabilityInsurance

Default: 1 empty city entry. User can add more.

---

### Section 3: Owner's/Corporate Officer's Information (`corporateOfficerInformation`)
This is a **single JSONB column** containing two sub-sections.

**Corporate Officers** — List (user can add/remove entries)
Each entry:
- `name` (string)
- `birthday` (DateField)
- `address` (string)
- `position` (string)
- `idScannedWith3Signature` (FileReference)

Default: 1 empty entry.

**Point of Contact Details** — Single object
- `contactPerson` (string)
- `contactNumber` (string)
- `deliveryAddress` (string)
- `landmarkPinLocation` (string)
- `emailAddress` (string) — this is also the email used for creating the client's user account
- `preferredMethodOfCommunication` (rich text)
- `alternativeContact` (string)

---

### Section 4: Access and Credentials (`accessCredentials`) — List
Each entry:
- `platform` (string)
- `linkToPlatform` (LinkReference: `{ url, label }`)
- `usernameOrEmail` (string)
- `password` (string)
- `notes` (string)

Default: 1 empty entry. User can add more.

---

### Section 5: Scope of Engagement (`scopeOfEngagement`)

**Header:**
- `dateOfEngagementLetter` (DateField)
- `engagementLetter` (FileReference)

**A. Documents & Information Gathering** — 6 rich text fields
- `salesInvoicesAndDocuments` (rich text)
- `purchaseAndExpenseDocuments` (rich text)
- `payrollDocuments` (rich text)
- `sssPhilhealthHdmfDocuments` (rich text)
- `businessPermitsLicensesAndOtherDocuments` (rich text)
- `additionalNotes` (rich text)

**B. Client Engagements**
- `taxCompliance` (rich text)
- **Bookkeeping sub-section:**
  - `bookOfAccounts` (dropdown/string)
  - `bookkeepingPermitNo` (string)
  - `looseleafCertificateAndBirTemplate` (FileReference)
  - `registeredBooks` — list of `{ bookName: string, notes: string }` (table)
  - `bookkeepingProcess` (rich text)
- `sssPhilhealthHdmfEngagement` (rich text)
- `paymentAssistance` (rich text)
- **Consultation Hours sub-section:**
  - `freeHoursPerMonth` (decimal) — always 2, fixed
  - `ratePerHourAfterFree` (decimal) — always 500, fixed
  - `consultations` — list of entries, each:
    - `date` (DateField)
    - `timeStarted` (string, e.g. "10:00 AM")
    - `timeEnded` (string, e.g. "11:30 AM")
    - `topicsAndDocumentation` (rich text)
    - `numberOfHours` (decimal)
    - `platform` (string)
  - `totalBillableAmount` (decimal) — auto-computed: sum of (hours - 2 free) × 500, but **manually overridable** for edge cases
  - Frontend should compute this for instant UI feedback, but the saved value is the source of truth

**C. Required Deliverable & Report**
- `standardDeliverable` (rich text)
- `requiredDeliverableOthers` (string)

---

### Section 6: Professional Fees (`professionalFees`) — List
Each entry:
- `serviceName` (string) — pre-seeded, but editable
- `fee` (string) — user fills in

**Pre-seeded items (9):**
1. Monthly Professional Fees
2. General Information Sheet (GIS)
3. Annual Alphalist of Employees & BIR Form 2316
4. Inventory List Submission
5. Sworn Statement of Gross Remittance - Online Seller
6. Sworn Statement - Professional
7. Lessee Information Sheet (LIS)
8. Loose-leaf
9. Annual Escalation Fees

User can add more entries beyond the pre-seeded ones.

---

### Section 7: Onboarding Information, Documents & Notes (`onboardingDetails`)

**Group Chat Info:**
- `nameOfGroupChat` (string)
- `platformUsed` (dropdown: Viber, WhatsApp, Messenger, Telegram, Other)
- `gcCreatedBy` (string)
- `gcCreatedDate` (DateField)

**Meetings** — List (user can add entries)
Each entry:
- `titleOfMeeting` (string)
- `date` (DateField)
- `timeStarted` (string)
- `timeEnded` (string)
- `agenda` (string)
- `linkToMeetingRecording` (LinkReference: `{ url, label }`)
- `minutes` (rich text)

**Pending Action Items** — List (user can add entries)
Each entry:
- `particulars` (string)
- `notes` (string)

---

## Client Info Task — Review Workflow

A unified system for both initial onboarding fill-up and later edits to client info.

### How It Works
1. Someone (OOS during onboarding, or an accountant later) creates a **Client Info Task**
2. The task contains a **full snapshot** of the client info (all 7 sections)
3. The task goes through a review cycle:
   ```
   OPEN → SUBMITTED → REJECTED (cycle back, resubmit) → APPROVED
   ```
4. On **approval**: the snapshot overwrites the live client info
5. A **diff** between the snapshot and the current live data is computed programmatically (not stored) — this lets reviewers see exactly what changed

### Action Logs
Every status transition is logged with: performer, comment, and timestamp.

---

## Tax Record Tasks — Work Assignment & Execution

### Overview
QTD creates tasks and assigns them to accountants. Accountants upload files, submit for review, and manage the filing lifecycle.

### Task Structure (Drill-Down Hierarchy — 6 layers)
1. **Category** (dropdown): SEC Files, City Hall Files, DTI Files, Book of Accounts, Internal Tax Compliance Reports, Client Documents & Records, SSS Philhealth & HDMF Files, BIR Files, Adhoc & Consultation
2. **Sub Category** (dropdown, filtered by category)
3. **Task Name** (dropdown, filtered by sub category)
4. **Year** (number, e.g. 2025)
5. **Period** (dropdown): JAN–DEC, Q1–Q4, or ANNUALLY
6. **Task Details** (the actual task fields)

### Task Title
Computed, not stored: `"Category > Sub Category > Task Name > Year > Period"`

### Task Fields
- Client (dropdown — active/handed-off clients only)
- Assigned accountants (multi-select — filtered by selected client's accountants)
- Description (text, optional)
- Deadline (date/time)
- Working files: list of items, each either:
  - `{ type: "file", fileId: UUID, fileName: string }` — uploaded file
  - `{ type: "link", url: string, label: string }` — external URL
- Output file (single PDF, optional)
- Proof of filing file (single PDF, optional)

### Task Status Pipeline
```
OPEN → SUBMITTED → REJECTED (cycle back, resubmit)
                 → APPROVED_FOR_FILING (accountant files with BIR)
                   → FILED (accountant uploads proof of filing)
                     → COMPLETED (files merged into tax record entries)
```

| Status | Who Acts | What Happens |
|--------|----------|-------------|
| OPEN | Accountant | Works on the task, uploads working files |
| SUBMITTED | QTD | Reviews the work |
| REJECTED | Accountant | Revises based on QTD comments, resubmits |
| APPROVED_FOR_FILING | Accountant | Files with BIR externally |
| FILED | Accountant | Uploads proof of filing/payment |
| COMPLETED | System | Files are merged into the client's permanent tax record entries |

### Tax Record Entries
When a task reaches COMPLETED, its files merge into the **tax record entries** table — one row per unique combination of client + category + sub category + task name + year + period. If an entry already exists, working files are appended and output/proof files are updated.

These entries represent the client's permanent, finalized tax records.

---

## Client Dashboard (Client Role)

The client sees:
- **Reminders** — text notes with deadlines/filing info (posted by QTD)
- **Pending Documents** — notes on what's still needed (posted by CSD)
- **Highlights** — important updates (posted by CSD)
- **Outstanding Billing** — TBD
- **Finalized Tax Records** — view/download only

The client **cannot** upload, edit, create, or interact with anything beyond viewing.

---

## Notifications

| Type | Triggered When |
|------|---------------|
| TASK_ASSIGNED | Task created and assigned to accountant |
| TASK_SUBMITTED | Accountant submits task for review |
| TASK_APPROVED | QTD approves the task |
| TASK_REJECTED | QTD rejects the task |
| TASK_FILED | Accountant marks as filed |
| TASK_COMPLETED | Task is completed |
| CLIENT_HANDOFF | Client is handed off from OOS to accountants |

Both in-app and email notifications.

---

## List Views

### Client Lists
- **QTD/Manager table**: All clients (except during onboarding draft) — columns: Client Name, Total Tasks, Pending Tasks, Overdue Tasks, Nearest Deadline, Status
- **Accountant table**: Only clients assigned to the logged-in accountant — same columns, but task counts scoped to that accountant
- **OOS onboarding list**: Clients created by the logged-in OOS user

Clicking a client row routes to the appropriate page based on status (onboarding form vs active client view).

### Task Lists
- **QTD/Manager table**: All tasks — columns: Title, Task Type, Client, Assigned To, Status, Deadline, Overdue flag, Created Date
- **Accountant table**: Only the logged-in accountant's tasks — same columns minus "Assigned To"

### Overdue Logic
When a task's deadline passes without submission, it is flagged as overdue. The task can still be submitted — it's a visual indicator, not a blocker.

---

## File Handling

- Files are uploaded and stored with a UUID reference
- Within JSONB structures, files are referenced as `FileReference { id: UUID, name: string }`
- File preview/download endpoint: `GET /api/v1/files/{fileId}/preview`
- Links within JSONB are `LinkReference { url: string, label: string }` — frontend opens the URL directly

---

## Rich Text Fields

- Stored as **TipTap/ProseMirror JSON document format**
- The backend is completely agnostic — it stores and returns the JSON blob as-is
- The **frontend owns** the editor schema, rendering, and all formatting capabilities
- Rich text supports: bold, italic, bullet lists, links, images, attachments, etc. (whatever TipTap supports)
- Description/instruction text on forms (like helper text, section descriptions) is for frontend display only — not stored as data

---

## Form Behavior Notes

- All description/instruction text on forms is purely for **frontend display** — the backend does not store or serve display-only text
- **Computed fields**: backend is source of truth, but frontend should compute for instant UI feedback (e.g., consultation billing, taxpayer category)
- **List sections** (BIR branches, city hall entries, officers, credentials, etc.) start with 1 empty entry by default — user can add/remove entries
- The client info form structure is **constant across all clients** — same fields for everyone. Adding new fields requires a code change.
- **DateField**: Every date input throughout the form uses `{ date, isImportant }` — the `isImportant` boolean allows users to flag critical dates
