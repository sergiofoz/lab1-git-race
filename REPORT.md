# 1. What I specified
- **Base:** A dynamic web application that greets the user. The greeting message adapts dynamically based on the current time of day (morning, afternoon, evening). The API returns an equivalent JSON payload.
- **Bonus (Auth & Achievements):** A secure perimeter requiring user authentication to access the main application. Users can register and log in. The system must track authenticated web visits per user, avoiding "phantom" visits from automated API calls or background browser pre-fetching. Based on the visit count, users dynamically unlock ranks (Newcomer, Regular, VIP). E2E tests must validate the full user journey without compromising production security settings (like CSRF).

# 2. What I changed
- **Base:** Modified `HelloController` and `HelloApiController` to return time-based greetings. Updated `welcome.html` to display these dynamic attributes.
- **Bonus:**
    - Added `SecurityConfig` to establish public (`/login`, `/register`) and private routes.
    - Added `UserAccount` entity and `UserRepository` interface for H2 database persistence.
    - Implemented `AuthController` and `CustomUserDetailsService` for the sign-up/sign-in flows.
    - Added `login.html` and `register.html` using Thymeleaf and Bootstrap.
    - Modified `HelloController` to extract identity from `Principal` and inspect `HttpServletRequest` headers.
    - Refactored `HelloControllerUnitTests` using Mockito to mock dependencies.
    - Added `HelloControllerMVCTests` with `@WithMockUser` and `@MockitoBean`.
    - Authored a comprehensive `IntegrationTest.kt` simulating a real browser flow.

# 3. Technical decisions
- **H2 In-Memory Database:** Chosen to persist users and visits during the application lifecycle without requiring external containerized databases, facilitating straightforward local evaluation.
- **BCrypt Password Encoding:** Chosen over plain-text storage to align with industry security standards against dictionary attacks.
- **Filtering Phantom Visits (`Accept: text/html`):** Instead of counting every request, the controller inspects the HTTP headers. This prevents background browser pre-fetching or API calls from artificially inflating the user's visit count and rank.
- **Maintaining CSRF in E2E Tests:** Rejected the common shortcut of disabling CSRF for testing. Instead, built a DOM scraper in `IntegrationTest.kt` to extract the hidden CSRF token and `JSESSIONID` cookie from the initial GET request and inject them into subsequent POST requests, mimicking exact browser behavior.
- **Overriding TestRestTemplate Redirects:** The default Java HTTP client follows `302 FOUND` redirects automatically, dropping intermediate session cookies. Injected a custom `SimpleClientHttpRequestFactory` with `instanceFollowRedirects = false` into the `TestRestTemplate` to gain manual control over the session lifecycle during E2E testing.

# 4. How I verified
- Executed `./gradlew check` and `./gradlew test` continually.
- **Initial Failures:**
    - `HelloControllerMVCTests` failed initially with `NullPointerException` because the isolated web context lacked Spring Security's `Principal`. Fixed by injecting a mock `Principal` directly into the test request builder.
    - `IntegrationTest` failed on login (`403 Forbidden` and `200 OK` instead of `302 FOUND`).
- **Fixes:** Analyzed the JUnit HTML reports. Realized `403` was due to missing CSRF tokens, fixed by implementing the HTML regex scraper. Realized the `200 OK` (instead of 302) was due to the HTTP client's auto-redirect behavior. Fixed by configuring `SimpleClientHttpRequestFactory` to block auto-redirects, allowing manual capture and forwarding of the `JSESSIONID` cookie to verify the VIP rank logic.

# 5. AI disclosure

| Field | What to write |
| :--- | :--- |
| **Tools / skills** | Gemini (Google) |
| **Purpose** | Debugging E2E test failures related to Spring Security (CSRF 403 errors) and `TestRestTemplate` redirect behaviors. Boilerplate refactoring for Mockito unit tests. Resolving Kotlin-specific syntax errors, type mismatches, and Java interoperability issues (AssertJ). |
| **Representative prompts** | "Why is TestRestTemplate returning 200 OK instead of 302 FOUND during login?", "Adapt these old MVC tests to support the new UserRepository constructor injection using Mockito", "Fix Kotlin type mismatch between MatchGroup and Int when parsing JSON response", "Why does Kotlin reject named arguments like ignoreCase=true for the AssertJ contains method?" |
| **Affected files/sections** | `IntegrationTest.kt` (DOM scraper, RequestFactory setup, JSON Map type conversions), `HelloControllerMVCTests.kt` (MockBean updates), `HelloControllerUnitTests.kt`. |
| **Validation steps** | Ran `./gradlew check`. Investigated HTML test reports to confirm the exact nature of the `403` and `200` errors. Applied Kotlin-specific type conversions (e.g., explicitly casting JSON values with `.toString().toInt()`) and replaced invalid named arguments with AssertJ's native `containsIgnoringCase()` to resolve compilation halts. |
| **Citations** | N/A (General framework syntax and debugging strategies). |
| **Human-reviewed** | Rejected AI suggestions that recommended disabling CSRF in the testing profile. Ensured the E2E test retained full security validations by actively parsing CSRF tokens. Reviewed and manually integrated the Kotlin syntax fixes to ensure I understood how Kotlin interacts with Java testing libraries. |