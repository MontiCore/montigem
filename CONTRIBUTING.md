# Contributing to MontiGem

Thank you for contributing! This guide explains how we work: how to report problems, how the
repository is organized, how to test and style your changes, and what a pull request has to look
like before it can be merged.

> **Draft.** Sections marked with `TODO` still need input from the maintainers.

---

## 1. Issues

- **Found a bug?** Open an issue, even if you do not plan to fix it yourself.
- **Want a new feature or a larger change?** Open an issue first so the idea can be discussed
  before you spend time on an implementation.
- **Use the issue templates.** When you click **New issue**, GitHub offers the available
  templates. Fill in every section. A bug report without steps to reproduce is hard to act on.
- Search the existing issues before opening a new one to avoid duplicates.

---

## 2. Contributors

| You are … | How you contribute                                                                                                                                                                |
|---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **External contributor** | Work in a **fork** and open a pull request from it. See [Working with a Fork](docs/WorkingWithAFork.md) for a step-by-step guide.                                                 |
| **Student** (thesis, lab, seminar) | Work in a fork in the RWTH GitLab that the admins create for you. The admins bring your work back to GitHub at the end. See [Student Contributions](docs/StudentContribution.md). |
| **Team member** | Create a branch in this repository and open a pull request.                                                                                                                       |

In all cases, **all changes go through pull requests**. Nobody pushes directly to `dev`.

---

## 3. Project structure

MontiGem is a Gradle multi-project build (see [`settings.gradle`](settings.gradle)). The most
important parts:

| Directory | Contents |
|---|---|
| [`umlp-tool/`](umlp-tool/) | The generator: grammars, CoCos, code generators for class diagrams (CD4A/UMLP) and GUI models, plus the Gradle plugin and IDE plugins. Built as an included build. |
| [`umlp-rte/`](umlp-rte/) | The runtime environment the generated code depends on. `backend/` (Java, Spring Boot) and `frontend/` (Angular GUI component library, split into modules such as `arrange`, `basic`, `charts`, `input`, `navigation`, `table`, …). |
| [`umlp-jsweet/`](umlp-jsweet/) | JSweet support for transpiling frontend Java to TypeScript. |
| [`umlp-test-rte/`](umlp-test-rte/) | Test runtime, e.g. the simulated GUI used in end-to-end tests. |
| [`comment2doc/`](comment2doc/) | Generates the component documentation from model comments. |
| [`modules/`](modules/) | Reusable feature modules (login, chat, comment, dashboard, …). |
| [`examples/`](examples/) | Example and integration-test applications, e.g. `vis-lib` (showcase of all GUI components), `it-test`, `jsweet-test`. |
| [`products/`](products/) | Applications built with MontiGem. |
| [`experiments/`](experiments/) | Prototypes and performance experiments. Not part of the stable framework. |
| [`docs/`](docs/) | User documentation, published with MkDocs (navigation in [`mkdocs.yml`](mkdocs.yml)). |

Rule of thumb: **a new GUI component** goes into the matching `umlp-rte/frontend/<module>`, gets a
showcase in `examples/vis-lib`, and is documented through its model comments.

---

## 4. Testing

Every change must be tested. Pick the test level that matches the change:

| Change | Expected tests |
|---|---|
| Generator / CoCo in `umlp-tool` | Unit tests for the generator or CoCo, including a model that **violates** a new CoCo. |
| Backend runtime (`umlp-rte/backend`) | JUnit tests (they run against an in-memory H2 database). Tag long-running tests with `@Tag("SlowTest")`; they are excluded from `gradle test`, run with `gradle slowtest`, and in CI only in the scheduled pipeline (with a PostgreSQL database). |
| GUI component (`umlp-rte/frontend`) | Showcase in `examples/vis-lib`, plus tests there: **unit tests** in TypeScript (Angular `TestBed`, run in a headless browser with Vitest, e.g. `src/test/ts/manual/*.spec.ts`, run with `gradle :examples:vis-lib:frontend:chartLayoutTest`) and/or **end-to-end tests** in Java (e.g. `src/test/java/domain/e2e/`). |
| Feature spanning generator + runtime | An integration test in `examples/` (e.g. `it-test`) that generates, builds and runs a small application. |
| Bug fix | A test that **fails without the fix** and passes with it. |

---

## 5. Coding guidelines

- **Formatting** follows [`.editorconfig`](.editorconfig): UTF-8, LF line endings, 2 spaces
  indentation, 100 characters line length, final newline. Most IDEs pick this up automatically.
- **Java:** no wildcard (`*`) imports, because of JSweet (see the `[*.java]` section in `.editorconfig`; IntelliJ picks this up automatically).
- **Frontend Java** (under `frontend/src/main/java`) is transpiled to TypeScript with JSweet. Only
  use JSweet-compatible parts of the Java standard library.
- **TypeScript:** single quotes.
- **Generated code is never edited.** To customize generated classes, use the TOP mechanism
  (hand-written class next to the generated `...TOP` class).
- **License header:** every source file starts with
  `/* (c) https://github.com/MontiCore/monticore */`.
- **Document** new GUI components and their parameters in the model comments; they become the
  [component library documentation](docs/gendoc_components/Frontend-RTE.md).
- Add a changelog entry for user-visible changes (in the PR description, see below).

---

## 6. Pull requests

### Rules

- **One issue per PR.** Every PR references exactly one issue (`Closes #1234`). Unrelated changes
  go into a separate PR.
- **Clean history.** Rebase on the current `dev` instead of merging `dev` into your branch; no
  "fix typo" or "WIP" commits in the final PR.
- **Title format:** `#1234 Short description`.
- **Fill in the [PR template](.github/pull_request_template.md):** linked issue, changelog
  entries (Added / Changed / Removed / Fixed) and how to test the change.
- **Target branch** is `dev`.
- Open a **draft PR** while the change is still in progress.

### Review

Every PR is reviewed by one of the admins on GitHub. Once your PR is ready (no longer a draft and
the pipeline is green), **assign an admin as reviewer**. The reviewer leaves comments on the PR.
Address them on the same branch and push again, the PR updates automatically. A PR is merged by
the reviewer once it is approved.

---

## 7. CI pipeline

- **On every push to any branch**, a GitHub Action
  ([`trigger_montigem_downstream.yml`](.github/workflows/trigger_montigem_downstream.yml))
  triggers the GitLab pipeline ([`.gitlab-ci.yml`](.gitlab-ci.yml)) and waits for its result.
- The **build stage** runs the full `gradle build` with all tests, collects test reports and code
  coverage, and builds the IDE plugins.
- The **check stage** runs additional checks, e.g. a scan for forbidden method usages in the
  generator.
- **Downstream projects** built with MontiGem are then built against your change to detect
  breaking changes early.
- **On `dev`** only: artifacts are published and containers are deployed
  ([`release_mg.yml`](.github/workflows/release_mg.yml)). Slow tests run in a scheduled pipeline.

A PR can only be merged when the pipeline is green. If it fails, open the job log linked in the
pipeline status.

**External contributors** (working from a fork) cannot see the pipeline results for the downstream projects on GitLab.
In case of a failure, contact an admin, e.g. in a comment on your PR. They check the pipeline for you and share the
relevant parts of the log.
