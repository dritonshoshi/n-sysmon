# INSTRUCTION.md — adding N-SysMon to your application

A hands-on guide for developers who want to see **where the time goes inside their own
Java application**, without an external profiler.

- **Audience:** developers integrating N-SysMon into an application they own.
- **Scope:** the agent library (`n-sysmon-core`), which is what you embed. The
  central server is optional and currently much less finished — see
  [Sending data to a server](#8-sending-data-to-a-central-server-optional).

For background and design rationale, see [`docs/architecture/arc42/architecture.md`](docs/architecture/arc42/architecture.md).

---

## 1. What you get

You add one dependency and two `web.xml` entries. N-SysMon then gives you:

- A web UI inside your own application at `/nsysmon` showing a **tree of every
  measurement you instrumented**, with durations and call parameters.
- Automatic instrumentation of **HTTP requests** and — if you opt in — **every JDBC
  statement**, with no code changes at all.
- Built-in measurements of the **JVM and machine**: CPU, load, memory, GC, threads,
  OS, JAR versions.
- Graphs of scalar values over time, plus a threshold monitor view.
- A switch to turn everything off instantly if N-SysMon misbehaves.

What N-SysMon does **not** do: it is not a sampling profiler and it cannot see code you
did not instrument. It only measures what you explicitly measure, plus what the JDBC
wrapper and the HTTP filter see.

**Requirements:** Java 17 or newer, and a Servlet 6.0 container (Tomcat 11+, Jetty 12,
Open Liberty). Built and tested on JDK 17 and 21.

---

## 2. Add the dependency

```xml
<dependency>
    <groupId>com.github.dritonshoshi</groupId>
    <artifactId>n-sysmon-core</artifactId>
    <version>1.5.2-SNAPSHOT>
</dependency>
```

`n-sysmon-core` brings `slf4j-api` only — it deliberately does **not** ship a logging
binding. Add one yourself (e.g. `logback-classic`) or route N-SysMon's output into
your application's existing logging; see [Logging](#10-logging).

> If the dependency does not resolve, `n-sysmon-core` also needs
> `com.ajpj.a-foundation` (`a-foundation:1.0-pre13`), which is not on Maven Central and
> must come from a repository you control. N-SysMon itself resolves it.

---

## 3. Wire it into `web.xml`

Servlet 6.0 descriptor (`src/main/webapp/WEB-INF/web.xml`):

```xml
<web-app version="6.0" xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                             https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd">

    <servlet>
        <servlet-name>nsysmon</servlet-name>
        <servlet-class>com.nsysmon.servlet.NSysMonServlet</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>nsysmon</servlet-name>
        <url-pattern>/nsysmon/*</url-pattern>
    </servlet-mapping>

    <!-- OPTIONAL: measures every HTTP request your app handles. -->
    <filter>
        <filter-name>nsysmon-filter</filter-name>
        <filter-class>com.nsysmon.measure.http.AHttpRequestMeasuringFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>nsysmon-filter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

</web-app>
```

That is the entire integration. No other code has to change to get a working UI.

After deploying, open:

```
http://<host>:<port>/<your-context>/nsysmon
```

**Do not expose this URL publicly.** N-SysMon's UI has no authentication and reveals
your application's internal call structure, SQL statements and parameters. Bind it to a
private network or restrict it at the proxy.

---

## 4. Add `nsysmon.properties` to your classpath

Put a file `nsysmon.properties` next to your application's classes. Everything is
configured by **fully qualified class name** and instantiated reflectively.

```properties
# --- identity, shown in the UI -------------------------------------------
application-package = com.yourcompany.yourapp
application-name    = Your Application
application-version = 1.0.0

# --- what to measure ------------------------------------------------------
# Point-in-time values, read on demand when you open /nsysmon/scalars
scalar-measurers = com.nsysmon.measure.jdbc.AConnectionCounter, \
                   com.nsysmon.measure.scalar.ACpuUtilizationMeasurer, \
                   com.nsysmon.measure.scalar.AJmxGcMeasurer, \
                   com.nsysmon.measure.scalar.AJmxMemMeasurer, \
                   com.nsysmon.measure.scalar.ThreadingMeasurer

# Sampled every duration-of-one-timed-scalar seconds into graphs
timedscalar-measurers = com.nsysmon.measure.scalar.ACpuUtilizationMeasurer, \
                        com.nsysmon.measure.scalar.ThreadingMeasurer

# Quasi-static facts (OS, CPU, JVM, JAR versions) on /nsysmon/env
env-measurers = com.nsysmon.measure.environment.impl.AEnvVarEnvironmentMeasurer, \
                com.nsysmon.measure.environment.impl.ASysPropEnvironmentMeasurer, \
                com.nsysmon.measure.environment.impl.AOverviewEnvironmentMeasurer

# --- optional: aggregated views ------------------------------------------
# The drill-down view aggregates your measurements by name. Off by default
# because it keeps per-identifier statistics for every distinct identifier.
drilldown-measurement-initially-started = true

# --- optional: protect the web front -------------------------------------
# Without this property /nsysmon/* is open. See §11 for writing an authorizer.
# web-authorizer = com.example.MyWebAuthorizer

# --- limits and safety ---------------------------------------------------
max-nested-measurements          = 100
max-measurements-per-hierarchy   = 100000
data-sink-timeout-nanos          = 1000000000
duration-of-one-timed-scalar     = 2
averaging-delay-for-scalars-millis = 1000
```

Precedence is: built-in defaults < your `nsysmon.properties` < system properties with
the prefix `nsysmon.` (e.g. `-Dnsysmon.max-nested-measurements=200`). Every key is
documented in `nsysmon-core/src/main/resources/nsysmon-default.properties`.

Any property you omit falls back to that built-in file, so you only write what you want
to change.

---

## 5. Instrument your code

Everything goes through `NSysMon.get()`, which returns the `NSysMonApi`. The call is
one line:

```java
import com.nsysmon.NSysMon;

NSysMon.get().measure("charge-card", m -> {
    Result r = paymentGateway.charge(order);
    m.addParameter("gateway", paymentGateway.name());
    m.addParameter("orderId", order.getId());
    return r;
});
```

The lambda's body is measured. The return value is passed through, and a checked
exception from your code propagates unchanged — N-SysMon's `finally` block always closes
the measurement.

Because the measurement tree is built by **nesting**, the natural structure is nested
calls: each `measure()` becomes a child of whatever measurement is already running on
that thread, and the UI renders the tree.

```java
NSysMon.get().measure("handle-request", m -> {
    NSysMon.get().measure("load-order", m2 -> {
        Object order = repository.find(orderId);
    });
    NSysMon.get().measure("charge", m3 -> {
        paymentGateway.charge(order);
    });
});
```

> **Always use a block body `{ ... }` for the lambda.** An expression body is ambiguous
> and will not compile:
>
> ```java
> NSysMon.get().measure("charge", m -> gateway.charge(order));   // does NOT compile
> ```
>
> `measure` has two overloads — one taking a void callback, one taking a
> value-returning callback — and an expression lambda matches both, so javac cannot
> choose. Wrapping the body in braces resolves it. This is the single most common
> compile error when starting out, and the error message ("reference to measure is
> ambiguous") does not make the cause obvious.

### Manual start / finish

When you cannot wrap the code in a lambda — for example a servlet whose entry and exit
points are in different methods:

```java
ASimpleMeasurement m = NSysMon.get().start("process-upload");
try {
    handleUpload(request);
} finally {
    m.finish();
}
```

`start(identifier)` is **serial** (default): its children are strictly nested inside it,
so their durations add up to at most its own. Pass `false` for a **parallel**
measurement — `start("await-async-reply", false)` — for wall-clock spans that overlap
their parent, such as waiting for a callback. The UI marks serial measurements `+` and
parallel ones `-`, and treats parallel children as not consuming the parent's budget.

### Parameters

`addParameter(String, String)` attaches key/value context to a measurement, shown as a
tooltip in the trace views and as columns in the aggregated views. Use it for the things
that make a slow call slow: which customer, which endpoint, which SQL id. **Do not put
secrets or personal data in parameters** — they are rendered into the UI and, if you
offload data, sent to your server in clear JSON.

### Flows (correlation across threads and requests)

A *flow* ties together measurements that belong to one logical unit of work but do not
nest in one thread — async work, batch jobs across a thread pool, service-to-service
calls. Each flow needs an `ACorrelationId(qualifier, id, idParent)`.

```java
ACorrelationId flow = new ACorrelationId("order", orderId, parentOrderId);

// in the thread that receives the work
NSysMon.get().startFlow(flow);

// inside a running measurement, in any thread
NSysMon.get().joinFlow(flow);
```

- `qualifier` distinguishes kinds of id in one system (`"order"` vs `"conversation"`).
- `idParent` is the parent id, which gives the CorrelationFlow view its hierarchy.

**Both calls must happen inside a running measurement**, otherwise N-SysMon logs an error
and ignores them. Flows show up on `/nsysmon/correlationFlow`, which you must enable
with `correlation-flow-disabled = false` (it is `true` by default because the view is
known to be unreliable across clustered deployments).

### Collecting measurements

When you would otherwise write many tiny measurements — the classic case is iterating a
`ResultSet` — use a *collecting* measurement. It aggregates many samples into one node:

```java
ACollectingMeasurement m = NSysMon.get().startCollectingMeasurement("scan-customers");
try (ResultSet rs = stmt.executeQuery("select ...")) {
    while (rs.next()) {
        m.detail("next", d -> rs.next());   // or: startDetail / addDetailMeasurement / finishDetail
    }
} finally {
    m.finish();
}
```

Only **one detail may be open at a time** per collecting measurement; a second
`startDetail` is logged as an error and ignored. Use one collecting measurement per loop,
not one per row.

---

## 6. Where your measurements appear

This is the part worth memorising. A measurement you add with `measure()` is broadcast
to every data sink, and different UI views aggregate it differently:

| You instrumented… | Look at | On by default? |
|---|---|---|
| `measure(...)` / `start(...)` — your own tree | **Trace → All Traces** (`/nsysmon/tracesAll`) | **yes** |
| the same, viewed per HTTP request | **Trace → HTTP Requests** (`/nsysmon/tracesHttp`) | **yes**, with the filter from §3 |
| the same, aggregated by name with min/avg/max and counts | **Performance → Hierarchy** (`/nsysmon/drilldown`) | **no** — set `drilldown-measurement-initially-started = true` or press *Start* |
| SQL statements (with the JDBC wrapper, §7) | **Performance → JDBC** (`/nsysmon/jdbc`) | **no** — press *Start*; there is no config flag |
| the slowest individual measurements | **Trace → Top 100 Longest Calls…** | **yes** |
| flows from `startFlow`/`joinFlow` | **CorrelationFlow** (`/nsysmon/correlationFlow`) | **no** — needs `correlation-flow-disabled = false` |
| JVM/machine values over time | **Context → TimedScalars** (`/nsysmon/timedScalars`) | **yes**, every `duration-of-one-timed-scalar` seconds |
| values you flagged as interesting | **Context → Monitor** (`/nsysmon/monitoring`) | page yes, **empty** until you set `.active = true`, see §9 |
| point-in-time scalar values | **Context → Scalars** (`/nsysmon/scalars`) | **yes** (also the default landing page) |
| OS, CPU, JVM, JAR versions | **Context → Environment** (`/nsysmon/env`) | **yes** |
| GC pauses | **Memory → GC View** (`/nsysmon/memgc`) | **yes** |
| live threads | **Threads → Thread Dump** (`/nsysmon/threaddump`) | **yes** |
| which components are configured | **Debug → Debugging Data** (`/nsysmon/overviewDebuggingData`) | **yes** |

So: **instrument something, and it is in *Trace → All Traces* immediately.** Everything
else is a different aggregation of the same data.

Page ids are also usable directly in the REST URL, which is handy for scripting:

```
http://<host>:<port>/<ctx>/nsysmon/_$_nsysmon_$_/rest/tracesAll/getData
```

Note the literal `_$_nsysmon_$_` path segment. Errors in REST calls come back as HTTP
**599** with a JSON body containing the message and a stack trace.

---

## 7. Automatic instrumentation

### JDBC — per statement and per pool, with no code changes

Either wrap your existing `DataSource`:

```java
import com.nsysmon.NSysMon;
import com.nsysmon.measure.jdbc.NSysMonDataSource;

DataSource monitored = new NSysMonDataSource(yourDataSource, "orders-db", NSysMon.get());
```

…or prefix your JDBC URL so N-SysMon's driver picks it up:

```
jdbc:nsysmon:jdbc:h2:mem:demo                          # wraps an H2 connection
jdbc:nsysmon:jdbc:postgresql://host/db?qualifier=orders  # qualifier names the pool
```

`qualifier` becomes the pool name used in the "JDBC: Open Connections …" scalars.
Either way you get:

- A measurement per SQL statement, identified as `jdbc: <your SQL>` — shown on
  **Performance → JDBC** and in the trace views.
- **Connection pool accounting**: open and active connections per pool, as scalars.
- Result-set *iteration* attributed to the statement that produced it, which is usually
  where the real time goes.

For this to appear in **Performance → JDBC** you must press *Start* on that page.

### HTTP requests

Adding `AHttpRequestMeasuringFilter` (§3) wraps every request your application handles
in a measurement. These show up on **Trace → HTTP Requests**, and as a top-level
measurement in the other trace views.

The default analyzer names every request `<http request>` and records the full URL,
remote address and cookies as parameters. If you want per-URL aggregation instead,
supply your own `AHttpRequestAnalyzer`:

```java
public class ByPathAnalyzer extends ASimpleHttpRequestAnalyzer {
    @Override protected AOption<String> extractIdentifier(String url) {
        return AOption.some(url.replaceFirst("[?].*$", ""));   // strip the query string
    }
}
```

and point the filter at it with an init-param `nsysmon.http-analyzer` set to your class
name. **Exclude N-SysMon's own URLs**, or the UI's polling will show up as application
traffic.

---

## 8. Sending data to a central server (optional)

`com.nsysmon.datasink.offloadhttpjson.AHttpJsonOffloadingDataSink` POSTs measurements
to an N-SysMon server so you can compare several instances over time.

```java
NSysMonConfigurer.addDataSink(NSysMon.get(), new AHttpJsonOffloadingDataSink(
        NSysMon.get(),
        "http://nsysmon-server:8899/upload",
        "my-application",     // sender  -> application id
        "instance-1",         // senderInstance
        100, 1000,            // trace queue size, scalar queue size
        1,                    // offloading threads
        10 * 1000));          // scalar sampling interval, millis
```

**Be aware of the current state of the server before you rely on this.** It is the
least finished part of the project:

- The agent sends **traces and scalars only** — not environment measurements.
- The server **queues traces and environment data but never persists them**: both worker
  loops are commented out, so those queues fill and then silently discard their oldest
  entries. **In practice only scalar data survives.**
- The server **has no read API yet** and no authentication.
- Its MongoDB host, port and database name are hard-coded, and a missing database makes
  the server fail to start.

Use it to ship scalars to a database you query yourself. Do not expect the server's UI
to give you anything yet.

If you would rather keep everything local, register a plain data sink and write the
data yourself:

```properties
data-sinks = mycompany
data-sinks.mycompany = com.mycompany.MyOwnDataSink
```

```java
public class MyOwnDataSink implements ADataSink {
    @Override public void onStartedHierarchicalMeasurement(String identifier) { }

    @Override public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
        myExporter.send(data);
    }

    // ADataSink extends AShutdownable: release resources here
    @Override public void shutdown() { }
}
```

Built-in alternatives you can enable the same way: `slf4j`, `stdout`, `stderr`,
`cyclic-dump-stdout`, `cyclic-dump-slf4j-debug`, `cyclic-dump-slf4j-info` (see the
`data-sinks.*` block in `nsysmon-default.properties`). **None are enabled by default.**

---

## 9. Writing your own measurements

### Adding a number to a graph

The quickest route — no measurer class needed:

```java
import com.nsysmon.data.AScalarDataPoint;

NSysMon.get().addTimedScalarMeasurement(
    new AScalarDataPoint(System.currentTimeMillis(), "orders:pending", 42L, 0));
```

The `name` becomes the series key on **Context → TimedScalars**. Note the value is a
scaled `long`: `new AScalarDataPoint(ts, "cpu:util", 873L, 1)` means `87.3`. When a
measurer with the same key exists, your point inherits its group name and description —
otherwise the series lands in a group called `other` with no description.

### Writing a proper measurer

Implement `AScalarMeasurer` if you want the two-phase sampling some sources need (CPU
load needs two readings separated by time), plus grouping and descriptions:

```java
public class MyQueueDepthMeasurer implements AScalarMeasurer {

    @Override public void prepareMeasurements(Map<String, Object> mementos) { }

    @Override public void contributeMeasurements(Map<String, AScalarDataPoint> data,
                                                 long timestamp,
                                                 Map<String, Object> mementos) {
        data.put("queue:depth", new AScalarDataPoint(timestamp, "queue:depth", depth(), 0));
    }

    @Override public AOption<Long> getTimeoutInMilliSeconds() {
        return AOption.some(500L);
    }

    // Shown as the bold group header on the TimedScalars page
    @Override public String getGroupnameOfMeasurement(String key) {
        return "Messaging";
    }

    // Shown as the grey description, and defines which keys this measurer owns
    @Override public String getDescriptionOfMeasurement(String key) {
        return key.startsWith("queue:") ? "Depth of the message queues" : null;
    }

    // Which keys should get .medium / .high / .active threshold properties
    @Override public List<String> getConfigurationParameters() {
        return Arrays.asList("queue:depth");
    }

    // AScalarMeasurer extends AShutdownable: release resources here
    @Override public void shutdown() { }
}
```

Register it in `nsysmon.properties` under `scalar-measurers` (read on demand) or
`timedscalar-measurers` (sampled into graphs), or at runtime via
`NSysMonConfigurer.addScalarMeasurer(NSysMon.get(), new MyQueueDepthMeasurer())`.

`getTimeoutInMilliSeconds()` matters: a measurer that blocks longer than this is timed
out, warned about and eventually disabled, so it cannot stall your application.

### Threshold monitoring

For each key returned by `getConfigurationParameters()`, you can declare thresholds:

```properties
timedscalar.MyQueueDepthMeasurer.queue\:depth.medium = 100
timedscalar.MyQueueDepthMeasurer.queue\:depth.high   = 1000
timedscalar.MyQueueDepthMeasurer.queue\:depth.active = true
```

These affect **Context → Monitor** only:

- `.active = true` is what puts the series on that page at all.
- The row is coloured `medium` when the **average** over the recent window is above
  `.medium`, and `high` when above `.high`. It is the average, not the maximum.
- The Monitor page uses a short ring buffer (50 points), independent of the 2880-point
  buffer behind the graphs.

Note the escaped `\:` — `.` is the separator in property keys, so dots inside a
measurement name must be escaped.

You can also enable per-measurer collection at runtime:
`NSysMonConfigurer.addScalarMeasurer(...)`, `addEnvironmentMeasurer(...)`,
`addDataSink(...)`. Timed scalars can only be added through the properties file.

---

## 10. Logging

N-SysMon uses SLF4J. Because it is a library, it does not bring a binding — supply one,
or route its output into your own logging by selecting a backend:

```properties
# SLF4J (default), stdout, or java.util.logging:
logger = com.mycompany.MyLoggerFactory
```

Your class must implement `com.nsysmon.config.log.NSysMonLoggerFactory` and return
`NSysMonLogger` instances. That interface exists so you can send N-SysMon's output into
an existing logging system rather than introducing a second one.

---

## 11. Safety

N-SysMon runs inside a JVM you do not control the availability of, so it is built to fail
quietly:

- **Every** measurer and data sink is wrapped in a resilient decorator. One that throws
  or blocks is warned about, and after `max-num-*-timeouts` consecutive failures it is
  disabled. Exceptions from a measurer never propagate into your code.
- **Measurement trees are bounded** by `max-nested-measurements` (default 100) and
  `max-measurements-per-hierarchy` (default 100000). On overflow N-SysMon stops
  accepting new measurements and marks the data so the UI can show that it was truncated,
  rather than growing without limit.
- **Offloading and ingestion queues are bounded** and discard the **oldest** entry on
  overflow.
- **Instant kill switch.** If N-SysMon ever misbehaves in production, turn it off
  without a rebuild:

  ```
  -Dcom.nsysmon.globallydisabled=true
  ```

  Every measurement call then returns a no-op object. Note this one property is
  *not* prefixed with `nsysmon.`

### Authorizing the web front

The web front under `/nsysmon/*` is guarded by an `AWebAuthorizer`. **Without a
configuration entry it is open, exactly as in previous releases.** Data ingestion is
never affected: the server's upload endpoint and all measurers and data sinks keep
working regardless.

```properties
web-authorizer = com.example.MyWebAuthorizer
```

The simplest way to get HTTP Basic authentication is to extend
`AAbstractBasicWebAuthorizer`, which parses the `Authorization` header and sends the
login challenge; you only decide whether the credentials are accepted:

```java
public class MyWebAuthorizer extends AAbstractBasicWebAuthorizer {
    @Override protected boolean checkCredentials(String user, String password) {
        return "admin".equals(user) && "admin".equals(password);
    }
}
```

For a session, token or SSO based scheme implement `AWebAuthorizer` directly and
override `onUnauthorized`, for example to redirect to a login page. A single instance is
shared by all requests, so an implementation must be thread-safe.

Rejected requests are logged at `WARN` with the remote address and the URI (never with
the credentials) and receive an empty response: 401 with a Basic challenge for
`AAbstractBasicWebAuthorizer`, 403 otherwise. An authorizer that throws is treated as a
denial, so a broken backend fails closed rather than turning into a 500 page.

The demo (`n-sysmon-demo`, `n-sysmon-demo-liberty`) ships
`com.nsysmon.demo.DemoWebAuthorizer` with the fixed credentials **admin / admin**. It is
a demo only. HTTP Basic transmits the credentials in clear text, so put the web front
behind TLS, or use an authorizer that does not send reusable credentials.

Watch your overhead: the agent is cheap when you instrument a handful of call sites and
use the JDBC wrapper, but a scalar measurement reads live system state and the
on-demand `getScalarMeasurements()` call **sleeps for `averaging-delay-for-scalars-millis`**
(default 1000 ms) to average CPU-style values. Measure before enabling expensive sinks
in production.

---

## 12. Gotchas worth knowing up front

| Symptom | Cause |
|---|---|
| "reference to `measure` is ambiguous" when compiling | The lambda needs a **block body** `{ ... }`. An expression lambda matches both `measure` overloads. See §5. |
| My measurement is not in the UI | Look under **Trace → All Traces**. The aggregated views are separate pages and are off by default. |
| "a detail measurement is already running" | Only one detail per collecting measurement may be open. Use one collecting measurement per loop. |
| "flow handling only while a measurement is running" | `startFlow`/`joinFlow` must be called inside a running measurement. |
| "Calling 'finish' on a measurement that is not on the measurement stack" | Mismatched `start`/`finish`, usually from an early return or an exception path between them. Use `try`/`finally`. |
| The CorrelationFlow page is empty | `correlation-flow-disabled` defaults to `true`. |
| Performance → JDBC is empty | Press *Start* on that page; there is no property that starts it automatically. |
| Context → Monitor is empty | No series has `.active = true`. |
| My series appears under a group called `other` | No measurer claims that key, so no group name or description is available. |
| SQL statements show up but pool counters do not | `AConnectionCounter` must be in your `scalar-measurers` list. |
| The UI shows my own N-SysMon polling requests | Your HTTP analyzer is not excluding the N-SysMon URLs. |
| Measurements truncated, UI flags `wasKilled` | You exceeded `max-nested-measurements` or `max-measurements-per-hierarchy`. |

---

## 13. Checklist

- [ ] `n-sysmon-core` on the classpath, plus an SLF4J binding
- [ ] `NSysMonServlet` mapped under `/nsysmon/*`, `load-on-startup=1`
- [ ] `AHttpRequestMeasuringFilter` mapped on `/*` (optional but recommended)
- [ ] `nsysmon.properties` on the classpath with `application-package` and `application-name`
- [ ] `scalar-measurers` / `timedscalar-measurers` / `env-measurers` set to what you want
- [ ] `drilldown-measurement-initially-started = true` if you want aggregated views
- [ ] At least one `NSysMon.get().measure(...)` call site
- [ ] UI URL not reachable from an untrusted network
- [ ] Know that `-Dcom.nsysmon.globallydisabled=true` turns everything off

**Verify it works** by running the bundled demo, which exercises every view. The demo
application lives in `n-sysmon-demo-webapp`; there are two runners for it, and they deploy
the same webapp content — nothing is duplicated:

| Runner | Container | Port | How to start |
|---|---|---|---|
| `n-sysmon-demo` | embedded Jetty 12 (ee11) | 8181 | IntelliJ run configuration `NsysmonDemoMain (default)`, or the command line below |
| `n-sysmon-demo-liberty` | Open Liberty 26.0.0.10 | 9292 | [`n-sysmon-demo-liberty/README.md`](n-sysmon-demo-liberty/README.md) owns these instructions: `mvn -pl n-sysmon-demo-liberty -am install -DskipTests`, then `liberty:run`, which provisions the server, deploys the war and runs it; port via `-Dliberty.http.port=9393` |

The project ships four IntelliJ run configurations under `.run/` (default, G1GC, ZGC, and
a fixed-heap variant) for the Jetty runner — the simplest option is
**Run → `NsysmonDemoMain (default)`**. For the Liberty runner, `mvn -pl
n-sysmon-demo-liberty liberty:dev` starts the same server with Liberty's dev mode attached.

From the command line, build the Jetty runner and run it from the repository root (it
resolves its webapp root by relative path, and it needs **port 8181 to be free**; build
the whole reactor, because the runner serves the `n-sysmon-demo-webapp` module):

```
mvn -q -pl n-sysmon-demo -am install -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true
mvn -q -pl n-sysmon-demo dependency:build-classpath -Dmdep.outputFile=/tmp/demo-cp.txt
java -cp "n-sysmon-demo/target/classes:$(cat /tmp/demo-cp.txt)" com.nsysmon.demo.NsysmonDemoMain
```

Then open <http://localhost:8181/nsysmon> (Liberty:
<http://localhost:9292/nsysmon/>) and hit
<http://localhost:8181/content/> a few times to generate load — the demo fabricates GC
pressure, a deliberate deadlock, an H2 database with slow queries, a deep measurement
tree and correlation flows, so every view has something in it. That artificial load is
started by `ArtificialLoadServletContextListener` from the webapp's own `web.xml`, so it
does not depend on the runner.

---

## 14. Reference

| Topic | Where |
|---|---|
| Every configuration key, with comments | `n-sysmon-core/src/main/resources/nsysmon-default.properties` |
| Architecture, design rationale, known gaps | [`docs/architecture/arc42/architecture.md`](docs/architecture/arc42/architecture.md) |
| A working example of all of the above | `n-sysmon-demo-webapp/src/main/java/com/nsysmon/demo/AppServlet.java` |
| Public API surface | `n-sysmon-core/src/main/java/com/nsysmon/NSysMonApi.java` |
| Open issues and planned work | `todo.txt` |