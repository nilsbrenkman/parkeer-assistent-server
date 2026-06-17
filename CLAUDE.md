# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Kotlin/Ktor (Netty) **backend-for-frontend** for the ParkeerAssistent iOS app (and a web client). It is a thin, mostly-stateless proxy that translates a simple app-facing API into calls against the upstream **Egis Parking Services** API (`api.parkeervergunningen.egisparkingservices.nl`) used for Amsterdam visitor parking permits. Most endpoints just forward the user's token, call upstream, and reshape the response.

## Commands

Build/run/test use the Gradle wrapper (`./gradlew`). JDK 25, Kotlin 2.2.

> JDK 24+ gates "restricted" native calls (JEP 472). Netty's `System.loadLibrary` for its
> native transport would otherwise warn (and eventually fail), so the build passes
> `--enable-native-access=ALL-UNNAMED` via `applicationDefaultJvmArgs` (covers `./gradlew run`
> and the `installDist` start script the Docker image runs) and via `jvmArgs` on the `test` task.

| Command | Purpose |
| --- | --- |
| `./gradlew run` | Run the server locally (reads env from `.env`, see below) |
| `./gradlew test` | Run all tests (JUnit 5) |
| `./gradlew test --tests "nl.parkeerassistent.route.GeoRoutesKtTest"` | Run a single test class |
| `./gradlew build` | Compile + test + assemble |
| `./gradlew installDist` | Produce runnable dist under `build/install/` (what the Dockerfile uses) |
| `./build.sh` | Build multi-arch Docker image and push to `nilsbrenkman/parkeerassistent` (bump the `TAG` var first) |

`build.gradle.kts` injects every `KEY=VALUE` line from `.env` into the environment of any `JavaExec` task (`run`, `test`). `PORT` is **required** — `main()` calls `System.getenv("PORT").toInt()` and will crash if it is unset, so keep `.env` populated for local runs.

The server ships its own UI: the vanilla-JS app under `src/main/resources/static/`. (The separate React/Vite frontend that used to live in `server/web/` has been moved out to the workspace-root `web/` component — see the root `CLAUDE.md`.)

## Architecture

Request flow: **`Application.kt` → `Routing.kt` → `route/*` → `service/*` → `client/Api.kt` → Egis**.

- **`Application.module()`** wires four concerns: `configureHTTP` (CORS wide-open, gzip, JSON, forwarded headers, rate limiting), `configureErrorHandling`, `configureMetrics`, `configureRouting`.
- **`route/*Routes.kt`** are Ktor routing DSL blocks typed as `RouterGroup` (`Route.() -> Unit`). They only parse params and delegate to a service; keep them thin.
- **`service/*Service.kt`** are `object` singletons holding the business logic: call upstream via `Api`, convert DTOs, and emit metrics. Each defines a `Method` enum implementing `ServiceMethod` used for logging/metrics labels.
- **`client/Api.kt`** is the single HTTP client to Egis. `addHeaders` reads the `token` cookie and sets `Authorization: Bearer <token>`. `followRedirects = false` is deliberate — upstream redirects mean "not authenticated" and are turned into 401s by the error handler.

### Two DTO worlds — do not mix them

- **`model/`** = the app-facing API contract (what the iOS/web client sends and receives).
- **`external/`** = upstream Egis API shapes.

Services are the boundary that converts between them (e.g. `ParkingService.convertParkingSession`, `UserService` mapping product/zone/regime). When adding a field, decide which world it belongs to.

### Session state lives in cookies

The server is otherwise stateless. Two httpOnly cookies carry session context between requests:
- `token` — the Egis bearer token, set on login, cleared on logout and on any upstream 401/redirect.
- `product_id` — the user's active visitor-permit product id, set by `UserService.get` and **required** by `ParkingService` / `UserService.balance` / `regime`. So the client must hit `/user` before parking calls work.

### Mock mode (for App Store review)

`MockRouteSelector` (in `mock/MockService.kt`) short-circuits the real routes when either the header `X-ParkeerAssistent-Mock: true` is present, or the request's `X-ParkeerAssistent-Build` is listed in the `MOCK_BUILDS` env var. Matching requests are served by `mockRouting()` against an in-memory `MockState` (held by `MockStateContainer`, auto-expires after 15 min). Mock login is `test` / `1234`; username `reset` rebuilds the state. This exists so App Store reviewers get deterministic data without real credentials.

### Error handling

`ErrorHandling.kt` intercepts at the `Monitoring` phase and maps upstream Ktor client exceptions to responses: redirects and 401s clear the `token` cookie and return 401; other client/server errors are passed through; anything else becomes 500.

### Metrics & observability

`Metrics.logAndCount(call, method, level, message)` is called throughout services. It both logs a structured line and increments two Prometheus counters: `parkeerassistent_api_requests` (labeled by service/method/level/message) and `parkeerassistent_device_data` (from `X-ParkeerAssistent-OS/SDK/Version/Build` headers). Scrape endpoint: `GET /metrics`.

### Dates

`util/DateUtil.kt` centralizes formatting and pins business logic to the **Europe/Amsterdam** timezone. The app-facing wire format is `yyyy-MM-dd'T'HH:mm:ssX`; upstream start/stop use distinct RFC-1123 / UTC formatters. Reuse these formatters rather than constructing new ones.

### Geo

`GeoService` lazily loads `src/main/resources/parkeerautomaten.json` (Amsterdam parking-meter GeoJSON), projects lat/lon to planar meters via `Point.from`, and does nearest-neighbor / bounding-box queries. `GeoRoutesKtTest` asserts against specific records in that file, so regenerating the data will require updating the test expectations.

### Testing

Tests run **fully offline** — none touch Egis. The whole suite covers only code that doesn't reach `client/Api.kt`:
- **Pure unit tests** for `util/` and pure service helpers (`LicenseUtilTest`, `DateUtilTest`, `MigrationUtilTest`, `UserServiceTest` for `getEndTime`).
- **Mock-mode route tests** (`mock/MockRoutesTest`): spin up the real app with `testApplication { application { module() } }` and drive the full routing stack against the in-memory `MockState` by sending the `X-ParkeerAssistent-Mock: true` header. Because the mock `token` cookie is set **without a path**, the test client's `HttpCookies` storage won't resend it to `/user` etc. — thread the token manually (capture it from each response's `setCookie()` and re-attach via `cookie("token", …)` in a `DefaultRequest` block) instead of relying on cookie persistence. Real browser clients work because they default the path to `/`.
- **Geo route tests** (`GeoRoutesKtTest`): also offline, since `GeoService` is the one service that never calls upstream.

Tests call `module()` directly (not `main()`), so the `PORT` env var isn't needed, but the Gradle `JavaExec` test task still injects `.env` if present.

To test the upstream-calling services (`ParkingService`/`UserService`/`VisitorService` DTO conversion), `client/Api.kt`'s `HttpClient` would need to be made injectable so a `MockEngine` can stand in — not done yet.
