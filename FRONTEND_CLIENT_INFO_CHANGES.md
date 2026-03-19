# Client Info API Changes — Backend Restructuring

## Summary
The backend has been restructured from **16 JSONB columns** down to **7**. Sub-sections that were previously sent as separate top-level fields in the API response are now nested inside wrapper objects. Several field types have also changed.

## What Changed

### API Response Structure: Before vs After

**BEFORE** — `GET /api/v1/clients/{id}/info` returned 16 top-level fields:
```json
{
  "mainDetails": { ... },
  "birDetails": [ ... ],
  "birTaxCompliance": { ... },
  "birComplianceBreakdown": { ... },
  "dtiDetails": { ... },
  "secDetails": { ... },
  "sssDetails": { ... },
  "philhealthDetails": { ... },
  "hdmfDetails": { ... },
  "cityHallDetails": [ ... ],
  "corporateOfficers": [ ... ],
  "pointOfContactDetails": { ... },
  "accessCredentials": [ ... ],
  "scopeOfEngagement": { ... },
  "professionalFees": [ ... ],
  "onboardingDetails": { ... }
}
```

**AFTER** — 7 top-level fields:
```json
{
  "mainDetails": { ... },
  "clientInformation": { ... },
  "corporateOfficerInformation": { ... },
  "accessCredentials": [ ... ],
  "scopeOfEngagement": { ... },
  "professionalFees": [ ... ],
  "onboardingDetails": { ... }
}
```

---

## Detailed Changes Per Section

### 1. `mainDetails` — CHANGED (fields removed)

**Before:**
```json
{
  "registeredName": "string",
  "tradeName": "string",
  "numberOfBranches": 0,
  "organizationType": "SOLE_PROPRIETORSHIP",
  "mreCode": "string",
  "commencementOfWork": "2024-01-01",
  "engagementStatus": "ACTIVE"
}
```

**After (GET response):**
```json
{
  "mreCode": "string",
  "commencementOfWork": { "date": "2024-01-01", "isImportant": false },
  "engagementStatus": "ACTIVE"
}
```

**PATCH request body** — includes accountant assignment alongside main details fields:
```json
{
  "mreCode": "string",
  "commencementOfWork": { "date": "2024-01-01", "isImportant": false },
  "engagementStatus": "ACTIVE",
  "csdOosAccountantIds": ["uuid1", "uuid2"],
  "qtdAccountantId": "uuid3"
}
```

- `registeredName`, `tradeName`, `numberOfBranches`, `organizationType` **moved** to `clientInformation` (see below)
- `commencementOfWork` changed from `LocalDate` string to `DateField` object
- **`csdOosAccountantIds`** — list of UUIDs for CSD/OOS accountants to assign. Use the accountant listing endpoint to populate the dropdown. Can be `null` or empty to clear assignments.
- **`qtdAccountantId`** — single UUID for QTD accountant. Can be `null` to clear assignment.
- Accountant IDs are stored in the `client_accountants` join table (not in JSONB). The assigned accountants appear in the response header fields (`assignedCsdOosAccountants`, `assignedQtdAccountants`).

### 2. `clientInformation` — NEW (replaces 9 separate fields)

This single object now contains everything that was previously spread across `birDetails`, `birTaxCompliance`, `birComplianceBreakdown`, `dtiDetails`, `secDetails`, `sssDetails`, `philhealthDetails`, `hdmfDetails`, `cityHallDetails`, plus the header fields that moved from `mainDetails`.

```json
{
  "registeredName": "string",
  "tradeName": "string",
  "numberOfBranches": 0,
  "organizationType": "SOLE_PROPRIETORSHIP",

  "birMainBranch": { /* BirBranchDetails — always present, head office */ },
  "birBranches": [ { /* BirBranchDetails — additional branches */ } ],
  "birTaxCompliance": { /* BirTaxComplianceDetails */ },
  "birComplianceBreakdown": { /* same BirComplianceBreakdown structure */ },
  "dtiDetails": { /* same DtiDetails structure */ },
  "secDetails": { /* same SecDetails structure */ },
  "sssDetails": { /* same GovernmentAgencyDetails structure */ },
  "philhealthDetails": { /* same GovernmentAgencyDetails structure */ },
  "hdmfDetails": { /* same GovernmentAgencyDetails structure */ },
  "cityHallDetails": [ { /* same CityHallDetails structure */ } ]
}
```

#### BIR Branch Split: `birDetails` → `birMainBranch` + `birBranches`

**Before:**
```json
"birDetails": [ { /* single flat list of all branches */ } ]
```

**After:**
```json
"birMainBranch": { /* BirBranchDetails — always present, represents head office */ },
"birBranches": [ { /* BirBranchDetails — additional branches */ } ]
```

