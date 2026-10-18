# n-sysmon-demo-liberty

Runs the **n-sysmon demo webapp on Open Liberty** — a second runner next to
[`n-sysmon-demo`](../n-sysmon-demo), which serves the very same webapp on embedded Jetty.

There is no demo code in this module. The application itself lives in
[`n-sysmon-demo-webapp`](../n-sysmon-demo-webapp) and is shared by both runners; this module adds
only the two things that are specific to Liberty: how the war is assembled and how the server is
configured. Change the demo in one place and both runners show it.

| | `n-sysmon-demo` | `n-sysmon-demo-liberty` |
|---|---|---|
| Container | embedded Jetty 12 (ee11), started in-process | Open Liberty 26.0.0.10, `servlet-6.1` |
| Packaging | jar | war (`n-sysmon-demo-liberty.war`) |
| HTTP port | 8181 | 9292 |
| Start with | `java … com.nsysmon.demo.NsysmonDemoMain` | `mvn -pl n-sysmon-demo-liberty liberty:run` |

Because the ports differ, both runners can be up at the same time.

## What the demo shows

The demo app is a small servlet that fabricates the load an n-sysmon user wants to look at: garbage
collection and allocation churn, a deliberately deadlocked pair of threads, slow H2 queries, a deep
measurement tree and correlation flows. That load is started by the webapp's own
`ArtificialLoadServletContextListener`, not by a launcher, so it runs identically on both
containers.

| URL | What it is |
|---|---|
| <http://localhost:9292/nsysmon/> | the n-sysmon UI — overview, GC, memory, threads, deadlock, JDBC, trace, longest calls, correlations, … |
| <http://localhost:9292/content/> | demo content that generates a "regular" system access |
| <http://localhost:9292/content/small/> | the same, as a "small" system access |

Keep the trailing slash on the UI URL: `/nsysmon` answers with a 302 redirect to `/nsysmon/`.

## Prerequisites

- **JDK 17 or newer** — the range the whole project supports (verified on 17 and 21).
- **Maven 3.9+**.
- **Network access on the first start.** The Liberty runtime is downloaded by the `liberty:*` goals
  (≈ 360 MB), never by the build: `mvn verify` packages this war without touching it. After the
  first download the runtime zip sits in the local Maven repository, so a rebuild of the server
  re-extracts it from there and needs no network.
- **Port 9292 free** — or override it, see below. 9292 is used instead of Liberty's default 9080,
  which is commonly taken by something else.

## Start it

From the **repository root** (the sibling modules are not installed in your local repository, so the
reactor has to build them):

```bash
# once: build n-sysmon-core and n-sysmon-demo-webapp, which this runner deploys
mvn -pl n-sysmon-demo-liberty -am install -DskipTests

# every time: start the server in the foreground; Ctrl+C stops it
mvn -pl n-sysmon-demo-liberty liberty:run
```

`liberty:run` is self-contained: it packages the war, creates the server, installs the
`servlet-6.1` feature, downloads the Liberty runtime on first use, deploys the war and then runs the
server in the foreground. If you want those steps separately — to provision a server before
starting it, for example, or from a script — they are:

```bash
mvn -pl n-sysmon-demo-liberty liberty:create liberty:install-feature liberty:deploy
mvn -pl n-sysmon-demo-liberty liberty:run
```

