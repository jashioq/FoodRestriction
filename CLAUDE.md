# CLAUDE.md

Kotlin Multiplatform app (`com.jan.food`), targets Android + iOS + Desktop/JVM.
Clean MVVM, Koin DI. Architecture and coding conventions live in `.claude/standards/`.

## Build & verify

```bash
./gradlew :shared:compileAndroidMain                # Android compile check
./gradlew :shared:compileKotlinIosSimulatorArm64    # iOS compile check
```

Both must pass before a change is considered done.

Tests run with `./gradlew :shared:testAndroidHostTest`.

Don't run desktop (`:shared:jvmTest`) or iOS simulator tests: Desktop/JVM isn't a shipped
target, and the iOS suite needs a simulator for no current benefit.

## Rules

- **Do not run Gradle unless explicitly asked, or as the final verification step of a
  completed change.** Gradle costs 10-30s per invocation. Type-check via the language
  server while iterating; batch verification to the end.
- Commit to the current branch only. Never create branches, force-push, `reset --hard`,
  or merge a PR.
- Desktop/JVM is hot-reload only — leave its `actual`s as `TODO`, don't wire it up.
- `SecureStorageRepository` is implemented and DI-bound. Consume its interface; never
  modify or re-bind it.
- Config values come from the generated `AppConfig` (env var → `local.properties`).
  Never hardcode a client id, URL, or secret in source.
 
