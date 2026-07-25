# Architecture

How the system is structured and wired. For per-file coding rules see `CODING_STANDARDS.md`.

## Layers

Layered, dependencies point inward: `presentation → domain → data`.

- **domain** — `model/` (plain or `@Serializable` data classes), `repository/` (interfaces),
  `useCase/` (one operation each), `util/UseCase`.
- **data** — `repository/` (implementations), `dataSource/` (clients, platform stores).
- **presentation** — `screen/<name>/` containing `ViewModel`, `Action`, `State`,
  `Destination`, `Screen`; shared widgets in `components/<kind>/`.

A domain repository interface has its implementation in `data/repository/`. The domain
layer never sees a DTO.

## Targets & source sets

- `commonMain` holds everything shareable, including Compose UI.
- `androidMain` / `iosMain` / `jvmMain` hold `actual`s only — platform stores, camera,
  haptics, logging, Koin platform wiring.
- Desktop/JVM is hot-reload only. Its `actual`s stay `TODO`; don't build features on it.

## DI (Koin)

- Modules split by layer: `domainModule` (use cases, `factory {}`), `dataModule`
  (repository bindings plus network and data-source providers, `single {}`),
  `platformModule` (`expect`/`actual`, platform pieces only — DataStore, SecureStore).
- Bind to the **fully-qualified domain interface**.
- Common code cannot live in `platformModule` because it is `expect`/`actual`. Shared
  providers go in `dataModule`.
- Register modules in both `KoinInitializer.android.kt` and `KoinInitializer.ios.kt`.
- Every `get()` on an active path must resolve.

## Networking

- Ktor, engine auto-selected off the classpath (OkHttp on Android, Darwin on iOS). Build
  clients in `commonMain` with `HttpClient { }` — no `expect`/`actual`.
- Two clients:
  - a **plain** client for Cognito (manual `x-amz-json-1.1` encode/decode, no content
    negotiation),
  - an **authed** client for the app API (ContentNegotiation + Bearer auth).
- The Bearer plugin sends the **ID token** and auto-refreshes via `AuthRepository`.
- Wire in dependency order — plain → `CognitoAuthClient` → `AuthRepository` → authed — to
  avoid a cycle.

## Config & secrets

- Build-time config via the `buildConfig` plugin, producing `com.jan.food.AppConfig`.
- Values resolve **env var (CI) → `local.properties` (local)** in `shared/build.gradle.kts`.
- `local.properties` and `build/` are gitignored. Never hardcode values in source.
- The Cognito client id and API URL are public (they ship in the app) and are externalized
  for config management, not secrecy. True secrets must never reach the client.

## Secure storage

`SecureStorageRepository` (Android Keystore / iOS Keychain) is implemented and DI-bound.
Consume its interface — `putSecureString` / `emitSecureString` / `clearSecureString`.
Do not modify or re-bind it.
