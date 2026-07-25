# Coding standards

Rules a reviewer should check by reading a diff. Deliberately excludes anything a
formatter or linter enforces mechanically — those are not review material.

## Repository pattern

- A domain **interface** in `domain/repository/`, and an implementation with the **same
  class name** in `data/repository/`.
- The implementation references its supertype **fully-qualified** in the `:` clause:
  `class AuthRepository(...) : com.jan.food.domain.repository.AuthRepository`.

## Result at every suspend boundary

- Every suspend boundary returns `Result<T>`, produced via `runCatching`.
- Unwrap inner calls with `.getOrThrow()`.
- Surface outcomes to callers with `.onSuccess` / `.onFailure`.
- Don't swallow a failure silently, and don't convert a `Result` to a nullable to avoid
  handling it.

## Reading is a Flow

- Any value flowing *up* from the data layer is a `Flow` — e.g.
  `emitSession(): Result<Flow<AuthSession?>>` — so callers subscribe instead of polling.
- One-shot reads (`.first()`) are allowed **only inside** the data layer. Never expose a
  one-shot read upward.
- Writes and actions return `Result<Unit>` or `Result<T>`.

## Use cases

- Shape: `open class XUseCase(deps) : UseCase<Val, Res>` with a single `call(value)`.
- Multi-argument input gets an `XParams` data class in the same file.
- One operation per use case. If it does two things, it's two use cases.
- Map DTO → domain here or in the repository.

## DTOs stay in `data`

- `@Serializable` DTOs live in `dataSource/` and are mapped to `domain/model/` types in
  the repository.
- A DTO appearing in `domain` or `presentation` is a violation.

## ViewModels

- Extend `CoreViewModel<State, Action>`.
- Inject use cases as their `UseCase<>` interface type, never the concrete class.
- Run work in `vmScope.launch`. Never `viewModelScope`.
- Use the injected `vmLogger`. Never construct a `Logger` directly.
- Drive all state changes through `sendAction(...)` — no direct state mutation from the UI
  or from inside a coroutine body.

## Compose

- Composables are stateless where possible; state is hoisted to the ViewModel.
- No business logic, I/O, or repository calls inside a composable.
- Collect flows lifecycle-aware; don't launch long-lived work from composition.

## KDoc

- `/** ... @param ... */` on interface methods and on class constructors.
- Not required on private helpers or obvious overrides.