- `birMainBranch` is always present (one object, not a list) — represents the head office
- `birBranches` is a list of additional branches — its size is driven by the `numberOfBranches` integer input
- Frontend should render: main branch form (always visible) + N additional branch forms based on `numberOfBranches` input value
- When user changes `numberOfBranches`, frontend should sync the `birBranches` list size accordingly

#### BIR Tax Compliance Changes

**Before:**
```json
{
  "grossSales": [...],
  "taxpayerClassification": null,
  "topWithholding": "Yes",
  "dateClassifiedTopWithholding": "2024-01-01",
  "incomeTaxRegime": "string"
}
```

**After:**
```json
{
  "grossSales": [...],
  "taxpayerClassification": "MICRO",
  "topWithholding": true,
  "dateClassifiedTopWithholding": { "date": "2024-01-01", "isImportant": false },
  "incomeTaxRegime": "string"
}
```

- `topWithholding` changed from `String` → `boolean` (render as yes/no dropdown, send `true`/`false`)
- `dateClassifiedTopWithholding` is now `DateField` (see DateField section below)
- **`taxpayerClassification`** — **computed by backend, read-only on frontend**. Display it after the gross sales table but do NOT let users edit it. It is computed from the most recent year's gross sales amount:
  - `"MICRO"` — gross sales < 3,000,000
  - `"SMALL"` — 3,000,000 to < 20,000,000
  - `"MEDIUM"` — 20,000,000 to < 1,000,000,000
  - `"LARGE"` — >= 1,000,000,000
  - `null` — if no gross sales entries exist
- Frontend can compute it locally for instant UI feedback, but backend is the source of truth.

### 3. `corporateOfficerInformation` — NEW (replaces 2 separate fields)

Wraps `corporateOfficers` and `pointOfContactDetails` into one object.

**Before** (two separate top-level fields):
```json
{
  "corporateOfficers": [ { ... } ],
  "pointOfContactDetails": { ... }
}
```

**After** (single nested object):
```json
{
  "corporateOfficerInformation": {
    "officers": [ { /* same CorporateOfficerDetails structure */ } ],
    "pointOfContact": { /* same PointOfContactDetails structure */ }
  }
}
```

Note the field name changes:
- `corporateOfficers` → `corporateOfficerInformation.officers`
- `pointOfContactDetails` → `corporateOfficerInformation.pointOfContact`

### 4. `scopeOfEngagement` — CHANGED (field type changes)

#### `bookOfAccounts` — String → Enum

**Before:**
```json
"bookOfAccounts": "Manual"
```

**After:**
```json
"bookOfAccounts": "MANUAL"
```

Valid values: `"MANUAL"`, `"LOOSE_LEAF"`, `"CAS"` — render as a dropdown.

#### `consultationHours.consultations[]` — New fields: `amount` and `vat`

**Before:**
```json
{
  "date": { "date": "2024-01-01", "isImportant": false },
  "timeStarted": "06:14 AM",
  "timeEnded": "09:14 AM",
  "topicsAndDocumentation": { /* rich text */ },
  "numberOfHours": 3,
  "platform": "Zoom"
}
```

**After:**
```json
{
  "date": { "date": "2024-01-01", "isImportant": false },
  "timeStarted": "06:14 AM",
  "timeEnded": "09:14 AM",
  "topicsAndDocumentation": { /* rich text */ },
  "numberOfHours": 3,
  "platform": "Zoom",
  "amount": 1500,
  "vat": 180.00
}
```

- `amount` (int) — overridable amount for the consultation
- `vat` (BigDecimal) — VAT amount
- Both should be rendered as editable inputs in each consultation entry

### 5. `onboardingDetails` — UNCHANGED (but frontend missing fields)

The backend structure is unchanged, but note that each meeting entry includes:
- `minutes` (JsonNode — rich text, TipTap/ProseMirror format) — **frontend must render a rich text editor for this field**
- `agenda` (String) — plain text field

The backend already sends these fields. Frontend needs to render them.

### 6-7. Other unchanged sections
- `accessCredentials` — same structure
- `professionalFees` — same structure

---

## DateField Change — ALL Date Fields

**Every single date field** across the entire client info structure has changed from a plain date string to a `DateField` object:

**Before:**
```json
"dateOfBirRegistration": "2024-01-15"
```

**After:**
```json
"dateOfBirRegistration": { "date": "2024-01-15", "isImportant": false }
```

