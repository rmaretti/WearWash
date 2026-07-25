# Wear & Wash Resume Notes

## Current State

The focused Wear & Wash MVP is implemented and verified in `C:\Developer\workspace\WearWash`.

The project currently includes:

- Kotlin + Jetpack Compose Android app
- Room-backed persistence with explicit migrations and an exported schema
- Localized strings for English, Spanish, and Brazilian Portuguese
- Complete core care cycle from registration through washing
- Item detail, usage and wash history, and chronology-safe event deletion
- Relational laundry basket with single, selected, and bulk washing
- Derived washing-readiness rules with localized explanations
- Unit, persistence, migration, Compose E2E, and physical-device E2E coverage
- Requirements document: `CLOTHING_LAUNDRY_APP_REQUIREMENTS.md`

## Important Product Direction

Working app name:

```text
Wear & Wash
Track what you wear. Know when to wash.
```

Core positioning:

```text
A wardrobe care and laundry readiness companion, not a generic outfit planner.
```

The app should differentiate from Whering, Acloset, and Indyx by focusing on:

- Usage count since wash
- Washing criteria
- Laundry basket
- Needs washing badges
- Future event preparation
- Household washable items
- Laundry service partnerships

## Next Recommended Steps

1. Complete hands-on exploratory testing and record any usability issues.
2. Address findings without changing the verified core domain invariants.
3. Add photos and structured lookup values for richer item registration.
4. Add future-event preparation and reminders.
5. Design backup/export and production release hardening.

## Environment Note

The current Codex execution session did not expose:

- `gradle`
- `ANDROID_HOME`
- `ANDROID_SDK_ROOT`

So the project was scaffolded but not built in this session.

## 2026-06-28 Checkpoint

Android Studio initially failed with this error:

```text
Configuration `:app:debugRuntimeClasspath` contains AndroidX dependencies, but the `android.useAndroidX` property is not enabled.
```

Fix applied:

- Created root `gradle.properties`
- Added `android.useAndroidX=true`
- Added standard Gradle/Kotlin project properties
- Changed Room database `exportSchema` from `true` to `false` to avoid schema export warning during early MVP development

Verification:

- `./gradlew.bat :app:assembleDebug` completed successfully
- App launched successfully from Android Studio

Next recommended implementation step:

1. Add local dependency container/database builder.
2. Replace sample item list with Room-backed UI state.
3. Implement Add/Edit Item form.
4. Persist registered washable items locally.

## 2026-06-29 Checkpoint

Implemented the first local persistence slice:

- Added `AppContainer` and `DefaultAppContainer`
- Added a Room database builder in `WearWashDatabase`
- Added `ItemRepository` backed by `WashableItemDao`
- Replaced the sample-only `ItemsScreen` with Room-backed UI state
- Added `ItemsViewModel`
- Added Add/Edit item dialog
- Persisted item metadata locally through Room
- Added temporary text fields for category, color, fabric, and season while lookup tables are still future work
- Added localized strings for the item form in English, Spanish, and Brazilian Portuguese

Verification:

- `./gradlew.bat :app:assembleDebug` completed successfully

Notes:

- Room database version is now `2`
- The app currently uses destructive migration during early MVP development
- The item form stores dates as text in `YYYY-MM-DD` format for now

Next recommended implementation step:

1. Add usage tracking actions and persist `UsageEventEntity`
2. Add wash tracking actions and persist `WashEventEntity`
3. Implement laundry basket add/remove workflows
4. Replace temporary text fields with lookup/custom value tables for category, color, fabric, and season

## 2026-06-29 UX Fix Checkpoint

Fixed the Add/Edit item experience:

- Fixed cursor jumping while typing by decoupling editor state changes from Room search flow subscriptions
- Simplified the default item form to the most important fields
- Moved optional item metadata behind a `More details` toggle
- Added quick item row actions:
  - `Used today` increments usage counts and writes a `UsageEventEntity`
  - Status chips allow marking an item as clean, worn, needs washing, or in basket
  - Marking an item clean resets `usesSinceWash` to `0`
  - Marking an item used recalculates whether it now needs washing

Verification:

- `./gradlew.bat :app:assembleDebug` completed successfully

## 2026-07-11 Requirements Review and Implementation Plan

The requirements were reviewed against the current implementation. The app currently has one working vertical slice: Room-backed item registration/editing, item search, `Used today`, persisted usage events, washing-readiness evaluation, and direct status controls. Entities exist for wash events, laundry basket entries, and future events, but their workflows and DAOs are not implemented yet.

### Agreed Domain Decisions

- Treat washing readiness as derived state rather than a freely edited status.
- Treat laundry-basket membership as relational state from `LaundryBasketEntryEntity`; an item may both need washing and be in the basket.
- Keep usage and washing operations atomic. A wash operation must update the item, create a wash event, and remove its basket entry in one transaction.
- Calculate date-based washing readiness when items are read/displayed so it does not become stale as time passes.
- Use archive as the normal removal action. Permanent deletion should be a deliberate secondary action because it cascades into history.
- Build history from durable domain events. Decide whether basket and item-lifecycle activity use dedicated tables or a unified activity-event table before implementing the history UI.
- Include future-event reminder infrastructure in the MVP, but defer Android system notifications until the core care workflow is complete.
- For MVP photos, implement gallery selection first; camera capture can follow.

### Requirements Clarifications Still Needed During Implementation