Then open <http://localhost:9292/nsysmon/> and hit <http://localhost:9292/content/> a few times to
generate load. If that URL shows something other than the n-sysmon UI — a "Not found" page, say —
then another process owns the port and the demo never got a listener; see
[When something goes wrong](#when-something-goes-wrong).

While developing, `liberty:dev` does the same in one goal and keeps the server in Liberty's dev
mode, where it recompiles and redeploys while it runs:

```bash
mvn -pl n-sysmon-demo-liberty liberty:dev
```

Dev mode deploys a *loose* application (`apps/nsysmon-demo-liberty.war.xml`) instead of the war,
overriding this module's `looseApplication=false`; the next `liberty:run` puts the war back.

**Do not combine `-am` with a direct `liberty:*` goal.** `-am` pulls the whole chain of sibling
modules into the reactor and a directly invoked goal runs on *every* module in it, so
`mvn -pl n-sysmon-demo-liberty -am liberty:run` would try to create a Liberty server in
`n-sysmon-core` too. That is why the `install` step comes first and the `liberty:*` goals are
called on this module alone.

**After changing demo code,** rebuild the modules and start again:

```bash
mvn -pl n-sysmon-demo-liberty -am install -DskipTests
mvn -pl n-sysmon-demo-liberty liberty:run
```

`liberty:run` repackages and redeploys on every start, so there is no separate deploy step to
remember — but recompiling your changes is not part of it, which is what the `install` step above
is for.

## Change the port

The port is the Liberty config variable `default.http.port`. `server.xml` reads it and falls back
to 9292, and the pom passes it to the server through `bootstrap.properties`:

```bash
mvn -pl n-sysmon-demo-liberty liberty:run -Dliberty.http.port=9393
```

Hand-editing files under `target/liberty` does not work: the plugin deletes and regenerates the
server's `bootstrap.properties` on every run, and `server.env` cannot be used for a dotted name
like `default.http.port` — Liberty's launcher only exports plain shell identifiers from it and drops
the rest silently.

## Stop it

`Ctrl+C` in the terminal that runs `liberty:run`. From another shell:

```bash
mvn -pl n-sysmon-demo-liberty liberty:stop
```

## How it is put together

- **The war.** This module has no `src/main/webapp`. `maven-dependency-plugin` unpacks
  `WEB-INF/web.xml` and `firstpage.png` out of the `n-sysmon-demo-webapp` jar into
  `target/webapp`, and `maven-war-plugin` takes that as its `warSourceDirectory`. The demo classes,
  `nsysmon.properties` and `logback.xml` reach the application the normal way, through
  `WEB-INF/lib/n-sysmon-demo-webapp.jar`. `<finalName>` makes the war `n-sysmon-demo-liberty.war`,
  which is the name `server.xml` deploys.
- **The server.** `src/main/liberty/config/server.xml` asks for exactly one feature, `servlet-6.1` —
  the API version `n-sysmon-core` is compiled against — listens on `${default.http.port}` without
  TLS, and deploys the war at context root `/` so that the UI ends up under `/nsysmon` just like in
  the Jetty runner.
- **The plugin configuration** (the pinned runtime artifact, `looseApplication=false`, the port
  property, the webapp unpacking) is in [`pom.xml`](pom.xml).
- **The runtime.** The plugin installs it under `target/liberty/wlp` and the server lives in
  `target/liberty/wlp/usr/servers/nsysmon-demo`. The log is
  `target/liberty/wlp/usr/servers/nsysmon-demo/logs/messages.log` — the server's own messages and
  everything the application writes (logback, `System.out` as `SystemOut` records) end up in that
  one file, and are echoed to the Maven console while `liberty:run` runs in the foreground.

## When something goes wrong

| Symptom | Cause and what to do |
|---|---|
| `CWWKO0221E … Address already in use` | 9292 is taken. Run with `-Dliberty.http.port=<free port>`. Note that `liberty:run` still ends with `BUILD SUCCESS` after a failed port bind — check the log rather than the exit code. |
| The browser shows another application, or “Not found” | Something else owns the port, so the demo never got a listener: the same `CWWKO0221E` as above, just less obvious, because Liberty starts anyway and the plugin still reports `BUILD SUCCESS`. Confirm with `CWWKO0219I … an Port <n> empfangsbereit` in `messages.log`, or with `ss -ltnp \| grep <port>`, then start again on a free port. |
| Nothing answers on the port | Read `messages.log`: the application may have failed to deploy, or the port is a different one than you expected. |
| The UI shows old content | Java changes were not recompiled — `liberty:run` repackages and redeploys, but it does not compile. Run `mvn -pl n-sysmon-demo-liberty -am install -DskipTests` first. |
| Server is in a strange state | `mvn clean` removes everything the plugin created under `target/liberty`; the next `liberty:run` provisions the server again without re-downloading the runtime, which stays in the local Maven repository. |
| Port change has no effect | Do not edit files under `target/liberty` — they are regenerated. Use `-Dliberty.http.port=…`. |

## Not a deployment template

This is a showcase, not a blueprint: it runs H2 in memory, serves plain HTTP without TLS or
authentication, and manages a single server. The deployable artifact of the project is
[`n-sysmon-server-war`](../n-sysmon-server-war).

## Related

- [Project readme](../README.md) — what n-sysmon is.
- [INSTRUCTION.md](../INSTRUCTION.md) — installing n-sysmon into your own application, plus the
  project-wide run instructions for both demo runners.
- [docs/architecture/arc42/architecture.md](../docs/architecture/arc42/architecture.md) — how the
  modules, the agent and the server fit together.
- [`n-sysmon-demo-webapp`](../n-sysmon-demo-webapp) — the shared demo application, including the
  servlet that fabricates the load.

## Url
The final demo-app is reachable at 
http://localhost:9292/nsysmon/