# Adding Services to Class Diagrams

!!! abstract "In this chapter"
    - add a service to the class diagram with the `<<singleton>>` stereotype
    - implement the commands that MontiGem generates for the service methods
    - files you touch: `models/Carrental.umlp`,
      `backend/src/main/java/carrental/commands/RentCar.java`,
      `ReturnCar.java`, `ProcessPayment.java`

Besides managing data an app may do other tasks, such as sending e-mails or
providing user authentication. Those tasks are typically managed through one or
several services that you can introduce via the class diagram. A (static)
service is a class annotated with the `<<singleton>>` stereotype: exactly one
instance of it exists in the running app.

Add the service to the class diagram:

```java title="models/Carrental.umlp"
umlp Carrental {
  /* other classes */

  <<singleton>>
  class RentalService {
    void rentCar(Car rentedCar);
    void returnCar(Car rentedCar);
    void processPayment(RentalTransaction rentalTransaction);
  }
}
```

Let's also add an attribute of type `RentableStatus` to the `Car` class. The
service uses it in the next step:

```java title="models/Carrental.umlp"
umlp Carrental {
  /* other classes */

  class Car {
    /* other attributes */
    RentableStatus rentableStatus = RentableStatus.AVAILABLE;
  }
}
```

## Every service method needs a command

For each method of a singleton class, MontiGem generates a command class. The
generated part is called `<MethodName>TOP`. The part that does the work is
hand-written, as described in the chapter about the
[TOP mechanism](./03_TOPMechanism.md).

!!! warning "Write the classes before you generate"
    MontiGem does not create a default implementation for you. If a service
    method has no hand-written command class, the generator stops with

    ```
    0xC0FFEE00: Custom implementation of command class ... required
    ```

    Our service has three methods, so we write all three classes now and
    generate afterwards.

The directory `backend/src/main/java/carrental/commands/` does not exist yet.
Create it together with the files.

### RentCar

The generated `RentCarTOP` stores the id of the car that was passed to
`rentCar(Car rentedCar)`. You read it with `getRentedCarGemId()`. Together with
`CarrentalManager.getCarSure(gemId)` from the
[previous chapter](./04_Customizing.md#hand-written-code-extensions-create-read-update-delete-crud)
you have everything you need. Try to implement `doAction()` yourself:

```java title="backend/src/main/java/carrental/commands/RentCar.java"
package carrental.commands;

import carrental.Car;
import carrental.CarrentalManager;
import carrental.RentableStatus;
import umlp.backendrte.command.StatusResult;

public class RentCar extends RentCarTOP {

  public RentCar(long rentedCarGemId) {
    super(rentedCarGemId);
  }

  @Override
  public StatusResult doAction() {
    // TODO: change the status of the rented car
    return StatusResult.ok(getId());
  }
}
```

??? example "Solution"
    ```java title="backend/src/main/java/carrental/commands/RentCar.java"
    @Override
    public StatusResult doAction() {
      Car c = CarrentalManager.getCarSure(getRentedCarGemId());
      c.setRentableStatus(RentableStatus.RENTED);
      return StatusResult.ok(getId());
    }
    ```

`doAction()` returns a `StatusResult`, because `rentCar` returns `void`. Methods
with a return value use `SimpleResult<T>` instead. You will see that in the
chapter about [charts from the backend](./10_GUIAdvanced5.md).

### ReturnCar

Returning a car makes it available again. The structure is the same as for
`RentCar`:

```java title="backend/src/main/java/carrental/commands/ReturnCar.java"
package carrental.commands;

import carrental.Car;
import carrental.CarrentalManager;
import carrental.RentableStatus;
import umlp.backendrte.command.StatusResult;

public class ReturnCar extends ReturnCarTOP {

  public ReturnCar(long rentedCarGemId) {
    super(rentedCarGemId);
  }

  @Override
  public StatusResult doAction() {
    // TODO: make the car available again
    return StatusResult.ok(getId());
  }
}
```

??? example "Solution"
    ```java title="backend/src/main/java/carrental/commands/ReturnCar.java"
    @Override
    public StatusResult doAction() {
      Car c = CarrentalManager.getCarSure(getRentedCarGemId());
      c.setRentableStatus(RentableStatus.AVAILABLE);
      return StatusResult.ok(getId());
    }
    ```

### ProcessPayment

Processing a payment needs a payment provider, which is out of scope for this
tutorial. The class still has to exist. It returns an error result to say that
nothing happened:

```java title="backend/src/main/java/carrental/commands/ProcessPayment.java"
package carrental.commands;

import umlp.backendrte.command.StatusResult;

public class ProcessPayment extends ProcessPaymentTOP {

  public ProcessPayment(long rentalTransactionGemId) {
    super(rentalTransactionGemId);
  }

  @Override
  public StatusResult doAction() {
    // payment is not part of the tutorial
    return StatusResult.error(getId());
  }
}
```

## Generating the code

Now that all three classes exist, generate the backend code:

```sh
./gradlew :backend:umlp2java
```

!!! note
    This command only generates. It does not start the server. Your IDE needs
    the generated `...TOP` classes to resolve the symbols in your new files.

The singleton can then be used in the frontend and backend code. The `RentCar`
command is sent using the `CommandManager`:

```java
CommandManager.executeCommand(new RentCar(car.getGemId()));
```

We will come back to this in the [next chapter](./06_GUIAdvanced1.md), where a
button in the GUI triggers it.

You changed the class diagram, so restart the backend and update the frontend:

```sh
./gradlew :frontend:generate
```

## Checkpoint

- `./gradlew :backend:umlp2java` finishes with `BUILD SUCCESSFUL` and
  `backend/build/umlp2java/carrental/commands/RentCarTOP.java` exists.
- The backend restarts without compile errors.
- If you see `0xC0FFEE00`, one of the three command classes is missing, or its
  class name does not match the method name of the service.
