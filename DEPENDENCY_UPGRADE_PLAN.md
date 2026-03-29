# Meraki: Dependency Modernization Audit & Upgrade Plan

**Date:** March 28, 2026
**Project:** Meraki (Mental Wellness Companion)
**Current Version:** 1.5 (versionCode 6)
**Architecture:** Single-module MVVM, Jetpack Compose, Firebase backend

---

## 1. Current Build Stack

| Component | Current Version | Latest Stable | Gap |
|---|---|---|---|
| Gradle Wrapper | 8.11.1 | 9.3.1+ | **Major** (8.x → 9.x) |
| Android Gradle Plugin (AGP) | 8.9.1 | 9.1.0 | **Major** (8.x → 9.x) |
| Kotlin | 2.0.0 | 2.3.20 | **Major** (3 minor releases behind) |
| Compose BOM | 2025.01.00 | 2026.03.00 | **Significant** (~14 months behind) |
| Compose Compiler/Plugin | via Kotlin 2.0.0 | via Kotlin 2.3.20 | Tied to Kotlin |
| Java/JVM Target | 11 | 17 (recommended for AGP 9.x) | **Needs upgrade for AGP 9** |
| compileSdk | 35 | 36 (Android 16) | 1 level behind |
| targetSdk | 35 | 36 | 1 level behind |
| minSdk | 31 | 31 (fine) | No change needed |
| R8 fullMode | false | true (default in AGP 9) | Behavioral change |

### Known Mismatches & Blockers

- **Kotlin 2.0.0 is significantly outdated.** Kotlin 2.3.20 is the current stable. The Compose compiler plugin is bundled with Kotlin since 2.0.0, so upgrading Kotlin upgrades Compose compiler automatically—but the Compose BOM must also be compatible.
- **JVM target 11 blocks AGP 9.x.** AGP 9.0+ requires JVM target 17 minimum. This is a hard requirement.
- **AGP 9.x requires Gradle 9.1.0+.** The current Gradle 8.11.1 cannot run AGP 9.x. These must be upgraded together.
- **R8 fullMode defaults to true in AGP 9.** Currently set to `false` in `gradle.properties`. Existing ProGuard rules may need review.
- **`play-services-auth` is deprecated.** Google is removing legacy Sign-In APIs from the SDK in May 2026. Migration to Credential Manager is urgent.
- **`generativeai` SDK (0.9.0) is deprecated.** Google recommends migrating to the unified Firebase AI / GenAI SDK.

---

## 2. Dependency Inventory

### 2.1 Build Toolchain

| Dependency | Current | Latest Stable | Risk | Impact |
|---|---|---|---|---|
| AGP (`com.android.application`) | 8.9.1 | 9.1.0 | **HIGH** | Entire build system |
| Kotlin (`org.jetbrains.kotlin.android`) | 2.0.0 | 2.3.20 | **HIGH** | All Kotlin source |
| Kotlin Compose plugin | 2.0.0 | 2.3.20 | **HIGH** | All Compose UI |
| Google Services plugin | 4.4.2 | ~4.5.0 | LOW | Firebase config |
| Firebase Crashlytics plugin | 3.0.2 | ~3.0.4 | LOW | Crash reporting |
| KSP (not currently used) | N/A | 2.3.20-1.0.x | **MEDIUM** | Room/Hilt migration |

### 2.2 AndroidX Core & UI

| Dependency | Current | Latest Stable | Risk | Impact |
|---|---|---|---|---|
| `androidx-core-ktx` | 1.15.0 | ~1.16.0 | LOW | Core utilities |
| `androidx-lifecycle-runtime-ktx` | 2.8.7 | ~2.9.0 | LOW | All ViewModels |
| `androidx-lifecycle-viewmodel-ktx` | 2.8.7 | ~2.9.0 | LOW | All ViewModels |
| `androidx-activity-compose` | 1.10.0 | ~1.11.0 | LOW | MainActivity |
| `androidx-material3` | 1.3.1 | ~1.4.0 | LOW | All UI screens |
| `material3-adaptive` | 1.1.0-alpha09 | ~1.2.0 | LOW | Adaptive layouts |

### 2.3 Jetpack Compose

