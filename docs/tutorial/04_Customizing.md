# Customizing

!!! abstract "In this chapter"
    - customize the start-up of the server application
    - create, read, update and delete domain objects from code
    - understand how backend and clients stay synchronized
    - add your own REST endpoint
    - no files to change: this chapter explains the concepts you use from the
      next chapter on

As mentioned for the TOP mechanism, hand-written code adaptations are necessary
for a full-stack application. Here is where and how to adapt the different parts
of the MontiGem-generated code.

## Customizing the server application

MontiGem generates server code to run the backend. The used technology framework
is [Spring Boot](https://spring.io/projects/spring-boot) which provides the
tools for server applications, endpoints, security, and more. The generated code
is adaptable by using the TOP mechanism.

### Start-up

The server application is generated from the UML class diagram. However, some
custom start-up logic might require hand-written adaptations. The entry point
for adapting the application start-up is the `ServerApplication` class, e.g.
`carrental.CarrentalServerApplication`. Here you can override the `init` method
with the Spring Boot `@PostConstruct` annotation. A typical use case for this is
the creation of start-up dummy data to test your product. You already worked
with the `initEntities()` method while learning the
[class diagram basics](./01_ClassDiagramBasics.md#try-it-yourself).

```java title="backend/src/main/java/carrental/CarrentalServerApplication.java"
import jakarta.annotation.PostConstruct;
import java.io.IOException;

public class CarrentalServerApplication extends CarrentalServerApplicationTOP {

  @PostConstruct
  @Override
  public void init() throws IOException {
    super.init();
    initEntities();
  }

  protected void initEntities() {
    // dummy data creation
  }
}
```

## Hand-written code extensions: create, read, update, delete (CRUD)

To handle domain objects a generated singleton class `Manager` is used (e.g.
`carrental.CarrentalManager`). It offers methods to create, retrieve or delete a
domain object. For example, for a domain object `Car`:

* `CarrentalManager.getCarSure(gemId)` returns the object,
* `CarrentalManager.carOpt(gemId)` returns an `Optional<Car>`,
* `CarrentalManager.getCarList()` returns all objects of type `Car`,
* `CarrentalManager.carBuilder()` returns a builder,
* `CarrentalManager.deleteCar(gemId)` deletes the object if present.

The manager is available both in the backend and in the frontend. Both are
synchronized via a WebSocket connection. Note that synchronization is ensured
between all clients and also the database.

!!! note
    Most of the Java code in MontiGem is synchronous, it executes operations
    sequentially. However, the Java code for the GUI which works with domain
    objects is translated to asynchronous TypeScript code for the app frontend.
    Since the frontend is a single process, making requests asynchronous ensures
    that the GUI stays responsive and enables user interaction while data is
    being transferred from the server.

    Example Java:

    ```java
    Car c = CarrentalManager.getCarSure(4);
    // work with the c object
    ```

    Example TypeScript:

    ```ts
    CarrentalManager.getCarSure(4).then((c: Car) => {
      // work with the c object
    });
    ```

Updating objects happens by using the setter methods of the objects. For
example:

```java
Car c = CarrentalManager.getCarSure(4);
c.setModel("DeLorean"); // update the car model
```

Synchronization with the manager and the server, all clients and the database
happens automatically in the background. Each domain object offers methods to
edit attributes and read associations.

## Synchronization

The communication between server and clients is implemented using a command
pattern and WebSockets. For each action, e.g. creating or updating a domain
object, MontiGem generates a command (a Java and TypeScript class) and
corresponding (de)serializers converting objects into JSON. When executing an
action, e.g. `car.setModel("DeLorean")`, these commands are exchanged between
server and clients via a WebSocket endpoint and executed. The logic of a command
can be overridden in its `doAction()` method.

!!! info "Command names"
    Commands generated for an attribute or an operation of a class follow the
    schema `<Class>_<operation>`, for example `Car_setModel`, `Customer_create`
    and `Customer_delete`. Commands generated for a method of a service class
    are named after the method, for example `RentCar`. You will write such a
    command in the [next chapter](./05_ClassDiagramAdvanced.md).

Commands can be sent manually by calling the singleton class `CommandManager`,
e.g.

```java
CommandManager.executeCommand(new Car_setModel(4, "DeLorean"));
```

!!! info "Result types"
    `doAction()` returns a result object:

    - `StatusResult` for actions without a return value, created with
      `StatusResult.ok(getId())` or `StatusResult.error(getId())`,
    - `SimpleResult<T>` for actions that return one object,
    - `ListResult<T>` for actions that return a list.

### Synchronization example

Consider a scenario with a database, a server, and two clients (A and B). All
have loaded the same `Foo` object (ID: 1).

1.  **Initial state:**
    *   Database: `Foo(1).bar = "initial"`
    *   Server: `Foo(1).bar = "initial"`
    *   Client A: `Foo(1).bar = "initial"`
    *   Client B: `Foo(1).bar = "initial"`

2.  **Action:** Client A executes `foo.setBar("updated")`.

3.  **Propagation:**
    *   **Client A:** sends a `Foo_setBar(1, "updated")` command to the server
        via WebSocket.
    *   **Server:** receives the command.
        *   Executes it: updates the in-memory object `Foo(1).bar = "updated"`.
        *   Persists it: updates the database `Foo(1).bar = "updated"`.
        *   Broadcasts: sends the command to all *other* connected clients
            (Client B).
    *   **Client B:** receives `Foo_setBar(1, "updated")`.
        *   Executes it: updates its local object `Foo(1).bar = "updated"`.
        *   The UI (if bound to `foo.bar`) updates automatically via the observer
            pattern.

This ensures that all participants eventually hold the same state.

!!! note "Watching the traffic"
    The backend console logs every command it receives and sends, for example
    `Received: {"commandId":3,"commandType":"carrental.commands.RentCar",...}`.
    This log is the fastest way to check whether a button in the GUI really
    triggered the action you expected.

### Endpoints

Synchronization happens over the command WebSocket endpoint. Spring Boot
provides utilities to add endpoints. You may need extra endpoints to handle
binary data, for example an endpoint to store a PDF file. To do so, add a new
class with the `@RestController` annotation:

```java title="backend/src/main/java/carrental/service/PDFEndpoint.java"
package carrental.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class PDFEndpoint {
  @PostMapping(value = "/pdf")
  public ResponseEntity<String> storePDF(
    @RequestPart("file") MultipartFile file,
    @RequestHeader("name") String name
  ) {
    // your code
    return ResponseEntity.ok("stored " + name);
  }
}
```

!!! warning "The package matters"
    Spring only scans the packages listed in `scanBasePackages` of the
    `@SpringBootApplication` annotation in `CarrentalServerApplication.java`.
    For this project that is `carrental.service` plus the MontiGem runtime
    packages. Put your controller in `carrental.service` (or a subpackage), or
    add your package to `scanBasePackages`. A controller in another package is
    never instantiated and its endpoint returns 404.