### Affected fields (all now `DateField`):
- `mainDetails.commencementOfWork`
- `clientInformation.birMainBranch.dateOfBirRegistration`
- `clientInformation.birBranches[].dateOfBirRegistration`
- `clientInformation.birTaxCompliance.dateClassifiedTopWithholding`
- `clientInformation.dtiDetails.dtiDateOfRegistration`
- `clientInformation.dtiDetails.dtiDateOfExpiration`
- `clientInformation.dtiDetails.bmbeDateOfRegistration`
- `clientInformation.dtiDetails.bmbeDateOfExpiration`
- `clientInformation.secDetails.dateOfIncorporation`
- `clientInformation.secDetails.dateOfActualMeetingPerBylaws`
- `clientInformation.sssDetails.dateOfRegistration`
- `clientInformation.philhealthDetails.dateOfRegistration`
- `clientInformation.hdmfDetails.dateOfRegistration`
- `clientInformation.cityHallDetails[].dateOfRegistration`
- `clientInformation.cityHallDetails[].quarterlyDeadlineQ2`
- `clientInformation.cityHallDetails[].quarterlyDeadlineQ3`
- `clientInformation.cityHallDetails[].quarterlyDeadlineQ4`
- `clientInformation.cityHallDetails[].permitExpirationDate`
- `clientInformation.cityHallDetails[].firePermit.expirationDate`
- `clientInformation.cityHallDetails[].sanitaryPermit.expirationDate`
- `clientInformation.cityHallDetails[].otherPermit.expirationDate`
- `corporateOfficerInformation.officers[].birthday`
- `scopeOfEngagement.dateOfEngagementLetter`
- `scopeOfEngagement.consultationHours.consultations[].date`
- `onboardingDetails.gcCreatedDate`
- `onboardingDetails.meetings[].date`

---

## PATCH Endpoint — Section Keys Changed

`PATCH /api/v1/clients/{clientId}/info/{sectionKey}`

**Before** — 16 valid section keys:
`mainDetails`, `birDetails`, `birTaxCompliance`, `birComplianceBreakdown`, `dtiDetails`, `secDetails`, `sssDetails`, `philhealthDetails`, `hdmfDetails`, `cityHallDetails`, `corporateOfficers`, `pointOfContactDetails`, `accessCredentials`, `scopeOfEngagement`, `professionalFees`, `onboardingDetails`

**After** — 7 valid section keys:
`mainDetails`, `clientInformation`, `corporateOfficerInformation`, `accessCredentials`, `scopeOfEngagement`, `professionalFees`, `onboardingDetails`

When saving a section, send the **entire section object** as the request body. For example, to update client information, send the full `clientInformation` object (with all sub-sections).

---

## NEW: Client Info Response — Computed Header Fields

`GET /api/v1/clients/{clientId}/info` now returns **5 computed header fields** alongside the 7 JSONB sections:

```json
{
  "clientDisplayName": "Trade Name (Registered Name)",
  "taxpayerClassification": "MICRO",
  "clientStatus": "ONBOARDING",
  "assignedCsdOosAccountants": [ { "id": "...", "displayName": "CPA John Doe, CPA", "position": "Sr Accountant I", "role": "Client Service Delivery", "roleKey": "CSD" } ],
  "assignedQtdAccountants": [ { "id": "...", "displayName": "Jane Smith", "position": "Jr Accountant III", "role": "Quality Training & Development", "roleKey": "QTD" } ],

  "mainDetails": { ... },
  "clientInformation": { ... },
  "corporateOfficerInformation": { ... },
  "accessCredentials": [ ... ],
  "scopeOfEngagement": { ... },
  "professionalFees": [ ... ],
  "onboardingDetails": { ... }
}
```

### Header field details:
- **`clientDisplayName`** — computed by backend: `"tradeName (registeredName)"`, or just `tradeName` / `registeredName` if only one exists. `null` during initial creation.
- **`taxpayerClassification`** — computed from `clientInformation.birTaxCompliance.taxpayerClassification`. One of `"MICRO"`, `"SMALL"`, `"MEDIUM"`, `"LARGE"`, or `null`. Read-only.
- **`clientStatus`** — one of `"ONBOARDING"`, `"APPROVED"`, `"ACTIVE"`.
- **`assignedCsdOosAccountants`** — list of assigned CSD/OOS accountants. Populated when OOS saves main details with accountant selections.
- **`assignedQtdAccountants`** — list of assigned QTD accountants. Populated when OOS saves main details with QTD selection.

Frontend should display these header fields at the top of the client info page. The accountant lists are read-only in the header display, but are editable via the main details PATCH (see below).

---

## NEW: Accountant Listing Endpoint

`GET /api/v1/users/accountants?roleKey=CSD,OOS`

Returns a list of active accountants filtered by role keys. Accepts comma-separated `roleKey` values.

**Example calls:**
- `GET /api/v1/users/accountants?roleKey=CSD,OOS` — for CSD/OOS dropdown
- `GET /api/v1/users/accountants?roleKey=QTD` — for QTD dropdown

**Response:**
```json
[
  {
    "id": "uuid",
    "displayName": "CPA John Doe, CPA",
    "position": "Sr Accountant I",
    "role": "Client Service Delivery",
    "roleKey": "CSD"
  }
]
```