| Dependency | Current | Latest Stable | Risk | Impact |
|---|---|---|---|---|
| Compose BOM | 2025.01.00 | 2026.03.00 | **MEDIUM** | All Compose UI |
| Compose UI / Material / Foundation | via BOM | via BOM | MEDIUM | All screens |

### 2.4 Navigation

| Dependency | Current | Latest Stable | Risk | Impact |
|---|---|---|---|---|
| `navigation-compose` | 2.8.5 | 2.9.7 | **MEDIUM** | All screen routing |
| `hilt-navigation-compose` | 1.2.0 | ~1.2.0 | LOW | All Hilt ViewModels |

### 2.5 Hilt / Dependency Injection

| Dependency | Current | Latest Stable | Risk | Impact |
|---|---|---|---|---|
| `hilt-android` | 2.52 | 2.57.1 | **MEDIUM** | All DI modules, all ViewModels |
| `hilt-compiler` | 2.52 | 2.57.1 | **MEDIUM** | Build-time code gen |

Note: Hilt 2.56+ has improved KSP support. Currently using KAPT—migration to KSP should be considered.

### 2.6 Room

| Dependency | Current | Latest Stable | Risk | Impact |
|---|---|---|---|---|
| `room-runtime` | 2.6.1 | 2.8.4 | **MEDIUM** | Chat persistence |
| `room-compiler` | 2.6.1 | 2.8.4 | **MEDIUM** | Code generation |
| `room-ktx` | 2.6.1 | 2.8.4 | LOW | Coroutine extensions |

Note: Room 3.0.0-alpha01 was released March 2026 (KMP-focused, breaking changes). Recommend staying on 2.8.x for now.

### 2.7 Coroutines

| Dependency | Current | Latest Stable | Risk | Impact |
|---|---|---|---|---|
| `kotlinx-coroutines-core` | 1.9.0 | ~1.10.x | LOW | All async code |
| `kotlinx-coroutines-android` | 1.9.0 | ~1.10.x | LOW | All async code |

### 2.8 DataStore

| Dependency | Current | Latest Stable | Risk | Impact |
|---|---|---|---|---|
| `datastore-preferences` | 1.1.2 | ~1.1.4 | LOW | Theme preferences |

### 2.9 Firebase & Google Services

| Dependency | Current | Latest Stable | Risk | Impact |
|---|---|---|---|---|
| Firebase BOM | 33.8.0 | 34.11.0 | **MEDIUM** | All Firebase services |
| `firebase-auth` | via BOM | via BOM | MEDIUM | Auth flow |
| `firebase-firestore` | via BOM | via BOM | MEDIUM | Journal, mood, user data |
| `firebase-messaging` | via BOM | via BOM | LOW | Push notifications |
| `firebase-analytics` | via BOM | via BOM | LOW | Analytics |
| `firebase-storage` | via BOM | via BOM | LOW | File storage |
| `firebase-crashlytics` | via BOM | via BOM | LOW | Crash reporting |
| `play-services-auth` | 21.3.0 | **DEPRECATED** | **CRITICAL** | Google Sign-In |

**CRITICAL:** `play-services-auth` deprecated APIs will be removed from the SDK in **May 2026**. Must migrate to `androidx.credentials` (Credential Manager).

### 2.10 Networking & JSON

| Dependency | Current | Latest Stable | Risk | Impact |
|---|---|---|---|---|
| `retrofit` | 2.9.0 | ~2.11.0 | LOW | Quotes API |
| `converter-gson` | 2.9.0 | ~2.11.0 | LOW | JSON conversion |
| `gson` | 2.10.1 | ~2.11.0 | LOW | JSON parsing |
| `okhttp` | 4.12.0 | 5.3.0 | **MEDIUM** | HTTP client |
| `logging-interceptor` | 4.11.0 | 5.3.0 | **MEDIUM** | Network logging |

Note: OkHttp 4.x → 5.x is binary compatible per Square, but the artifact coordinates change.

### 2.11 AI / Generative AI

| Dependency | Current | Latest Stable | Risk | Impact |
|---|---|---|---|---|
| `generativeai` | 0.9.0 | **DEPRECATED** | **HIGH** | Chatbot feature |

