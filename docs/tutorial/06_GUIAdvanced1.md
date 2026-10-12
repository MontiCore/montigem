# Adding Functions in GUI

!!! abstract "In this chapter"
    - call your own functions from a GUI model
    - implement them in hand-written Java with the TOP mechanism
    - react to a button click and send a command
    - files you touch: `frontend/src/main/gui/gui/CarOverview.gui`,
      `frontend/src/main/java/gui/CarOverview.java`,
      `frontend/src/main/gui/gui/navigation/NavContainer.gui`

Previously we have built a very simple list of cars. Now we will build something
a bit closer to an actual page. Create a new page:

```ts title="frontend/src/main/gui/gui/CarOverview.gui"
package gui;

import mc.fenix.basic.GemText;
import mc.fenix.arrange.GemColumn;

import gui.navigation.NavContainer;

import Carrental.Car;

page CarOverview(List<Car> cars) {
  @NavContainer(content = [
    @GemColumn(components = [

      @GemText(value = getAvailableCarsTitle(cars))

    ])
  ]);
}
```

Add the page to `NavContainer` as well:

```ts title="frontend/src/main/gui/gui/navigation/NavContainer.gui"
@GemNavList(width = "40ch", height = "100vh", components = [
  @GemNavItem(title = "Home", target = "/gui/Start"),
  @GemNavItem(title = "Simple car list", target = "/gui/SimpleCarList"),
  @GemNavItem(title = "Customer overview", target = "/gui/CustomerOverview"),
  @GemNavItem(title = "Car overview", target = "/gui/CarOverview")
]),
```

!!! note "The customer overview has its own navigation bar"
    `CustomerOverview.gui` was prepared before `NavContainer` existed, so it
    has its own `GemNavList`. Add the same `GemNavItem` there as well, after
    the item "Customer overview" (separated by a comma). Otherwise the entry
    disappears when you open the customer overview.

The `@GemText` in `CarOverview` calls a function `getAvailableCarsTitle(cars)`
to get the correct value. The function was never declared in the component, but
the generator assumes that you will implement it and creates an empty function.

!!! note
    You could expect to be able to write something like
    `@GemText(value = getAvailableCarsAmount() + " available cars")`, but this is
    not allowed. The generator is unable to process more complex expressions
    containing method calls: a method call has to be the whole argument.

!!! warning "Pass what the function depends on"
    The generated TypeScript re-evaluates such a function when one of its
    arguments changes. That is why we pass `cars` as a parameter. A function
    without parameters is evaluated once and its result stays on the screen even
    when the data changes.

    The re-evaluation reacts to the argument itself, here the list of cars. A
    change of a single attribute of one car does not trigger it. Values that
    have to follow such a change are read directly in the GUI model, as you will
    see in the [chapter about the layout](./08_GUIAdvanced3.md) with
    `c.rentableStatus`.

## Implementing the function

Let's add the function to `CarOverview.java`. The file does not exist yet.
Create it in `frontend/src/main/java/gui/`. We apply the TOP mechanism and
override the empty function to show the number of available cars:

```java title="frontend/src/main/java/gui/CarOverview.java"
package gui;

import carrental.Car;
import carrental.RentableStatus;
import umlp.jsweet.extension.annotation.Component;

import java.util.List;

@Component()
public class CarOverview extends CarOverviewTOP {

  @Override
  public String getAvailableCarsTitle(List<Car> cars) {
    if (cars == null) {
      return "";
    }
    int i = 0;
    for (Car c : cars) {
      if (c.getRentableStatus() == RentableStatus.AVAILABLE) {
        i++;
      }
    }
    return i + " available cars";
  }
}
```

!!! note
    Just like backend Java files (e.g. `CarrentalServerApplication.java`),
    frontend Java classes are placed in the
    `frontend/src/main/java/<packageName>/` directory. In this case, the only
    package used in this tutorial is `gui`.

The method `getAvailableCarsTitle` checks whether the cars are loaded already,
counts the available cars and returns a string indicating the amount, e.g.
`3 available cars`.

!!! warning "Always check for null"
    The page renders before the data has arrived from the backend. Any function
    that works with domain objects has to handle `null`, otherwise the browser
    throws an exception and the page stays empty.

!!! question "Where does the `cars` parameter come from?"
    The generator produces methods that are mapped from variables and attributes
    in the class diagram and the GUI component model. Getters and setters are
    generated for most class diagram attributes and GUI component parameters.

Some useful generated methods:

- Each GUI component parameter has a getter, for example, in
  `page CarOverview(List<Car> cars)` the `cars` parameter is transformed into

    - a `getCars` method,
    - an `initCars` method,
    - a `setCars` method,
    - a `_cars` attribute,
    - a `cars` pseudo-attribute (only in a TypeScript file).

