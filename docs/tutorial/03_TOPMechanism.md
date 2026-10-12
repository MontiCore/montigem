# TOP Mechanism

!!! abstract "In this chapter"
    - understand how hand-written code extends generated code
    - learn the naming rules for the different kinds of extensions
    - change the default route of the app as a first small exercise
    - files you touch: `frontend/src/main/gui/AppGemRouting.ts`

Most of the more advanced features of MontiGem involve extending generated code
with hand-written code. Such extensions are mostly done using the so-called
TOP mechanism, which is described in this chapter, as well as its variations.
This chapter mostly goes through the general concept. You will use the TOP
mechanism in the following chapters.

The TOP mechanism integrates hand-written code into generated code using
inheritance. There are several variations of it in a MontiGem app, all of them
follow similar principles.

!!! warning "Never edit generated code"
    Everything under `backend/build/` and `frontend/build/` is regenerated on
    every build. Your changes there are lost without warning. That is why the
    TOP mechanism exists: it gives every generated artifact a place where your
    own code survives regeneration.

    For the same reason, do not delete `frontend/build/app` while the dev server
    is running. It contains `hwc`, a link to your own sources.

## Source roots

The following explanation mentions the *source root*: a set of paths to folders
that MontiGem considers to contain the source code of an app.

The default app setup has source roots for:

- **backend**

    - `backend/build/umlp2java/`: code generated from models
    - `backend/src/main/java`: hand-written code

- **frontend**

    - `frontend/build/app/umlpJava2ts/`, `frontend/build/app/gui2java2ts/`:
      code generated from models
    - `frontend/src/main/java`: hand-written Java code
    - `frontend/src/main/gui`: hand-written TypeScript, HTML and SCSS code, and
      GUI models

- **models**

    - `models`: folder with models

!!! note
    The path to the generated code is normally irrelevant for app development,
    since the paths are easier to find out from the qualified names of classes
    and components.

!!! info "HWC"
    Hand-written code is abbreviated as *HWC* in MontiGem documentation and in
    some folder names.

## TOP for backend Java classes

Backend Java classes can be extended by defining a file with the same path from
the source root as the generated Java file and then extending the generated
class.

Simpler: given a qualified name of a class `foo.bar.Baz`, if you create a file
`foo/bar/Baz.java` in the source root of hand-written backend code, the
generator will produce a `foo/bar/BazTOP.java` file. You can then extend your
`Baz` class with `BazTOP` (`Baz extends BazTOP`) to inherit and override its
functionality. The generator will use your `Baz` class instead of the generated
one.

!!! note
    The `...TOP` class only exists after the generator has run. Create your
    hand-written file first, run the generator (e.g. `./gradlew :backend:umlp2java`),
    and your IDE will resolve the symbols.

!!! note
    If some generated backend service instantiates a `Baz` object to send it to
    the frontend, you can override the instantiation logic using the TOP
    mechanism to send an altered version of the object instead.

## TOP for frontend TypeScript classes from a class diagram

Frontend TypeScript classes generated from a class diagram can be extended by
defining a file with the same path from the source root as the generated
TypeScript file and then extending the generated class.

!!! note
    Basically the same schema as for backend Java classes.

Simpler: given a qualified name of a class `foo.bar.Baz`, if you create a file
`foo/bar/Baz.ts` in the source root of hand-written frontend code, the generator
will produce a `foo/bar/BazTOP.ts` file. You can then extend your `Baz` class
with `BazTOP` (`Baz extends BazTOP`) to inherit and override its functionality.

## TOP with JSweet

MontiGem targets application development completely in Java to enable app
developers to write and execute end-to-end tests as unit tests. For this purpose
the logic normally written in TypeScript is instead written in Java and then
translated by MontiGem to Angular-compatible TypeScript code. The translator is
called JSweet.

Transpiling Java code to TypeScript code means that concepts such as specific
types, methods, statements and expressions from Java have to be mapped to an
equivalent concept in TypeScript. One example is the `Integer` type from Java
which is transpiled to a TypeScript `number`, e.g. whenever you return an
`Integer` in a Java method the TypeScript method transpiled by JSweet returns a
`number`.

!!! warning "Not all Java works in the frontend"
    Frontend Java is transpiled, so a few Java features are not available:
    reflection, threads, most of the Java standard library beyond collections
    and simple types, and static initializers with complex logic. Stick to
    fields, loops, collections, `String` operations and lambdas. If you need
    something else, calculate it in the backend (see the chapter about
    [charts from the backend](./10_GUIAdvanced5.md)).

Frontend Java classes generated from GUI models can be extended by defining a
file with the same path from the source root as the generated Java file, exactly
as for backend Java classes. Additionally, you should add the annotation
`umlp.jsweet.extension.annotation.Component` to the hand-written class:

```java
import umlp.jsweet.extension.annotation.Component;

@Component()
public class SomePage extends SomePageTOP {
}
```

Simpler: given a GUI component `myapp.SomePage`, if you create a file
`myapp/SomePage.java` in the source root of hand-written frontend code, the
generator will produce a `myapp/SomePageTOP.java` file.