**HIGH RISK:** The standalone `com.google.ai.client.generativeai` Android SDK is deprecated. Google recommends migrating to either the Firebase AI SDK (`firebase-ai`) or the unified Google GenAI SDK. This will require API changes in `ChatViewModel`, `NetworkModule`, and `Utils`.

### 2.12 Media & Animation

| Dependency | Current | Latest Stable | Risk | Impact |
|---|---|---|---|---|
| `media3-exoplayer` | 1.5.1 | ~1.6.x | LOW | Breathing audio |
| `media3-ui` | 1.5.1 | ~1.6.x | LOW | Player UI |
| `lottie-compose` | 6.6.2 | ~6.7.x | LOW | Animations |
| `glide-compose` | 1.0.0-beta01 | ~1.0.0 stable | LOW | Image loading |

### 2.13 Accompanist

| Dependency | Current | Latest Stable | Risk | Impact |
|---|---|---|---|---|
| `accompanist-pager` | 0.28.0 | **DEPRECATED** | **MEDIUM** | Onboarding carousel |
| `accompanist-navigation-animation` | 0.30.1 | **DEPRECATED** | **MEDIUM** | Navigation transitions |

**Both are deprecated.** Accompanist Pager is replaced by `androidx.compose.foundation.pager` (available since Compose Foundation 1.4.0). Navigation animation is now built into `navigation-compose` 2.7.0+.

### 2.14 Paging

| Dependency | Current | Latest Stable | Risk | Impact |
|---|---|---|---|---|
| `paging-runtime-ktx` | 3.3.5 | ~3.3.7 | LOW | Defined but usage unclear |
| `paging-compose` | 3.3.5 | ~3.3.7 | LOW | Defined but usage unclear |

Note: Paging is declared in `libs.versions.toml` but no import of `androidx.paging` was found in source code. May be unused.

### 2.15 WorkManager

| Dependency | Current | Latest Stable | Risk | Impact |
|---|---|---|---|---|
| `work-runtime-ktx` | 2.10.0 | ~2.10.1 | LOW | Notification scheduling |

### 2.16 Testing

| Dependency | Current | Latest Stable | Risk | Impact |
|---|---|---|---|---|
| `junit` | 4.13.2 | 4.13.2 | None | Unit tests |
| `androidx-junit` | 1.2.1 | ~1.2.1 | None | Instrumented tests |
| `espresso-core` | 3.6.1 | ~3.6.1 | None | UI tests |
| `mockwebserver` | 4.11.0 | 5.3.0 | LOW | Network mocking |

Note: Only placeholder tests exist (ExampleUnitTest, ExampleInstrumentedTest). No feature-level test coverage.

---

## 3. Feature-to-Dependency Map

| Feature | Dependencies | Blast Radius |
|---|---|---|
| **Authentication (Login/Register/Settings)** | Firebase Auth, Firestore, play-services-auth (Google Sign-In), Hilt, Coroutines, Lottie (onboarding), Accompanist Pager (onboarding) | **CRITICAL** — auth touches every feature; play-services-auth deprecation is urgent |
| **Chatbot** | Generative AI SDK (Gemini), Room (chat history), Firebase Auth, Hilt, Coroutines | **HIGH** — generativeai SDK deprecation requires migration |
| **Journal (CRUD)** | Firebase Firestore, Firebase Auth, Hilt, Coroutines, Navigation | MEDIUM |
| **Mood Tracker & Insights** | Firebase Firestore, Firebase Auth, Hilt, Coroutines | MEDIUM |
| **Home Dashboard** | Retrofit + Gson (Quotes API), Firebase Firestore (streaks), Firebase Auth, Hilt, Coroutines | LOW-MEDIUM |
| **Breathing Exercise** | Media3/ExoPlayer (audio), Lottie (animation), Compose | LOW |
| **Notifications/Reminders** | Firebase Cloud Messaging, WorkManager | LOW |
| **Theme/Dark Mode** | DataStore Preferences, Compose Material3 | LOW |
| **Adaptive Layouts** | Material3 Adaptive, Compose | LOW |
| **Navigation** | Navigation Compose, Hilt Navigation Compose, Accompanist Navigation Animation | MEDIUM — Accompanist deprecation |
| **All Screens** | Compose BOM, Material3, Lifecycle, Activity Compose | **HIGH** — core UI framework |

