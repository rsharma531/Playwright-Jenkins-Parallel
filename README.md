# Journey-based Playwright Java Framework

A parallel UI-test framework where the unit of parallelism is a **journey** — one
complete business flow (signup, add-to-cart, search...) — not a test method.

```
TestNG  →  journeys  →  page objects  →  core engine (ThreadLocal browser factory)
```

Parallelism is **two-tier** and lives in the framework, not in Jenkins or TestNG:

| Tier | Where | Mechanism | Controlled by |
|------|-------|-----------|---------------|
| Shard layer | Jenkins | Matrix fans out N parallel branches; each JVM takes a disjoint slice of the journey manifest | `SHARD_INDEX` / `SHARD_TOTAL` |
| Worker layer | inside each JVM | ForkJoinPool workers pull journeys off a shared queue until it's empty (self-balancing) | `WORKERS` |

The same engine runs on a laptop with `SHARD_TOTAL=1` — CI and local differ only
by environment variables.

Target app: <https://automationexercise.com> (public demo e-commerce site).

---

## Project structure

```
.
├── pom.xml                         Maven build: Playwright, TestNG, ExtentReports, Surefire
├── mvnw / .mvn/                    Maven wrapper — no Maven install needed (local or CI)
├── Jenkinsfile                     Tier 1: matrix fans out 2 shards in Playwright Docker image
├── jenkins/
│   ├── docker-compose.yml          Local Jenkins controller (docker compose up -d --build)
│   └── Dockerfile                  Jenkins LTS + Docker CLI + required plugins
└── src
    ├── main/java/com/rahul
    │   ├── framework
    │   │   ├── config/FrameworkConfig.java      env-var config (BASE_URL, WORKERS, SHARD_*, ...)
    │   │   ├── core
    │   │   │   ├── BrowserFactory.java          ThreadLocal Playwright/Browser per worker thread
    │   │   │   ├── Journey.java                 the interface every business flow implements
    │   │   │   ├── JourneyContext.java          what a journey gets: its Page + its report node
    │   │   │   ├── JourneyEngine.java           Tier 2: shared queue + ForkJoinPool worker loops
    │   │   │   ├── JourneyManifest.java         reads manifest, slices it by shard
    │   │   │   └── JourneyResult.java           pass/fail record per journey
    │   │   └── reporting/ExtentReportManager.java  one Extent HTML report per shard
    │   ├── pages/                               BasePage, HomePage, SignupLoginPage,
    │   │                                        ProductsPage, CartPage (pure page objects)
    │   └── journeys/                            UserSignupJourney, GuestAddToCartJourney,
    │                                            ProductSearchJourney, NewsletterSubscriptionJourney
    ├── main/resources/journeys.manifest         THE list of journeys — single source of truth
    └── test
        ├── java/com/rahul/tests/JourneySuiteTest.java   the single TestNG entry point
        └── resources/testng.xml                         launcher suite (no TestNG parallelism!)
```

---

## How the flow works, end to end

### 1. TestNG is only the launcher
`mvn test` → Surefire → `testng.xml` → `JourneySuiteTest.runAllJourneysForThisShard()`.
There is deliberately **no** `parallel="..."` in testng.xml and no Surefire forking:
one JVM, one TestNG thread. TestNG's job is to start the engine and turn
"any journey failed" into a red build.

### 2. The manifest is sliced for this shard
`JourneyManifest` reads `journeys.manifest`, sorts it, and keeps line `i` when
`i % SHARD_TOTAL == SHARD_INDEX`. Because every shard sorts the same list, the
slices are disjoint and complete — nothing runs twice, nothing is skipped.
Locally `SHARD_TOTAL=1`, so the slice is "everything".

### 3. The engine runs the slice on a self-balancing pool
`JourneyEngine` drops the journeys into a `ConcurrentLinkedQueue` and starts a
`ForkJoinPool` with `WORKERS` threads. Each thread runs a worker loop:
`poll()` the next journey → run it → repeat until the queue is empty. No journey
is pre-assigned to a thread, so a thread stuck on a slow journey doesn't block
the fast ones — the queue self-balances.

