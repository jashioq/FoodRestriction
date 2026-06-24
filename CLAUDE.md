# CLAUDE.md

Kotlin Multiplatform app (`com.jan.food`), targets **Android + iOS + Desktop/JVM**. JVM is
hot-reload only — leave its `actual`s as `TODO`, don't wire it. Clean MVVM with Koin DI.

## Architecture

Layered, dependencies point inward: `presentation → domain → data`.

- **domain** — `model/` (plain/`@Serializable` data classes), `repository/` (interfaces),
  `useCase/` (one operation each), `util/UseCase`.
- **data** — `repository/` (impls), `dataSource/` (clients, platform stores).
- **presentation** — `screen/<name>/` with `ViewModel`, `Action`, `Destination`, `Screen`.

## Conventions

- **Repository pattern**: a domain **interface** in `domain/repository/`, and an impl with the
  **same class name** in `data/repository/`, referencing the supertype **fully-qualified** in the
  `:` clause (e.g. `class AuthRepository(...) : com.jan.food.domain.repository.AuthRepository`).
- **`Result` everywhere**: every suspend boundary returns `Result<T>` via `runCatching`. Unwrap
  inner calls with `.getOrThrow()`; surface to callers with `.onSuccess`/`.onFailure`.
- **Reading is a Flow**: any value flowing *up* from the data layer is a `Flow` (e.g.
  `emitSession(): Result<Flow<AuthSession?>>`), so callers subscribe instead of polling. One-shot
  reads (`.first()`) are allowed only **inside** the data layer; never expose them upward. Writes
  and actions return `Result<Unit>`/`Result<T>`.
- **Use cases**: `open class XUseCase(deps) : UseCase<Val, Res>` with a single `call(value)`.
  Multi-arg input gets a `XParams` data class in the same file. Map DTO → domain here or in the
  repo so DTOs never leak past `data`.
- **DTOs stay in `data`**: `dataSource/` `@Serializable` DTOs are mapped to `domain/model/` types
  in the repository; the domain layer never sees a DTO.
- **ViewModels**: extend `CoreViewModel<State, Action>`; inject use cases as their `UseCase<>`
  interface type; run work in `vmScope.launch`; drive everything through `sendAction(...)`. Use
  injected `vmScope`/`vmLogger` (never `viewModelScope`/`new Logger()` directly).
- **KDoc**: `/** ... @param ... */` on interface methods and class constructors.

## DI (Koin)

- Modules by layer: `domainModule` (use cases, `factory {}`), `dataModule` (repository bindings +
  network/data-source providers, `single {}`), `platformModule` (`expect/actual`, platform
  pieces only — DataStore, SecureStore). Bind to the **fully-qualified domain interface**.
- Common code can't live in `platformModule` (it's `expect/actual`); put shared
  providers in `dataModule`.
- Register modules in both `KoinInitializer.android.kt` and `.ios.kt`.

## Networking

- Ktor, engine auto-selected off the classpath (OkHttp on Android, Darwin on iOS) — build clients
  in `commonMain` with `HttpClient { }`, no `expect/actual`.
- Two clients: a **plain** one for Cognito (manual `x-amz-json-1.1` encode/decode, no content
  negotiation) and an **authed** one for the app API (ContentNegotiation + Bearer auth). The Bearer
  plugin sends the **ID token** and auto-refreshes via `AuthRepository`. Wire in dependency order
  (plain → CognitoAuthClient → AuthRepository → authed) to avoid a cycle.

## Config & secrets

- Build-time config via the `buildConfig` plugin → generated `com.jan.food.AppConfig`. Values
  resolve **env var (CI) → `local.properties` (local)** in `shared/build.gradle.kts`; never hardcode
  in source. `local.properties` and `build/` are gitignored.
- Note: the Cognito client id + API URL are **public** (ship in the app), externalized for config
  management, not secrecy. True secrets must never reach the client.

## Secure storage

`SecureStorageRepository` (Android Keystore / iOS Keychain) is implemented and DI-bound — consume
its interface (`putSecureString` / `emitSecureString` / `clearSecureString`); don't modify or
re-bind it.

## Build / verify

```bash
./gradlew :shared:compileAndroidMain                # Android
./gradlew :shared:compileKotlinIosSimulatorArm64    # iOS
```
Both must compile. Every Koin `get()` on the active path must resolve.