---

## 4. Compatibility Risks

### 4.1 CRITICAL: AGP + Gradle + Kotlin + JVM Cluster

These **must** be upgraded together. AGP 9.x requires Gradle 9.1+, and both require JVM 17+. Kotlin 2.3.x is needed for compatibility with the latest Compose compiler.

**Upgrade path:**
- AGP 8.9.1 → 9.1.0 (requires Gradle 9.1+)
- Gradle 8.11.1 → 9.3.1 (required by AGP 9.x)
- Kotlin 2.0.0 → 2.3.20 (for Compose compiler compatibility)
- JVM target 11 → 17 (hard requirement for AGP 9)

**Risk:** HIGH. This is the single riskiest step—if this breaks, nothing else works.

### 4.2 HIGH: Kotlin + Compose BOM + Compose Compiler

The Compose compiler plugin is bundled with Kotlin since 2.0.0. Upgrading Kotlin to 2.3.20 automatically upgrades the Compose compiler. The Compose BOM must be compatible with this compiler version.

**Constraint:** Compose BOM 2026.03.00 requires Kotlin 2.1.0+ (verify exact minimum).

### 4.3 CRITICAL: play-services-auth → Credential Manager

`play-services-auth` (Google Sign-In) deprecated APIs are being **removed from the SDK in May 2026**. This is the most time-sensitive migration.

**Migration:** Replace `play-services-auth` with:
- `androidx.credentials:credentials`
- `androidx.credentials:credentials-play-services-auth`
- `com.google.android.libraries.identity.googleid:googleid`

**Impact:** LoginScreen, RegisterScreen, RegisterViewModel, and any Google Sign-In logic.

### 4.4 HIGH: Generative AI SDK Deprecation

The `com.google.ai.client.generativeai:generativeai:0.9.0` SDK is deprecated. Google recommends the Firebase AI SDK or unified GenAI SDK.

**Impact:** ChatViewModel, NetworkModule (Gemini model initialization), Utils (emotion analysis).

### 4.5 MEDIUM: Firebase BOM 33.8.0 → 34.11.0

Firebase BOM manages all Firebase library versions. A jump of ~1 major version (33 → 34) may include breaking changes. The BOM handles internal consistency, but API surface changes should be checked.

### 4.6 MEDIUM: Accompanist → Native Compose APIs

Both Accompanist libraries in use are deprecated:
- **Pager** → `androidx.compose.foundation.pager.HorizontalPager` (available in the current Compose BOM)
- **Navigation Animation** → built into `navigation-compose` 2.7.0+ (`AnimatedNavHost`)

### 4.7 MEDIUM: Room 2.6.1 → 2.8.x + KSP Consideration

Room 2.8.x works with both KAPT and KSP. Moving to KSP would improve build times. Room 3.0 exists but is alpha and KMP-focused—avoid for now.

### 4.8 MEDIUM: Hilt 2.52 → 2.57.x + KSP

Hilt 2.56+ has stable KSP support. If migrating Room to KSP, consider migrating Hilt to KSP simultaneously to eliminate KAPT entirely.

### 4.9 LOW: OkHttp 4.x → 5.x

Binary compatible per Square, but artifact coordinates change. Since Retrofit wraps OkHttp, ensure Retrofit version is compatible with OkHttp 5.x.

### 4.10 LOW-RISK: Potentially Unused Dependencies

- **Paging** (`paging-runtime-ktx`, `paging-compose`): Declared in version catalog but no imports found in source. Candidate for removal.

---

## 5. Recommended Upgrade Phases

### Phase 1: Build Toolchain Foundation
**Goal:** Get the project building on modern toolchain without touching library APIs.

| Change | From | To |
|---|---|---|
| Gradle Wrapper | 8.11.1 | 9.3.1 |
| AGP | 8.9.1 | 9.1.0 |
| Kotlin | 2.0.0 | 2.3.20 |
| Kotlin Compose plugin | 2.0.0 | 2.3.20 |
| JVM target | 11 | 17 |
| `sourceCompatibility` / `targetCompatibility` | Java 11 | Java 17 |