- Define whether deleting a usage event recalculates `usesSinceWash` from event chronology or simply decrements counters. Chronological recalculation is recommended for correctness around washes.
- Define the exact permanent-delete UX and retention expectations for history.
- Decide whether automatically detected `Needs washing` items appear inside the basket screen as a separate suggested section or are automatically given basket entries. A separate suggested section is recommended.
- Define currency behavior for purchase price. Storing only cents without a currency code is insufficient if multi-currency use is expected.

### Implementation Phases

#### Phase 1: Stabilize Domain and Persistence

1. Separate derived washing readiness from stored lifecycle status.
2. Define explicit basket reasons and wash/out-of-cycle reasons.
3. Add DAOs and repositories for usage history, wash events, laundry basket, and history.
4. Add transactional operations for record usage, delete usage, record wash, basket add/remove, and washing selected items.
5. Replace destructive migration before distributing production builds.
6. Add unit tests for washing rules and persistence transactions.

#### Phase 2: Complete the Core Care Cycle

Deliver the primary product loop:

```text
Register item -> record use -> needs washing -> add to basket -> wash -> clean
```

Implement:

- Item detail screen
- Custom-date usage with optional notes
- Usage history and undo/delete
- Mark-washed dialog with date and optional out-of-cycle comment
- Laundry basket screen
- Wash one, selected, or all basket items
- Localized explanations for why an item needs washing
- Archive and deliberate permanent-delete behavior

#### Phase 3: Improve Registration and Discovery

- Localized validation errors
- Date pickers and explicit currency handling
- Gallery photo selection with persisted URI permission
- Predefined and custom categories, colors, fabrics, and seasons
- Category filtering
- Cost-per-use display
- Loading, empty, and error states

#### Phase 4: Future Event Preparation

- Event list and event editor
- Assign multiple items to events
- Planned, needs-preparation, and prepared states
- Prepare with or without washing
- Event actions in item history
- Reminder scheduling and notification permissions

#### Phase 5: MVP Hardening

- Compose UI tests for critical workflows
- Room migration tests
- Accessibility and localization review
- Rotation and process-restoration checks
- Performance checks with larger item collections
- Backup/export design; Google Drive implementation may remain post-MVP

### Next Sprint

Focus exclusively on completing the wash cycle:

1. Refactor status and washing-readiness semantics.
2. Implement wash and basket DAOs/repositories.
3. Add transactional `recordWash` behavior.
4. Build item detail and mark-washed UI.
5. Build the laundry basket with single and bulk washing.
6. Add domain and repository tests.
7. Verify all new user-facing strings in English, Spanish, and Brazilian Portuguese.

Do not start event, photo, or lookup-table work until this core loop is coherent and tested.

## 2026-07-23 Core Care Cycle Checkpoint

Implemented the MVP care loop:

```text
Register item -> record use -> needs washing -> add to basket -> wash -> clean
```

Delivered:

- Derived washing readiness with localized usage/date explanations
- Item detail with custom-date usage, usage history, deletion, and wash history
- Relational laundry basket with suggested, selected, and all-item washing
- Atomic usage operations and atomic multi-item wash transactions
- Chronology protection that rejects washes dated before later usage or a previous wash
- Archive behavior that also removes basket membership
- Database-enforced unique basket membership
- Explicit Room migrations from versions 1 to 2 and 2 to 3
- Exported Room schema version 3
- Event-derived counters protected from ordinary item editing
- English, Spanish, and Brazilian Portuguese strings for the new workflows

Verification:

- 6 washing-readiness unit tests
- 5 Room-backed core-cycle and regression tests
- 2 executable migration tests
- 1 Robolectric Compose end-to-end journey
- 1 physical-device instrumentation journey on a Samsung SM-N950F running Android 9
- `./gradlew.bat testDebugUnitTest :app:assembleDebug` completed successfully
- `./gradlew.bat lintDebug` completed successfully with no errors
- `./gradlew.bat connectedDebugAndroidTest` completed successfully with 1/1 tests passing
- Independent review agent score: 4/5

Final device-test hardening completed today:

- Added a UI Automator production-path E2E test that launches `MainActivity` and uses the real Room repository
- Verified item creation, three usage records, derived `Needs washing` state, basket addition, and `Wash all`
- Made the test locale-independent by resolving visible labels from Android resources
- Added keyguard handling, screen-awake protection, keyboard dismissal, and clickable-ancestor selection for Android 9 compatibility
- Restored the device's normal screen-timeout behavior after verification

Work was saved as the initial Git snapshot:

```text
48b4542 Build and verify WearWash MVP
Author: Rogerio <rmaretti01@gmail.com>
```

Deferred from the focused MVP:

- Future events and reminders
- Photos and lookup tables
- Cost per use and richer metadata
- Cloud backup, partnerships, AI, and analytics

## 2026-07-25 Category Management Checkpoint

Implemented structured categories after hands-on UX review:

- Added a Room-backed category lookup table and explicit migration from version 3 to 4
- Preserved existing free-text category values as custom categories during migration
- Added 12 localized predefined categories with practical default washing rules
- Added searchable category selection to the Add/Edit Item form
- Category selection applies its washing rule as an editable item default
- Added category management with search, create, edit, and safe delete
- Predefined names are localized and protected; their washing defaults remain editable
- Custom categories retain exactly the name entered by the user
- Added migration and repository regression tests

Verification:

- `testDebugUnitTest`, `assembleDebug`, and `lintDebug` completed successfully
