# Charts Backend

!!! abstract "In this chapter"
    - move a chart calculation from the browser to the server
    - add a service that returns a value instead of `void`
    - learn when to calculate where
    - files you touch: `models/Carrental.umlp`,
      `frontend/src/main/gui/gui/Dashboard.gui`,
      `backend/src/main/java/carrental/commands/GetMileageDataIncrease.java`

Sometimes we do not want to load all data from the backend into the frontend
just to calculate a chart from it. In this case we can add a service task as
introduced in the chapter about
[services](./05_ClassDiagramAdvanced.md).

In the following example we implement the same functionality as before, but this
time in the backend instead of on the client side. The advantage is that it is
much faster for large amounts of data, since only the result travels over the
network. The disadvantage is that the result is not necessarily updated
automatically when the underlying data changes.

## A service that returns a value

Add a singleton with a method that returns the chart data. You have to import
`GemLineChartData` into the class diagram. Imports of chart types defined in the
frontend runtime follow the schema
`mc.fenix.charts.${ChartName}Types.${ChartData}`:

```java title="models/Carrental.umlp"
import mc.fenix.charts.GemLineChartTypes.GemLineChartData;

umlp Carrental {

  <<singleton>>
  class App {
    GemLineChartData getMileageDataIncrease(List<Car> cars);
  }

}
```

We can include this service directly in our dashboard page. The singleton is a
domain object, so it is loaded like any other one: declare it as a page
parameter.

```ts title="frontend/src/main/gui/gui/Dashboard.gui"
import Carrental.App;
/* other imports */

page Dashboard(List<Car> cars, App app) {
  @NavContainer(content = [
    @GemRow(wrap = "wrap", rowGap = "10px", colGap = "10px", hAlign = "space-evenly", components = [

      /* the three cards from the previous chapter */

      @GemCard(
        width = "45%",
        height = "400px",
        title = "Mileage increase over time (calculated in the backend)",
        component = @GemLineChart(data = app.getMileageDataIncrease(cars))
      )

    ])
  ]);
}
```

Calling `app.getMileageDataIncrease(cars)` in the GUI sends the command
`GetMileageDataIncrease` to the backend. The command does not transport the car
objects. It transports their ids, so its constructor takes a `List<Long>`. You
read them with `getCarsGemIdList()`.

## Implementing the service in the backend

We still have to implement the service in the Java backend. Create the file
`backend/src/main/java/carrental/commands/GetMileageDataIncrease.java`:

```java title="backend/src/main/java/carrental/commands/GetMileageDataIncrease.java"
package carrental.commands;

import carrental.Car;
import carrental.CarrentalManager;
import carrental.MileageDataPoint;

import mc.fenix.charts.gemlinecharttypes.GemLineChartData;
import mc.fenix.charts.gemlinecharttypes.GemLineChartDataBuilder;
import mc.fenix.charts.gemlinecharttypes.GemLineChartEntry;
import mc.fenix.charts.gemlinecharttypes.GemLineChartEntryBuilder;

import umlp.backendrte.command.SimpleResult;
import umlp.backendrte.common.LoadingState;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GetMileageDataIncrease extends GetMileageDataIncreaseTOP {

  public GetMileageDataIncrease(List<Long> cars) {
    super(cars);
  }

  @Override
  public SimpleResult<GemLineChartData> doAction() {
    Optional<GemLineChartData> res = calculateMileageData(this.getCarsGemIdList());
    if (res.isEmpty()) {
      return new SimpleResult<>(getId(), LoadingState.FAILED_TO_LOAD, GemLineChartData.class.getName());
    }
    return new SimpleResult<GemLineChartData>(getId(), res.get(), GemLineChartData.class.getName());
  }

  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  protected Optional<GemLineChartData> calculateMileageData(List<Long> carIds) {
    List<Car> cars = new ArrayList<>();
    for (Long carId : carIds) {
      Optional<Car> car = CarrentalManager.carOpt(carId);
      if (car.isPresent()) {
        cars.add(car.get());
      }
    }
    if (cars.isEmpty()) {
      return Optional.empty();
    }

    /* the same calculation as in Dashboard.java of the previous chapter */

    GemLineChartData data = new GemLineChartDataBuilder().entries(entries).labels(labels).build().get();
    return Optional.of(data);
  }

  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  public void interpolateData(List<Double> stat, int interpolatedIndex) {
    /* the same interpolation as in Dashboard.java of the previous chapter */
  }
}
```

Note the differences to the frontend variant:

- `doAction()` returns a `SimpleResult<GemLineChartData>`, because the service
  method has a return value. A method returning `void` uses a `StatusResult`, as
  in the chapter about [services](./05_ClassDiagramAdvanced.md).
- `LoadingState.FAILED_TO_LOAD` tells the client that no data could be produced.
  The chart then shows its empty state.
- The command receives ids and loads the objects itself with
  `CarrentalManager.carOpt(...)`.

!!! info "@BackendOnly"
    A command class exists in the backend **and** in the frontend, so JSweet
    transpiles it. The `@BackendOnly` annotation tells JSweet not to transpile
    the body of a method. This is helpful when the body uses Java that has no
    TypeScript equivalent, and when the code only ever runs in the backend
    anyway. The method header is still transpiled.

    If the transpilation of a method causes problems even then, there is also
    `jsweet.lang.Erased`, which removes the method from the transpiled code
    completely.

You changed the class diagram, so restart the backend and update the frontend:

```sh
./gradlew :frontend:generate
```

## Frontend or backend?

| | Frontend (chapter 09) | Backend (this chapter) |
|---|---|---|
| Where the data lives | all objects are loaded into the browser | only the result is transferred |
| Updates | when the list of objects changes | only when the command is executed again |
| Good for | small data sets, values that change while the user watches | large data sets, expensive calculations, data the user may not see in full |

## Checkpoint

- The dashboard shows a fourth card whose line chart looks like the one of
  chapter 09.
- The backend console logs a line containing
  `commandType":"carrental.commands.GetMileageDataIncrease`.
- Reload the dashboard. The backend console logs a new `GetMileageDataIncrease`
  line, because the backend chart is calculated again on every page load.