- `displayName` includes user title prefixes/suffixes (e.g., "CPA Jia Alyssa Acol, CPA, CTT")
- Only returns users with `ACTIVE` status
- Requires `client.assign` permission

---

## NEW: Handoff Endpoint

`POST /api/v1/clients/{clientId}/handoff`

Transitions the client from `APPROVED` → `ACTIVE`. No request body needed — accountants must already be assigned via the main details PATCH.

**Validation:**
- Client must be in `APPROVED` status (returns 400 otherwise)
- Accountants must already be assigned (returns 400 if none assigned)

**Response:** `204 No Content`

Requires `client.assign` permission.

### Frontend handoff flow:
1. During onboarding, OOS selects CSD/OOS and QTD accountants in the main details section (saved via PATCH)
2. After client info is approved (ONBOARDING → APPROVED), a "Handoff" button appears
3. Click handoff → POST to handoff endpoint (no body)
4. On success, client status changes to ACTIVE

---

## PATCH Endpoint — Section Keys Changed

`PATCH /api/v1/clients/{clientId}/info/{sectionKey}`

**Before** — 16 valid section keys:
`mainDetails`, `birDetails`, `birTaxCompliance`, `birComplianceBreakdown`, `dtiDetails`, `secDetails`, `sssDetails`, `philhealthDetails`, `hdmfDetails`, `cityHallDetails`, `corporateOfficers`, `pointOfContactDetails`, `accessCredentials`, `scopeOfEngagement`, `professionalFees`, `onboardingDetails`

**After** — 7 valid section keys:
`mainDetails`, `clientInformation`, `corporateOfficerInformation`, `accessCredentials`, `scopeOfEngagement`, `professionalFees`, `onboardingDetails`

When saving a section, send the **entire section object** as the request body. For example, to update client information, send the full `clientInformation` object (with all sub-sections).

---

## Migration Checklist for Frontend

1. **Update API response types** — replace 16-field response with 7-field response + 5 computed header fields
2. **Update field access paths**:
   - `response.birDetails` → split into `response.clientInformation.birMainBranch` (single object) + `response.clientInformation.birBranches` (list)
   - `response.birTaxCompliance` → `response.clientInformation.birTaxCompliance`
   - `response.birComplianceBreakdown` → `response.clientInformation.birComplianceBreakdown`
   - `response.dtiDetails` → `response.clientInformation.dtiDetails`
   - `response.secDetails` → `response.clientInformation.secDetails`
   - `response.sssDetails` → `response.clientInformation.sssDetails`
   - `response.philhealthDetails` → `response.clientInformation.philhealthDetails`
   - `response.hdmfDetails` → `response.clientInformation.hdmfDetails`
   - `response.cityHallDetails` → `response.clientInformation.cityHallDetails`
   - `response.mainDetails.registeredName` → `response.clientInformation.registeredName`
   - `response.mainDetails.tradeName` → `response.clientInformation.tradeName`
   - `response.mainDetails.numberOfBranches` → `response.clientInformation.numberOfBranches`
   - `response.mainDetails.organizationType` → `response.clientInformation.organizationType`
   - `response.corporateOfficers` → `response.corporateOfficerInformation.officers`
   - `response.pointOfContactDetails` → `response.corporateOfficerInformation.pointOfContact`
3. **Update all date field handling** — every date is now `{ date, isImportant }` instead of a plain string
4. **Update PATCH calls** — use the 7 new section keys instead of 16
5. **Section labels** — frontend owns all section/sub-section labels (backend stores pure data only)
6. **BIR branch rendering** — render `birMainBranch` as always-visible head office form + `birBranches` as dynamic list driven by `numberOfBranches` input
7. **topWithholding** — changed from string to boolean, render as yes/no dropdown, send `true`/`false`
8. **bookOfAccounts** — changed from free string to enum: `"MANUAL"`, `"LOOSE_LEAF"`, `"CAS"`, render as dropdown
9. **Consultation entries** — add `amount` (int, overridable) and `vat` (decimal) input fields to each consultation
10. **Meeting minutes** — render a rich text editor for `onboardingDetails.meetings[].minutes` (JsonNode, TipTap format) — backend already supports this field
11. **Client info page header** — display `clientDisplayName`, `taxpayerClassification`, `clientStatus`, and assigned accountants from the response header fields
12. **Accountant dropdowns in main details** — use `GET /api/v1/users/accountants?roleKey=CSD,OOS` and `?roleKey=QTD` to populate dropdowns in the main details section. Include selected accountant IDs when PATCHing mainDetails (`csdOosAccountantIds`, `qtdAccountantId`).
13. **Handoff button** — POST to `/api/v1/clients/{clientId}/handoff` (no body). Only available when client is APPROVED and accountants are assigned.
