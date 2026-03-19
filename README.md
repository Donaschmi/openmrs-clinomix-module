# ClinomX Backend Module

An OpenMRS module that provides REST APIs and FHIR R4-compliant business logic for managing **Questionnaires** and **QuestionnaireResponses**. Built on the OpenMRS OMOD architecture with HAPI FHIR as the parsing/serialisation engine.

---

## Table of Contents

- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [REST API Reference](#rest-api-reference)
  - [Questionnaire](#questionnaire)
  - [QuestionnaireResponse](#questionnaireresponse)
  - [Module Info](#module-info)
- [Service Layer](#service-layer)
- [FHIR Compliance](#fhir-compliance)
- [Security & Privileges](#security--privileges)
- [Configuration](#configuration)
- [Building](#building)
- [Testing](#testing)

---

## Architecture

The module follows a four-layer architecture:

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────────┐
│           REST Layer  (omod)            │
│  QuestionnaireRestResource              │
│  QuestionnaireResponseRestResource      │
│  ClinomXResource                        │
└────────────────┬────────────────────────┘
                 │ delegates to
                 ▼
┌─────────────────────────────────────────┐
│        Service Layer  (api/impl)        │
│  FhirQuestionnaireServiceImpl           │  ← FHIR parsing, versioning, dedup guard
│  FhirQuestionnaireResponseServiceImpl   │  ← reference resolution, patient linkage
└────────────────┬────────────────────────┘
                 │ calls
                 ▼
┌─────────────────────────────────────────┐
│         DAO Layer  (api/dao)            │
│  ClinomXDaoImpl                         │  ← native SQL, no Hibernate mapping needed
└────────────────┬────────────────────────┘
                 │ reads/writes
                 ▼
┌─────────────────────────────────────────┐
│          MySQL / MariaDB                │
│  clinom_x_questionnaire                 │
│  clinom_x_questionnaire_response        │
└─────────────────────────────────────────┘
```

**Key design choices:**

| Choice | Reason |
|---|---|
| Native SQL (no Hibernate entity mapping) | Avoids HBM XML overhead; full control over schema |
| Full FHIR JSON blob stored alongside scalar columns | Scalar columns enable efficient SQL filtering; the JSON blob preserves all FHIR fields |
| HAPI FHIR used as a library only | No FHIR server infrastructure required; parse/serialise on the fly |
| `url` + `version` as canonical identity | FHIR R4 canonical resource uniqueness rule |

---

## Project Structure

```
backend/
├── pom.xml                              — Parent POM (multi-module Maven project)
│
├── api/                                 — Core module (no web dependencies)
│   ├── pom.xml
│   └── src/main/java/org/openmrs/module/clinomx/
│       ├── ClinomXActivator.java        — Module lifecycle (startup, shutdown, seeding)
│       ├── ClinomXConstants.java        — Shared constants placeholder
│       ├── ClinomXPrivileges.java       — Privilege name constants
│       ├── api/
│       │   ├── ClinomXService.java                      — Module info service (interface)
│       │   ├── FhirQuestionnaireService.java             — Questionnaire CRUD + search (interface)
│       │   ├── FhirQuestionnaireResponseService.java     — Response CRUD + search (interface)
│       │   └── impl/
│       │       ├── ClinomXServiceImpl.java
│       │       ├── FhirQuestionnaireServiceImpl.java
│       │       └── FhirQuestionnaireResponseServiceImpl.java
│       ├── dao/
│       │   ├── ClinomXDao.java          — DAO interface
│       │   └── impl/
│       │       └── ClinomXDaoImpl.java  — Native SQL implementation
│       ├── model/
│       │   ├── QuestionnaireRecord.java         — DB row POJO
│       │   └── QuestionnaireResponseRecord.java — DB row POJO
│       └── resources/
│           └── moduleApplicationContext.xml     — Spring bean definitions
│
├── omod/                                — Web module (REST resources, packaged as .omod)
│   ├── pom.xml
│   └── src/main/
│       ├── java/org/openmrs/module/clinomx/web/resource/
│       │   ├── QuestionnaireDelegate.java          — REST DTO
│       │   ├── QuestionnaireRestResource.java       — REST resource
│       │   ├── QuestionnaireResponseDelegate.java  — REST DTO
│       │   ├── QuestionnaireResponseRestResource.java
│       │   ├── ClinomXModuleInfo.java
│       │   └── ClinomXResource.java
│       └── resources/
│           ├── config.xml               — Module descriptor (id, privileges, dependencies)
│           ├── liquibase.xml            — Database schema migrations
│           └── moduleApplicationContext.xml
│
└── modules/                             — Compiled .omod output (git-ignored)
```

---

## Database Schema

Migrations are managed by **Liquibase** (`omod/src/main/resources/liquibase.xml`).

### `clinom_x_questionnaire`

| Column | Type | Notes |
|---|---|---|
| `questionnaire_id` | `INT` PK AI | Surrogate key |
| `uuid` | `CHAR(38)` UNIQUE | OpenMRS UUID; exposed as the REST/FHIR resource `id` |
| `fhir_id` | `VARCHAR(255)` UNIQUE | Internal FHIR resource ID |
| `url` | `VARCHAR(512)` | FHIR canonical URL — stable across all versions of the same questionnaire |
| `name` | `VARCHAR(255)` | Machine-readable label (FHIR `name` search parameter) |
| `title` | `VARCHAR(255)` | Human-readable title (FHIR `title` search parameter) |
| `status` | `VARCHAR(20)` | FHIR publication status (`draft`, `active`, `retired`, `unknown`) |
| `version` | `VARCHAR(50)` | Semantic version string (e.g. `1.2.0`) |
| `fhir_json` | `LONGTEXT` | Complete FHIR R4 JSON representation |
| `date_created` | `DATETIME` | |
| `date_changed` | `DATETIME` | |

**Indexes:** `idx_clinom_x_questionnaire_url` on `url`, `idx_clinom_x_questionnaire_name` on `name`.

**Uniqueness constraint (enforced in service layer):** the combination of `url` + `version` must be unique across all rows. A `null` version is treated as a distinct value (a questionnaire with no version and one with `version = NULL` are the same; see FHIR R4 §11.16).

### `clinom_x_questionnaire_response`

| Column | Type | Notes |
|---|---|---|
| `response_id` | `INT` PK AI | Surrogate key |
| `uuid` | `CHAR(38)` UNIQUE | OpenMRS UUID; exposed as the REST/FHIR resource `id` |
| `fhir_id` | `VARCHAR(255)` UNIQUE | Internal FHIR resource ID |
| `questionnaire_id` | `INT` FK | References `clinom_x_questionnaire.questionnaire_id` |
| `patient_uuid` | `CHAR(38)` | Patient UUID extracted from `subject` for efficient filtering |
| `status` | `VARCHAR(20)` | FHIR response status (`in-progress`, `completed`, `amended`, …) |
| `authored` | `DATETIME` | When the response was filled out |
| `fhir_json` | `LONGTEXT` | Complete FHIR R4 JSON representation |
| `date_created` | `DATETIME` | |

**Indexes:** `idx_clinom_x_response_questionnaire_id` on `questionnaire_id`, `idx_clinom_x_response_patient_uuid` on `patient_uuid`.

---

## REST API Reference

All endpoints are under the OpenMRS REST base path: `/ws/rest/v1/`

Authentication uses HTTP Basic auth (OpenMRS username / password).

### Questionnaire

**Base path:** `/ws/rest/v1/questionnaire`

#### List all questionnaires

```http
GET /ws/rest/v1/questionnaire
```

Response (default representation):
```json
{
  "results": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440000",
      "url": "https://example.org/fhir/Questionnaire/PHQ9",
      "name": "PHQ9",
      "title": "PHQ-9 Depression Screening",
      "status": "active",
      "version": "1.0.0",
      "description": null,
      "date": null,
      "publisher": null
    }
  ]
}
```

Append `&v=full` to include the complete `json` field (raw FHIR JSON).

#### Search questionnaires

```http
GET /ws/rest/v1/questionnaire?title=PHQ          — case-insensitive substring match on title
GET /ws/rest/v1/questionnaire?name=PHQ9          — case-insensitive substring match on name
GET /ws/rest/v1/questionnaire?url=https://...    — exact match on canonical URL (returns all versions, sorted by semver)
GET /ws/rest/v1/questionnaire?url=https://...&versions=true  — explicit FHIR $versions equivalent
```

When `url` (with or without `versions=true`) is provided, results are ordered **oldest → newest** by semantic version (numeric major.minor.patch comparison, nulls first).

#### Get a single questionnaire

```http
GET /ws/rest/v1/questionnaire/{uuid}
```

#### Create a questionnaire

```http
POST /ws/rest/v1/questionnaire
Content-Type: application/json

{
  "json": "{\"resourceType\":\"Questionnaire\",\"url\":\"https://example.org/fhir/Questionnaire/PHQ9\",\"name\":\"PHQ9\",\"title\":\"PHQ-9 Depression Screening\",\"status\":\"active\",\"version\":\"1.0.0\",\"item\":[{\"linkId\":\"q1\",\"text\":\"Little interest or pleasure in doing things?\",\"type\":\"choice\"}]}"
}
```

- The `json` field must contain a valid FHIR R4 `Questionnaire` JSON string.
- Returns `HTTP 201 Created` with the created resource.
- Returns `HTTP 400 Bad Request` if the `url` + `version` combination already exists in the database.

#### Update a questionnaire

```http
POST /ws/rest/v1/questionnaire/{uuid}
Content-Type: application/json

{
  "json": "{ ...updated FHIR JSON... }"
}
```

- The resource's FHIR id is preserved from the original record.
- Updating to a `url` + `version` that belongs to a different questionnaire is rejected.
- Updating a questionnaire to the same `url` + `version` it already has is allowed (in-place edit).

#### Delete a questionnaire

```http
DELETE /ws/rest/v1/questionnaire/{uuid}
```

---

### QuestionnaireResponse

**Base path:** `/ws/rest/v1/questionnaireresponse`

#### List all responses

```http
GET /ws/rest/v1/questionnaireresponse
```

Response (default representation):
```json
{
  "results": [
    {
      "uuid": "660e8400-e29b-41d4-a716-446655440001",
      "status": "completed",
      "questionnaire": "Questionnaire/550e8400-e29b-41d4-a716-446655440000",
      "subject": "Patient/patient-uuid-here",
      "authored": "2024-03-15T10:30:00+00:00"
    }
  ]
}
```

Append `&v=full` to include the complete `json` field.

#### Search responses

```http
GET /ws/rest/v1/questionnaireresponse?questionnaire={questionnaire-uuid}
GET /ws/rest/v1/questionnaireresponse?patient={patient-uuid}
GET /ws/rest/v1/questionnaireresponse?questionnaire={questionnaire-uuid}&patient={patient-uuid}
```

All three search parameters use the OpenMRS UUID (not the FHIR resource id).

#### Get a single response

```http
GET /ws/rest/v1/questionnaireresponse/{uuid}
```

#### Create a response

```http
POST /ws/rest/v1/questionnaireresponse
Content-Type: application/json

{
  "json": "{\"resourceType\":\"QuestionnaireResponse\",\"questionnaire\":\"Questionnaire/{questionnaire-uuid}\",\"status\":\"completed\",\"subject\":{\"reference\":\"Patient/{patient-uuid}\"},\"authored\":\"2024-03-15T10:30:00+00:00\",\"item\":[{\"linkId\":\"q1\",\"answer\":[{\"valueCoding\":{\"code\":\"2\"}}]}]}"
}
```

- The `questionnaire` reference must be `"Questionnaire/{uuid}"` where the UUID is the OpenMRS UUID of an existing questionnaire.
- The `subject.reference` must be `"Patient/{uuid}"` where the UUID is an OpenMRS patient UUID.
- Both references are resolved to database IDs at save time.

#### Update a response

```http
POST /ws/rest/v1/questionnaireresponse/{uuid}
Content-Type: application/json

{
  "json": "{ ...updated FHIR JSON... }"
}
```

#### Delete a response

```http
DELETE /ws/rest/v1/questionnaireresponse/{uuid}
```

---

### Module Info

**Base path:** `/ws/rest/v1/clinom-x-module-info`

```http
GET /ws/rest/v1/clinom-x-module-info/info
```

Returns the module version string. Read-only; write operations return `405 Method Not Allowed`.

---

## Service Layer

### `FhirQuestionnaireService`

| Method | Description |
|---|---|
| `getAllQuestionnaires()` | Returns all questionnaires, invalid JSON records silently skipped |
| `searchQuestionnairesByTitle(title)` | Case-insensitive substring match |
| `searchQuestionnairesByName(name)` | Case-insensitive substring match on machine-readable name |
| `getVersionsByUrl(url)` | All versions of the canonical URL, sorted by semver ascending |
| `getQuestionnaireByUuid(uuid)` | Single lookup by OpenMRS UUID |
| `createQuestionnaire(q)` | Validates uniqueness, auto-generates id if missing, persists |
| `updateQuestionnaire(uuid, q)` | Validates uniqueness (excluding self), updates all fields |
| `deleteQuestionnaire(uuid)` | No-op if not found |

**Duplicate guard (`assertNoDuplicate`):** before any write, the service checks whether a different record already owns the same `url` + `version` pair. If so, it throws `APIException`. Passing the current record's UUID as `excludeUuid` prevents false conflicts when saving a questionnaire in-place.

**Semantic version ordering (`SEMVER_ORDER`):** versions are parsed as `[major, minor, patch]` integer arrays and compared numerically, so `1.10.0 > 1.9.0`. `null` and empty strings sort as `0.0.0` (before all real versions). Non-numeric parts (e.g. `-SNAPSHOT`) are stripped before parsing.

### `FhirQuestionnaireResponseService`

| Method | Description |
|---|---|
| `getAllQuestionnaireResponses()` | Returns all responses |
| `getQuestionnaireResponseByUuid(uuid)` | Single lookup |
| `getResponsesByQuestionnaire(questionnaireUuid)` | Filtered by questionnaire |
| `getResponsesByPatient(patientUuid)` | Filtered by patient |
| `getResponsesByQuestionnaireAndPatient(qUuid, pUuid)` | Filtered by both |
| `createQuestionnaireResponse(r)` | Resolves references, persists |
| `updateQuestionnaireResponse(uuid, r)` | Updates fields; restores FHIR id from original record |
| `deleteQuestionnaireResponse(uuid)` | No-op if not found |

**Reference resolution at write time:**
- `response.questionnaire` (`"Questionnaire/{uuid}"`) → resolved to the questionnaire's database integer id and stored in the `questionnaire_id` FK column.
- `response.subject.reference` (`"Patient/{uuid}"`) → patient UUID extracted and stored in `patient_uuid` column.

Both columns allow efficient SQL `WHERE` filtering without scanning JSON blobs.

---

## FHIR Compliance

| Rule | Implementation |
|---|---|
| **Canonical URL uniqueness** (`url` + `version` must be unique) | `assertNoDuplicate()` guard in `FhirQuestionnaireServiceImpl` |
| **`status` is required (1..1)** | Always extracted from the FHIR object; `null` is stored as-is and validated in tests |
| **`item.linkId` is required (1..1)** | Validated in FHIR compliance tests; stored in JSON; not enforced server-side (FHIR validation is the caller's responsibility) |
| **`$versions` operation** | `getVersionsByUrl()` returns all versions of a canonical resource sorted by semver |
| **Resource identity** | The OpenMRS UUID is set as the FHIR `id` on every returned object; internal FHIR ids are only used at write time |
| **Reference format** | `Questionnaire/{uuid}` and `Patient/{uuid}` literal references are preserved through the JSON round-trip |
| **JSON round-trip fidelity** | Full FHIR JSON blob is stored and re-parsed on every read; no data is reconstructed from scalar columns |

---

## Security & Privileges

Two privileges control access, defined in `config.xml` and enforced via `@Authorized` annotations on service methods:

| Privilege | Constant | Grants |
|---|---|---|
| `Get ClinomX Data` | `ClinomXPrivileges.GET_CLINOM_X_DATA` | Read access (GET) |
| `Manage ClinomX Data` | `ClinomXPrivileges.MANAGE_CLINOM_X_DATA` | Write access (POST, DELETE) |

Assign these privileges to the appropriate OpenMRS roles in the administration panel.

---

## Configuration

### Module global properties

| Property | Default | Description |
|---|---|---|
| `clinom-x.defaultLocale` | `en` | Default locale for display strings |

### Required module dependencies

| Module | Minimum version |
|---|---|
| `org.openmrs.module.webservices.rest` | 2.30.0 |
| `org.openmrs.module.fhir2` | 2.2.0 |

**OpenMRS core** version requirement: `2.6.*` – `9.*`

### Spring beans (`api/src/main/resources/moduleApplicationContext.xml`)

| Bean id | Class | Notes |
|---|---|---|
| `clinom-x.FhirContext` | `ca.uhn.fhir.context.FhirContext` | Shared R4 context (singleton, cached) |
| `clinom-x.ClinomXDao` | `ClinomXDaoImpl` | Wired with OpenMRS `sessionFactory` |
| `clinom-x.ClinomXService` | `ClinomXServiceImpl` | Transactional proxy |
| `clinom-x.FhirQuestionnaireService` | `FhirQuestionnaireServiceImpl` | Transactional proxy |
| `clinom-x.FhirQuestionnaireResponseService` | `FhirQuestionnaireResponseServiceImpl` | Transactional proxy |

### Module activator (`ClinomXActivator`)

On startup the activator seeds three dummy patients (`CLX-001`, `CLX-002`, `CLX-003`) for development and testing purposes. Seeding is idempotent — existing patients are not duplicated. Remove or disable this logic before deploying to production.

---

## Building

**Prerequisites:** Java 8, Maven 3.6+, access to the OpenMRS Nexus repository.

```bash
# Build all modules and package the OMOD
cd backend
mvn clean package

# The compiled OMOD is copied automatically to backend/modules/
ls modules/clinom-x-*.omod
```

The `maven-antrun-plugin` in `omod/pom.xml` copies the compiled `.omod` into `../modules/` as part of the `package` phase, making it available for the Docker volume mount.

### Installing into a running OpenMRS

1. Copy `modules/clinom-x-1.0.0-SNAPSHOT.omod` into the OpenMRS modules directory.
2. Restart the OpenMRS application or use the Admin → Manage Modules page to load the module hot.
3. Liquibase changesets run automatically on first load.

### Docker (full stack)

See the root `docker-compose.yml`. The `backend/modules/` directory is bind-mounted into the OpenMRS container so the built OMOD is picked up automatically.

---

## Testing

Tests live in `api/src/test/`. All tests are pure unit tests — no database, no application context.

```bash
# Run all API tests
cd backend
mvn test -pl api

# Run a specific test class
mvn test -pl api -Dtest=FhirQuestionnaireServiceImplTest
mvn test -pl api -Dtest=FhirQuestionnaireResponseServiceImplTest
mvn test -pl api -Dtest=FhirComplianceTest
```

### Test classes

| Class | Scope | Tests |
|---|---|---|
| `FhirQuestionnaireServiceImplTest` | `FhirQuestionnaireServiceImpl` | CRUD, duplicate guard, semver ordering, malformed JSON handling |
| `FhirQuestionnaireResponseServiceImplTest` | `FhirQuestionnaireResponseServiceImpl` | CRUD, reference stripping, null subject/questionnaire handling |
| `FhirComplianceTest` | Both services | FHIR R4 rules: correct resourceType, required fields, valid status codes, reference format, item structure, JSON round-trip |
| `ClinomXServiceTest` | `ClinomXService` | Module info endpoint |

All tests use **Mockito** (mocked DAO) and a real **HAPI FhirContext** so that FHIR parsing/serialisation is exercised on every test run. The `FhirContext` is cached as a static field to avoid per-test initialisation overhead.