### 4. The ThreadLocal browser factory makes that safe
Playwright Java is **not thread-safe**: every Playwright object must be used
only by the thread that created it. `BrowserFactory` therefore keeps a
`ThreadLocal<Playwright>` and `ThreadLocal<Browser>`: each worker thread lazily
launches its own browser once (launches are expensive), then hands each journey
a fresh, cheap `BrowserContext` (own cookies/storage — journey isolation).
When a worker's loop ends, it closes its own browser — on its own thread.

### 5. Reporting & failure evidence
- Tracing starts before every journey. **Pass** → trace discarded. **Fail** →
  `target/traces/<journey>-<time>.zip` saved, plus a full-page screenshot.
- Every journey is a node in the **Extent report**
  (`target/extent-report/extent-report.html`) with its business steps, thread
  name, duration, and — on failure — the stack trace, embedded screenshot and
  the trace path.
- Open a trace with `npx playwright show-trace target/traces/<file>.zip`
  or drag the zip into <https://trace.playwright.dev>.

### 6. Jenkins adds the shard layer on top
The `Jenkinsfile` matrix creates one parallel branch per `SHARD_INDEX` value.
Each branch runs `./mvnw test` inside `mcr.microsoft.com/playwright/java:v1.52.0-noble`
(browsers + OS deps pre-installed, version-matched to the pom). Jenkins never
knows what a journey is — it just sets `SHARD_INDEX` and archives
`shard-<n>/extent-report` + `shard-<n>/traces` per branch.

---

## Running locally

Prereqs: Java 17+ (you have it). No Maven needed — use the wrapper.

```bash
# whole suite, 2 parallel journeys, headless (first run downloads Chromium ~150 MB)
./mvnw test

# watch the browsers, more workers
HEADLESS=false WORKERS=4 ./mvnw test

# simulate CI shard 0 of 2 on your laptop
SHARD_INDEX=0 SHARD_TOTAL=2 ./mvnw test

# then open the report
open target/extent-report/extent-report.html
```

All knobs: `BASE_URL`, `BROWSER` (chromium|firefox|webkit), `HEADLESS`,
`WORKERS`, `SHARD_INDEX`, `SHARD_TOTAL` — env vars or `-Dkey=value`.

## Running on Jenkins (all in Docker)

Prereq: [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop/).

1. **Push this repo to GitHub** (public repo = no credentials needed in Jenkins):
   ```bash
   git init && git add -A && git commit -m "Journey-based Playwright framework"
   git branch -M main
   git remote add origin https://github.com/<you>/<repo>.git
   git push -u origin main
   ```
2. **Start Jenkins**:
   ```bash
   cd jenkins && docker compose up -d --build
   # initial admin password:
   docker exec jenkins-playwright cat /var/jenkins_home/secrets/initialAdminPassword
   ```
3. Open <http://localhost:8080>, paste the password, choose **Install suggested
   plugins** (the pipeline-specific ones are already baked into the image),
   create your admin user.
4. **New Item → Pipeline**, name it, then under *Pipeline*:
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**, Repository URL: your GitHub URL, Branch: `*/main`
   - Script Path: `Jenkinsfile`
5. **Build Now**. First build is slow (pulls the ~2 GB Playwright image and
   downloads Maven dependencies); later builds reuse both via the workspace-local
   `.m2repo`.
6. Watch the two shard branches run in parallel, then check the build page:
   **artifacts** `shard-0/…` and `shard-1/…` contain each shard's Extent report
   and any failure trace zips; test results come from the JUnit publisher.

Scaling out: to run 3 shards, change the matrix axis to `'0','1','2'` and set
`SHARD_TOTAL='3'` in the Jenkinsfile. The manifest math does the rest.

## Do we need Docker?

- **Locally: no.** Playwright downloads its own browsers on first run.
- **For CI: strongly recommended.** A Jenkins agent needs a JDK, browsers and
  dozens of OS libraries; the official Playwright image ships all of it,
  version-matched. Here Docker is used twice, for different reasons:
  1. `jenkins/docker-compose.yml` runs the **Jenkins controller** itself — only
     because you're practicing locally; with a real Jenkins server you'd skip it.
  2. The `Jenkinsfile` runs each **build stage** in the Playwright image — this
     part you'd keep in a real setup.

## Adding a journey

1. Create a class in `com.rahul.journeys` implementing `Journey`
   (keep it self-contained: create its own data, clean up after itself).
2. Add its fully-qualified class name to `src/main/resources/journeys.manifest`.

That's it — laptop runs, shard math and reports pick it up automatically.