**Why grouped:** These are mutually dependent. AGP 9 won't run without Gradle 9 and JVM 17. Kotlin 2.3.20 is needed for Compose compiler compatibility.

**Expected Risk:** HIGH. Build script changes may surface deprecation warnings or errors.

**Additional steps:**
- Review `gradle.properties`: `android.nonTransitiveRClass` and `android.enableR8.fullMode` behavior changes in AGP 9
- Update `gradle-wrapper.properties` distribution URL
- Verify `settings.gradle.kts` repository configuration still works

**Validation:** Gradle sync, `./gradlew assembleDebug` compiles successfully.

---

### Phase 2: Core AndroidX & Compose Alignment
**Goal:** Bring Compose and AndroidX core to current stable.

| Change | From | To |
|---|---|---|
| Compose BOM | 2025.01.00 | 2026.03.00 |
| `androidx-core-ktx` | 1.15.0 | latest stable |
| `androidx-lifecycle-*` | 2.8.7 | latest stable |
| `androidx-activity-compose` | 1.10.0 | latest stable |
| `androidx-material3` | 1.3.1 | latest stable |
| `material3-adaptive` | 1.1.0-alpha09 | latest stable |
| Coroutines | 1.9.0 | latest stable |

**Why grouped:** Compose BOM governs Compose UI/Material/Foundation versions. Lifecycle and Activity Compose should match.

**Expected Risk:** MEDIUM. Compose API deprecations may cause warnings; Material3 component signature changes possible.

**Validation:** `assembleDebug`, visually check all major screens (Home, Journal, Mood, Settings, Chatbot).

---

### Phase 3: Navigation + Accompanist Removal
**Goal:** Upgrade Navigation Compose and replace deprecated Accompanist libraries.

| Change | Details |
|---|---|
| `navigation-compose` | 2.8.5 → 2.9.7 |
| Remove `accompanist-navigation-animation` | Replace with built-in `AnimatedNavHost` from navigation-compose |
| Remove `accompanist-pager` | Replace with `HorizontalPager` from `androidx.compose.foundation.pager` |
| `hilt-navigation-compose` | 1.2.0 → verify compatibility with Nav 2.9.x |

**Why grouped:** Navigation animation is the replacement for Accompanist navigation. Pager replacement is a small, isolated change (onboarding only).

**Expected Risk:** MEDIUM. Code changes required in `MerakiNavigation.kt` (NavHost → AnimatedNavHost), `OnBoarding.kt` (Accompanist Pager → Foundation Pager).

**Validation:** Full navigation flow (splash → onboarding → register → login → home → all screens → back navigation). Verify animations work.

---

### Phase 4: Architecture & Data Layer
**Goal:** Upgrade Room, Hilt, and consider KAPT → KSP migration.

| Change | From | To |
|---|---|---|
| Room | 2.6.1 | 2.8.4 |
| Hilt | 2.52 | 2.57.1 |
| KAPT → KSP | KAPT for Room + Hilt | KSP for both |
| DataStore | 1.1.2 | latest stable |
| WorkManager | 2.10.0 | latest stable |

**Why grouped:** Room and Hilt both use annotation processing. Migrating both to KSP simultaneously avoids running both KAPT and KSP.

**Expected Risk:** MEDIUM-HIGH. KSP migration changes `build.gradle.kts` plugin configuration and may surface compile errors. Room schema migration should be verified.

**KSP migration steps:**
1. Add KSP plugin to root `build.gradle.kts`
2. Replace `kapt` with `ksp` in `app/build.gradle.kts` for Room and Hilt
3. Replace `hilt-compiler` KAPT with `hilt-compiler` KSP
4. Replace `room-compiler` KAPT with `room-compiler` KSP
5. Remove KAPT plugin entirely

**Validation:** `assembleDebug`, chatbot conversation with history persistence, theme preference switching, notification scheduling.

---

### Phase 5: Firebase & Google Integrations
**Goal:** Update Firebase BOM and begin the critical Credential Manager migration.

#### Phase 5a: Firebase BOM Update
| Change | From | To |
|---|---|---|
| Firebase BOM | 33.8.0 | 34.11.0 |
| Google Services plugin | 4.4.2 | latest stable |
| Crashlytics plugin | 3.0.2 | latest stable |