- Each class attribute has a getter, for example, in
  `class Car { RentableStatus rentableStatus; }` the `rentableStatus` attribute
  is transformed into

    - a `getRentableStatus` method,
    - a `setRentableStatus` method.

    !!! note
        For `boolean` type attributes an `is` prefix is generated. For example,
        if the attribute `boolean available` was defined, an `isAvailable`
        method would be generated instead of `getAvailable`.

- If an attribute is a list or is described as an association with `*`
  cardinality, additional methods can be used, for example, a `Customer` has an
  association to `RentalTransaction`:
  `association [*] RentalTransaction <-> Customer [1]`. For the `Customer` class
  the following methods are generated:

    - an `addRentalTransaction` method,
    - a `removeRentalTransaction` method,
    - a `getRentalTransactionList` method,
    - a `sizeRentalTransactions` method.

Apply your changes:

```sh
./gradlew :frontend:generate
```

The TypeScript variant of `getAvailableCarsTitle` is re-evaluated at application
runtime when any of its arguments, such as `cars`, changes, for example when a
car is created or deleted. If the evaluation result is different from the last
method call, the corresponding GUI part is automatically updated.

## Calling a service

Let's extend the page to show cars and a button for renting them:

```ts title="frontend/src/main/gui/gui/CarOverview.gui"
package gui;

import mc.fenix.basic.GemText;
import mc.fenix.basic.GemButton;
import mc.fenix.arrange.GemColumn;

import gui.navigation.NavContainer;

import Carrental.Car;

page CarOverview(List<Car> cars) {
  @NavContainer(content = [
    @GemColumn(components = [

      @GemText(value = getAvailableCarsTitle(cars)),
      @iterate(Car c : cars) {
        @GemText(value = c.make + " " + c.model);
        @GemButton(label = "Rent car", leftClick = rentCar(c));
      }

    ])
  ]);
}
```

Here we iterate through the cars and add a button for each car. `rentCar` is a
custom function similar to `getAvailableCarsTitle`, but it has to return a
`Consumer<Void>` (the TypeScript equivalent would be the `Event<void>` type).
The generated code is:

```java
public Consumer<Void> rentCar(Car val) {
  return null;
}
```

The generator finds out what a function should return based on the context.
Here, `leftClick` is an event that doesn't provide any value, thus the type is
`Consumer<Void>`.

!!! note
    The button could, for example, provide the id of a clicked button as a
    string, in which case the type would be `Consumer<String>`.

The `Consumer<Void>` type indicates a callback function that should be called
when an event triggers, e.g. when a left click happens. This means that the
function `rentCar` has to return a function. For example, we want to call a
function that changes the status of the car from available to rented when the
button is clicked. Thus, we implement the following method:

```java title="frontend/src/main/java/gui/CarOverview.java"
import java.util.function.Consumer;
/* other imports */

public class CarOverview extends CarOverviewTOP {
  /* ... */

  @Override
  public Consumer<Void> rentCar(Car car) {
    return x -> {
      car.setRentableStatus(RentableStatus.RENTED);
    };
  }
}
```

The syntax `x -> { /*...*/ }` indicates a lambda function. In TypeScript this
would be an
[arrow function](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Functions/Arrow_functions).

We could stop there, but renting a car doesn't only mean changing its status, it
also means that the car is reserved for a customer, a customer transaction is
created, etc. The whole logic could be defined in the `rentCar` function, but a
better option is to use a service that executes the necessary actions.
[Previously](./05_ClassDiagramAdvanced.md) we defined such a service. We can use
it by executing the corresponding command via the `CommandManager`:

```java title="frontend/src/main/java/gui/CarOverview.java"
import carrental.commands.RentCar;
import umlp.backendrte.command.CommandManager;
/* other imports */

public class CarOverview extends CarOverviewTOP {
  /* ... */

  @Override
  public Consumer<Void> rentCar(Car car) {
    return x -> {
      CommandManager.executeCommand(new RentCar(car.getGemId()));
    };
  }
}
```

Apply your changes again with `./gradlew :frontend:generate`.

### Event handling best practices

Currently, method overloading is not supported for event handler methods in the
GUI DSL (this might change in future versions). Instead of:

```ts
@GemButton(leftClick = doSomething("A"));
@GemButton(leftClick = doSomething(12));
```

use unique method names in the GUI DSL and delegate to a common implementation
in Java:

```ts
@GemButton(leftClick = doThingA());
@GemButton(leftClick = doThingB());
```

```java
public Consumer<Void> doThingA() { return v -> doSomething("A"); }
public Consumer<Void> doThingB() { return v -> doSomething(12); }

private void doSomething(String param) { /* ... */ }
private void doSomething(int param) { /* ... */ }
```

## Checkpoint

- [http://localhost:4200/gui/CarOverview](http://localhost:4200/gui/CarOverview)
  shows `3 available cars` and one "Rent car" button per car.
- Click "Rent car" on the first car. The backend console logs a line containing
  `commandType":"carrental.commands.RentCar`.
- Reload the page: the title now shows `2 available cars`, because the status of
  that car is `RENTED` in the backend.