**Important:** When overriding the `init()` method in a Java component, always
call `super.init()` first to ensure that the initialization logic of the
generated `...TOP` class is executed. The same applies to `destroy()`.

**Important:** Methods you override must carry `@Override`. Without it a typo in
the method name silently creates a new, unused method.

For more information on JSweet, see the [JSweet User Guide](../JSweetUserGuide.md).

## TOP for frontend TypeScript classes from GUI models

In some cases a hand-written Java component extension is not enough, e.g. when
the component uses a third-party library, like `d3.js`, or when the component
logic requires DOM manipulations.

Frontend TypeScript classes generated from GUI models can be extended by
defining a file with the same path from the source root as the generated
TypeScript file and then extending the generated class with the `Component`
suffix and using the generated `config` constant.

Simpler: given a GUI component `myapp.SomePage`, if you create a file
`myapp/SomePage.ts` in the source root of hand-written frontend code, the
generator will produce `myapp/SomePageTOP.ts` and `myapp/SomePageComponent.ts`
the next time you run `./gradlew :frontend:generate`.
You can then extend your `SomePage` class with `SomePageComponent` to inherit
and override its functionality. Additionally, you need to import and use the
`config` constant from the `myapp/SomePageComponent.ts` file:

```ts
import { SomePageComponent, config } from "myapp/SomePageComponent";
import { ChangeDetectionStrategy, Component } from "@angular/core";

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports],
  selector: config.selector,
  templateUrl: config.templateUrl,
  styles: config.styles,
  styleUrls: config.styleUrls
})
export class SomePage extends SomePageComponent {
}
```

!!! warning "Keep `changeDetection: ChangeDetectionStrategy.Eager`"
    MontiGem apps use Angular 22. Without this line Angular does not update
    the page when the data arrives from the backend. The page then stays empty
    and the browser console shows no error.

!!! note
    The `@Component(...)` annotation is always the same. There is a technical
    limitation imposed by the Ivy compiler (used in the Angular framework) that
    makes it difficult to completely eliminate this code. This issue will be
    resolved in the future.

!!! note
    A hand-written TypeScript file and a hand-written Java file for the same
    component shadow each other. Use one of them, not both.

## TOP for frontend templates (HTML) and stylesheets (SCSS)

Frontend HTML and SCSS files generated from GUI models can be **overwritten** by
defining a file with the same path from the source root as the generated HTML
and SCSS files.

!!! note
    HTML and SCSS do not support inheritance, which is why the generated code
    cannot be reused.

Simpler: given a GUI component `myapp.SomePage`, if you create a file
`myapp/SomePage.html` in the source root of hand-written frontend code, this
file will be used instead of the generated one. The same applies to
`myapp/SomePage.scss`. You will do this in the chapter about the
[GUI layout](./08_GUIAdvanced3.md).

The project already contains an example: the root component of the app,
`AppComponent`, is generated, but `frontend/src/main/gui/AppComponent.html` and
`AppComponent.scss` replace its template and style to show the logo and the
footer.

!!! note
    Most current MontiGem builds do not generate an SCSS file at all, but have a
    few default style definitions specified via `styles: config.styles` of the
    `Component` annotation.

## TOP for frontend routing

Frontend routing files (with the suffix `GemRouting.ts`) generated from a GUI
**package** can be extended by defining a file with the same path from the
source root as the generated routing file.

Simpler: given that there is a GUI page `myapp.SomePage`, if you create a file
`MyappGemRouting.ts` in the source root of hand-written frontend code, this file
will be used instead of the generated one. The file contains a `routes`
constant, that specifies the URL for all pages that reside in the corresponding
package.

There is a global routing file `AppGemRouting.ts` that specifies the routing for
all components. Normally you would only override `AppGemRouting`, as the URL
doesn't really matter to the end user. If you are curious about routing rules,
the full set of rules is specified in the
[Angular guide](https://angular.dev/guide/routing).

### Try it yourself

The project already contains a hand-written `AppGemRouting.ts`. It sends a user
who opens the app without a path to the start page:

```ts title="frontend/src/main/gui/AppGemRouting.ts"
/* (c) https://github.com/MontiCore/monticore */
import { Route } from "@angular/router";
import { routes as routesTOP } from "AppGemRoutingTOP";

export const routes: Route[] = [
  {
    path: "",
    pathMatch: "full",
    redirectTo: "gui/Start"
  },
  ...routesTOP
]
```

Change the value of `redirectTo` to `"gui/SimpleCarList"` and save the file.

`routes` is an array of route objects. You can build your own array (it must be
assigned to the `routes` variable) and reuse the entries of the generated
`routesTOP` array with the spread operator (`...routesTOP`). Angular uses the
first matching route, so your entry wins over the generated ones.

!!! note
    This is hand-written TypeScript inside `frontend/src/main/gui`. The dev
    server watches that folder, so it picks up the change on its own. You do not
    need `generate` here.

## Checkpoint

- Open [http://localhost:4200](http://localhost:4200) without a path. The app
  now redirects to `/gui/SimpleCarList` instead of `/gui/Start`.
- Change `redirectTo` back to `"gui/Start"` if you prefer the start page.