**Validation:** Auth flow, Firestore read/write (journal CRUD, mood logging), FCM token registration, Crashlytics initialization.

#### Phase 5b: Credential Manager Migration (URGENT)
| Change | Details |
|---|---|
| Remove `play-services-auth` | Replace Google Sign-In with Credential Manager |
| Add `androidx.credentials:credentials` | New credential API |
| Add `credentials-play-services-auth` | Play Services backend for Credential Manager |
| Add `googleid` library | Google ID token handling |

**Code changes required:**
- `LoginScreen.kt`: Replace `GoogleSignIn` / `GoogleSignInClient` with `CredentialManager` API
- `RegisterScreen.kt`: Same migration
- `RegisterViewModel.kt`: Update sign-in flow logic
- `NetworkModule.kt`: Update any Google auth initialization

**Expected Risk:** HIGH. Auth is critical path. Google Sign-In API surface changes significantly with Credential Manager.

**Validation:** Full auth flow: register with email, login, Google Sign-In, password reset, sign out, account deletion.

#### Phase 5c: Generative AI SDK Migration
| Change | Details |
|---|---|
| Remove `generativeai:0.9.0` | Deprecated standalone SDK |
| Add Firebase AI SDK or GenAI SDK | Replacement |

**Code changes required:**
- `ChatViewModel.kt`: Update model initialization and chat API calls
- `NetworkModule.kt`: Update Gemini model provision
- `Utils.kt`: Update emotion analysis calls

**Expected Risk:** HIGH. AI model API surface will change. Chat history format may need adaptation.

**Validation:** Chatbot conversation (send messages, receive AI responses), emotion analysis, chat history persistence.

---

### Phase 6: Supporting Libraries & Cleanup
**Goal:** Update remaining libraries, remove unused dependencies, final polish.

| Change | From | To |
|---|---|---|
| Retrofit | 2.9.0 | latest stable |
| Gson | 2.10.1 | latest stable |
| OkHttp | 4.12.0 | 5.3.0 |
| Logging Interceptor | 4.11.0 | 5.3.0 |
| Media3 ExoPlayer | 1.5.1 | latest stable |
| Lottie Compose | 6.6.2 | latest stable |
| Glide Compose | 1.0.0-beta01 | 1.0.0 stable |
| MockWebServer (test) | 4.11.0 | 5.3.0 |
| Remove Paging libraries | 3.3.5 | (remove if unused) |
| compileSdk / targetSdk | 35 | 36 |

**Why last:** These are lower-risk, independent upgrades. OkHttp 5 is binary compatible. Retrofit, Media3, Lottie are isolated to specific features.

**Expected Risk:** LOW-MEDIUM. OkHttp artifact coordinates change (verify Retrofit compatibility). compileSdk 36 may introduce new lint warnings.

**Validation:** Quotes loading (Home), breathing audio playback, onboarding animations, image loading.

---

## 6. Validation Checklist

Use this after **each phase**. Items marked with a phase number indicate when they become particularly relevant.

### Build Validation (Every Phase)
- [ ] `./gradlew clean` completes
- [ ] `./gradlew assembleDebug` compiles with no errors
- [ ] `./gradlew assembleRelease` compiles (verify ProGuard/R8 rules still work)
- [ ] No new deprecation warnings that indicate broken functionality
- [ ] `./gradlew test` passes (placeholder tests)

### Startup & Auth (Phase 1+, Critical in Phase 5)
- [ ] App launches without crash
- [ ] Splash screen displays correctly
- [ ] Onboarding flow completes (Phase 3: verify pager replacement)
- [ ] Email/password registration works
- [ ] Email/password login works
- [ ] Google Sign-In works (Phase 5b: Credential Manager migration)
- [ ] Password reset flow works
- [ ] Sign out works
- [ ] Account deletion with reauthentication works

### Journal CRUD (Phase 2+, Critical in Phase 5a)
- [ ] Create new journal entry
- [ ] View journal list with real-time updates
- [ ] Search/filter journals
- [ ] Delete journal entry
- [ ] Journal data persists across app restart

