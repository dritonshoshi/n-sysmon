# Architecture Documentation

Comprehensive architecture documentation for N-SysMon.

## arc42

The full architecture is documented following the [arc42](https://arc42.org/) standard:

- [arc42 Architecture Documentation](arc42/architecture.md)

That document covers all 12 arc42 sections: introduction and goals, constraints,
context and scope, solution strategy, building block view, runtime view, deployment
view, crosscutting concepts, architecture decisions, quality requirements, risks and
technical debt, and a glossary.

## Code-level documentation

- [README](../../README.md) — how to get the agent into an application
- Javadoc is built for every module during `mvn package`; the HTML lands in
  `<module>/target/site/apidocs`
- Configuration reference: `n-sysmon-core/src/main/resources/nsysmon-default.properties`
  documents every supported key
- Known gaps and open work: `todo.txt` at the repository root

## Quality gates

Enforced during `mvn verify`, and run by CI on JDK 17 and JDK 21:

| Gate | Where | Threshold |
|------|-------|-----------|
| Line coverage | `jacoco-maven-plugin`, per module | ≥ 80% |
| Code style | `maven-checkstyle-plugin`, `config/checkstyle/checkstyle.xml` | 0 violations |
| Supported JDK | `maven-enforcer-plugin` | `[17,26)` |