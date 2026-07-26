# Shared module test framework & coverage

## Problem

The `shared` module has no real tests — only placeholder files in `commonTest`,
`androidHostTest`, `jvmTest`, and `iosTest` asserting `1 + 2 == 3`. There is no mocking,
coroutine-testing, or Flow-assertion tooling wired up, so there is no safety net for the
repository, use-case, and ViewModel layers and no established pattern for a coding agent to
follow when doing TDD.

## Solution

Stand up a unit-test framework in the `shared` module, located entirely in `commonTest` and
run via the Android host-test task, and add comprehensive coverage for the repository,
use-case, and ViewModel layers. Mocking is done with Mokkery (a KMP compiler-plugin),
coroutines with `kotlinx-coroutines-test`, and Flow/`StateFlow` assertions with Turbine, on
top of the existing `kotlin.test` assertions. The result is a deterministic, KMP-safe test
suite that both protects the current logic and gives future agents a consistent pattern to
extend via TDD. Nothing in the shipped app changes; this is test infrastructure and tests
only.

## User stories

1. As a developer, I want a mocking + coroutine + Flow test toolchain wired into
   `commonTest`, so that I can write deterministic unit tests that run via
   `./gradlew :shared:testAndroidHostTest`.
2. As a developer, I want every repository covered, so that data mapping and `Result`
   propagation are protected:
   - `FoodRepository` maps a data-source DTO to the domain `ProductCheck` and propagates
     success and failure.
   - `AllergenRepository` and `DataStoreRepository` read/write correctly over a mocked
     `DataStore<Preferences>`.
   - `AuthRepository` performs login, logout, session emission, and refresh over mocked
     `CognitoAuthClient` and `SecureStorageRepository` with a real `Json`.
3. As a developer, I want every use case in `domain/useCase/` covered, so that delegation to
   its repository, parameter passthrough, and `Result` behavior are verified.
4. As a developer, I want every ViewModel covered — `HomeScreenViewModel`,
   `MenuScreenViewModel`, `OnboardingScreenViewModel`, `NavigationViewModel` — so that
   actions sent via `sendAction` produce the expected state transitions.
5. As a developer, I want ViewModel tests to run without hitting platform logging, so that
   they pass on the host JVM (the Android `Logger` actual calls `android.util.Log`).
6. As a developer testing a ViewModel, I want to assert on `StateFlow` emissions over time
   (initial, loading, result, failure states), so that awkward states are checked, not just
   the happy path.
7. As a coding agent, I want a single consistent test pattern and home for tests, so that I
   can add new tests via TDD without re-deriving conventions.

## Implementation decisions

- **Layers touched:** none of the production layers change. New test code covers
  `data/repository/` (`FoodRepository`, `AllergenRepository`, `DataStoreRepository`,
  `AuthRepository`), all of `domain/useCase/`, and the ViewModels
  (`HomeScreenViewModel`, `MenuScreenViewModel`, `OnboardingScreenViewModel`,
  `NavigationViewModel`).
- **Location:** all tests live in `shared/src/commonTest`. No iOS-, desktop/JVM-, or
  Android-specific test source sets are used. Tests are run only via
  `./gradlew :shared:testAndroidHostTest`.
- **Mocking:** Mokkery, added to `gradle/libs.versions.toml`, the `plugins` block, and
  `shared/build.gradle.kts`. Used to mock domain interfaces, use-case interfaces, and
  in-module classes (`FoodRemoteDataSource`, `CognitoAuthClient`, `DataStore<Preferences>`).
- **Async testing:** `kotlinx-coroutines-test` (`runTest` + `StandardTestDispatcher`
  injected into `CoreViewModel`'s `vmScope`) and Turbine for `Flow`/`StateFlow` assertions.
  Both added to the version catalog and the `commonTest` dependencies.
- **Assertions:** `kotlin.test` (already present); no new assertion library.
- **Seams / mock boundaries:**
  - `FoodRepository` is tested by mocking `FoodRemoteDataSource` (the data-source boundary),
    not through Ktor's `MockEngine`. The Ktor data-source layer is out of scope.
  - `AllergenRepository` and `DataStoreRepository` are tested by mocking
    `DataStore<Preferences>` with Mokkery.
  - `AuthRepository` is tested by mocking `CognitoAuthClient` and `SecureStorageRepository`
    and using a real `Json` instance (the external final `Json` cannot be mocked by
    Mokkery). `SecureStorageRepository` is only consumed as a mock — never modified or
    re-bound.
  - ViewModels are tested by mocking the use-case interfaces they inject and by injecting a
    test `CoroutineScope` (built on `StandardTestDispatcher`).
- **Logger handling:** a no-op `TestLogger : Logger()` fixture in `commonTest` is injected
  into ViewModels, because the Android `Logger` actual calls `android.util.Log`, which is
  unavailable on the host JVM. `Logger` is an `open` `expect class`, so subclassing is
  preferred over mocking it.
- **Fixtures:** small sample-data builders for domain models (`Allergen`, `ProductCheck`,
  `AuthSession`) in `commonTest`, added as needed by the tests.
- **Platform:** common only. No `expect`/`actual` test code; no Android/iOS-specific tests.
- **Data:** no production persistence, auth, or session behavior changes; those collaborators
  are mocked in tests.
- **Placeholders:** delete all four placeholder test files (`commonTest`, `androidHostTest`,
  `jvmTest`, `iosTest`); `commonTest` becomes the sole home of tests.
- **Coverage tooling:** no Kover (or other coverage-reporting) plugin.
- **Standards doc:** no `TESTING.md` is written in this pass.

## Verification

- Toolchain (story 1): `./gradlew :shared:testAndroidHostTest` runs the new suite green.
  `./gradlew :shared:compileAndroidMain` and
  `./gradlew :shared:compileKotlinIosSimulatorArm64` both pass, confirming the added test
  dependencies and Mokkery plugin do not break either target. Gradle is run only as the
  final verification step.
- Repositories (story 2): each repository test asserts both the success mapping/value and
  the failure path (`Result.isFailure`) against its mocked collaborators, and passes under
  `:shared:testAndroidHostTest`.
- Use cases (story 3): each use-case test verifies the mocked repository is called with the
  expected parameters and that the returned `Result` is propagated, passing under
  `:shared:testAndroidHostTest`.
- ViewModels (stories 4, 6): each ViewModel test drives behavior via `sendAction` and
  asserts the resulting `state` transitions through Turbine (including loading and
  failure states where applicable), passing under `:shared:testAndroidHostTest`.
- Logger (story 5): ViewModel tests run to completion on the host JVM without an
  `android.util.Log` failure, confirming `TestLogger` is used on all logging paths.
- Agentic pattern (story 7): the delivered tests share one consistent structure and all
  live under `shared/src/commonTest`; the four placeholder files are gone.

## Out of scope

- UI / Compose tests.
- iOS-specific and desktop/JVM-specific tests, and running the `:shared:jvmTest` or iOS
  simulator test suites.
- Data-source layer tests, including Ktor `MockEngine` end-to-end tests of
  `FoodRemoteDataSource`.
- Code-coverage reporting (Kover) and coverage-percentage targets.
- A testing standards document (`TESTING.md`).
- Any change to production/shipped code, including `SecureStorageRepository`.