### Mood Logging & Insights (Phase 2+)
- [ ] Log a mood score
- [ ] View mood trend history
- [ ] Mood insights screen loads with analytics
- [ ] Streak calculation on Home screen

### Chatbot (Phase 4+, Critical in Phase 5c)
- [ ] Send message and receive AI response
- [ ] Chat history persists (Room database)
- [ ] Chat history survives app restart
- [ ] Emotion-based context tracking works
- [ ] New conversation starts clean

### Home Dashboard (Phase 6)
- [ ] Inspirational quote loads from API
- [ ] Daily streak displays correctly

### Notifications (Phase 4+)
- [ ] FCM token registration succeeds
- [ ] Push notification received
- [ ] WorkManager scheduled notification fires
- [ ] Notification tap opens correct screen

### Breathing & Media (Phase 6)
- [ ] Breathing screen loads
- [ ] Audio playback starts and stops
- [ ] Lottie animation plays smoothly
- [ ] Timer countdown works correctly

### Theme & UI (Phase 2+)
- [ ] Dark mode toggle works
- [ ] Theme preference persists across restart
- [ ] All screens render correctly in light and dark mode
- [ ] Adaptive layouts work on different screen sizes

### Navigation (Phase 3)
- [ ] All screen transitions animate correctly
- [ ] Back navigation works from every screen
- [ ] Deep links work (if any)
- [ ] No navigation crashes on rapid back presses

---

## 7. Open Questions & Assumptions

### Open Questions

1. **Generative AI SDK replacement:** Which replacement does the team prefer—Firebase AI SDK (tighter Firebase integration) or standalone Google GenAI SDK (more flexibility)? This affects Phase 5c implementation.

2. **KSP migration:** Is build time a concern? KSP is faster than KAPT but requires changing annotation processor configurations. Could be deferred if risky.

3. **Room schema version:** What is the current Room database schema version? Upgrading Room may require a migration or `fallbackToDestructiveMigration()` if schema changes.

4. **compileSdk 36 (Android 16):** Are there any Android 16 behavioral changes that affect this app? Need to review release notes before bumping.

5. **Paging libraries:** Confirm these are unused and safe to remove. Search found no imports, but verify no dynamic/reflection usage.

6. **CI/CD:** No CI pipeline exists. Consider adding GitHub Actions for build validation after upgrades.

7. **Test coverage:** Only placeholder tests exist. Should minimal smoke tests be added before or during the upgrade to catch regressions?

8. **Credential Manager migration timeline:** Given the May 2026 removal deadline, Phase 5b may need to be prioritized ahead of Phases 3-4 depending on Play Store submission timelines.

### Assumptions

- Latest stable versions listed are based on web search results from March 2026. Exact patch versions should be verified against Maven Central / Google Maven at implementation time.
- The project is not using any Gradle convention plugins or composite builds.
- No multi-module refactoring is planned during this upgrade.
- The ProGuard rules in `proguard-rules.pro` cover the current dependency set and will need review (but not rewrite) for new versions.
- `local.properties` contains valid `GEMINI_API_KEY` and `WEB_CLIENT_ID` for testing.
- Firebase project configuration (`google-services.json`) is up to date and compatible with Firebase BOM 34.x.

---

## Summary Priority Matrix

| Priority | Item | Deadline |
|---|---|---|
| **P0 - URGENT** | Credential Manager migration (play-services-auth removal) | **May 2026** |
| **P0 - URGENT** | Build toolchain (AGP 9 + Gradle 9 + Kotlin 2.3 + JVM 17) | ASAP (blocks everything) |
| **P1 - HIGH** | Generative AI SDK migration | Before SDK EOL |
| **P1 - HIGH** | Compose BOM + AndroidX alignment | After toolchain |
| **P2 - MEDIUM** | Accompanist removal, Navigation upgrade | After Compose alignment |
| **P2 - MEDIUM** | Room + Hilt upgrade + KSP migration | After Compose alignment |
| **P2 - MEDIUM** | Firebase BOM update | Before Credential Manager migration |
| **P3 - LOW** | Retrofit, OkHttp, Media3, Lottie, cleanup | Final phase |

---

*This document is an analysis artifact. No code changes have been made. Proceed with Phase 1 only after reviewing and confirming the plan.*
