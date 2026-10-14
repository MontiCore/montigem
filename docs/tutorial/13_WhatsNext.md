# What's Next

!!! abstract "In this chapter"
    - a summary of what you built
    - where to look up the things this tutorial only touched
    - which topics to learn next for a real project

Congratulations. Your app models a domain, generates a full-stack web
application from it, has pages with layout and styling, own functions,
navigation, forms, charts calculated in the browser and on the server, an
end-to-end test and input validation.

## Look things up

- [MontiGem documentation](../MontiGem_Documentation.md) is the reference. It
  covers data modeling, GUI modeling, frontend logic, the runtime environment,
  styling, the backend, the TOP mechanism, project setup, testing and
  deployment.
- [GUI component library](../gendoc_components/Frontend-RTE.md) lists every
  `Gem*` component with its parameters. Use it whenever you need a component
  that this tutorial did not show.
- [Helpful links](../HelpfulLinks.md) collects the remaining documents, for
  example about the module system, generating GUIs from class diagrams (CD2GUI)
  and creating initial data from object diagrams (OD2Data).
- [JSweet User Guide](../JSweetUserGuide.md) explains what the Java to
  TypeScript transpilation supports.
- [GUI validity generator internals](../GUIValidityDev.md) goes deeper into the
  OCL validity checking of the last chapter.

## Topics for a real project

The tutorial keeps the setup as simple as possible. A production app usually
needs a few more things:

- **Database.** This app uses an in-memory database, so all data is gone when
  the backend stops. The configuration files are
  `backend/src/main/resources/database.properties` and `application.properties`.
  See the project setup chapter of the reference for PostgreSQL.
- **Authentication and permissions.** This app uses
  `umlp.service.websocket.impl=noAuth`, i.e. no login at all.
- **Initial data.** Dummy data in `initEntities()` is fine for a tutorial. For
  real data sets look at OD2Data.
- **Theming.** The chapter about the GUI layout changed single styles. The theme
  of the whole app can be configured as well.
- **Deployment.** The project contains a GitLab CI configuration that builds a
  Docker image with jib. See the deployment chapter of the reference.

## Getting help

- Compare with the solution of the example project. Every chapter's code is
  marked with `/*SOL start <chapter>*/` and `/*SOL end <chapter>*/`.
- Re-read the table
  [which command after which change](./00_Prerequisites.md#which-command-after-which-change)
  and the troubleshooting table below it. Most problems during development are
  a missing `generate` or a backend that was not restarted.
- Ask the MontiGem team at SE RWTH Aachen.
