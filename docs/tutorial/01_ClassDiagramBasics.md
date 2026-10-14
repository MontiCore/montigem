# Class Diagram Basics

!!! abstract "In this chapter"
    - model the data structure of the app with a class diagram
    - add a class, an enum and an association
    - create, read, update and delete objects in Java
    - files you touch: `models/Carrental.umlp`,
      `backend/src/main/java/carrental/CarrentalServerApplication.java`

The core of a MontiGem app is a data structure modeled as a class diagram. The
basic one requires few elements to get started:

- class diagram
- class
- enum
- association

!!! note
    A complete list will be discussed later, you may also take a look at the
    [lecture slides](https://www.se-rwth.de/sle_material/SLE01.3.CD4Analysis/) and
    the [CD4Analysis project](https://github.com/MontiCore/cd4analysis).

## Class diagram

MontiGem uses a single class diagram as the source of an app's data structure.
The rough structure of a class diagram is:

```java
package mypackage.name;

import external.TypeName;
import another.external.TypeName2;

umlp ClassDiagramName {
  /* classes, associations, etc. */
}
```

It contains

- an optional single package declaration,
- an optional list of imports,
- a single class diagram declaration with the keyword `umlp` (or `classdiagram`),
- the contents of the class diagram, e.g. classes, enums and associations.

!!! note
    There is no difference between using `umlp` and `classdiagram` as of yet,
    but prefer `umlp` for describing the app's data structure.

The car rental example already has such a diagram:

```java title="models/Carrental.umlp"
import java.time.LocalDateTime;

umlp Carrental {
  /* contents */
}
```

### What the generator makes of it

!!! note
    The following information about generated classes is useful for
    understanding the app when you want to override the default behavior.

A class diagram defines the base of the app. The generated files can be found in
`backend/build/umlp2java` and `frontend/build/app/umlpJava2ts`. Both have very
similar implementations. Generated classes include:

* ServerApplication: `CarrentalServerApplicationTOP`, used as the main class for
  running the server. It is also a common place for dummy data initialization.

!!! note
    The class name has a `TOP` suffix, because it is a generated file that is
    extended via hand-written code. You will learn about hand-written extensions
    in the chapter about the [TOP mechanism](./03_TOPMechanism.md).

* Manager: `CarrentalManager`, used to get builder classes, load data, and for
  internal data management.
* Manager mock: `CarrentalMock`, used in testing as a substitute for the real
  `CarrentalManager`.

Having an empty class diagram is not really meaningful, but it plays a certain
role in the generated code as will be explained further.

!!! note
    The generator runs as part of the already mentioned commands `./gradlew build`,
    `./gradlew :backend:run` and `./gradlew :frontend:run`.

## Class

A class specifies a type for an instantiable entity. Its rough structure is:

```java
class Name {
  Type1 attrName1;
  Type2 attrName2;
}
```

It contains

- a single class declaration with the keyword `class`,
- an optional list of attributes.

The car rental example already contains a single class:

```java title="models/Carrental.umlp"
class Customer {
  String name;
  String email;
  int age;
}
```

`Customer` declares two attributes of type `String` and one of type `int`.

Specifying classes and running the generator produces a bunch of code that is
used for entity management:

* Domain classes: `Customer` represents the domain object.
* Builder classes: `CustomerBuilder` is used to build (i.e. instantiate) domain
  objects.
* Data Access Object (DAO): `CustomerDAO` is used to get and set data from and
  to the database.
* (De)serializer: `CustomerDeSer` (de)serializes the domain objects for the
  transfer between backend and frontend (and other services).
* Observer: `CustomerObserver` is used to observe and immediately react to
  changes in the domain objects.
* Commands: `Customer_create`, `Customer_delete` and others execute the
  corresponding operations.
* Other: there are many classes which are used internally or as a supplement for
  the described infrastructure that are not yet relevant for this project.

The most relevant classes are the domain and the builder classes. Those are used
to create and update domain objects.

!!! info "gemId"
    Every domain object has a unique identifier, its *gemId*. You read it with
    `getGemId()`. Commands, the database and the frontend all refer to objects
    by this id.

### Try it yourself

Open `backend/src/main/java/carrental/CarrentalServerApplication.java` and find
the `initEntities()` method. It is empty and marked with a `// TODO` comment.
Add the following code there.

1. Create objects using a builder:

   ```java title="backend/src/main/java/carrental/CarrentalServerApplication.java"
   Customer jim = CarrentalManager.customerBuilder() // returns CustomerBuilder
       .name("Jim Douglas")
       .email("incorrect_mail@example.com")
       .age(34)
       .build() // returns Optional<Customer>
       .get();
   Customer dummy = CarrentalManager.customerBuilder()
       .name("Delete me")
       .email("delete@me.mail")
       .age(33)
       .build()
       .get();
   ```

2. Read object attributes using `get` methods:

   ```java title="backend/src/main/java/carrental/CarrentalServerApplication.java"
   jim.getName();
   ```

3. Update object attributes using `set` methods:

   ```java title="backend/src/main/java/carrental/CarrentalServerApplication.java"
   jim.setEmail("jim.douglas@example.com");
   ```

4. Delete objects using the `delete` methods of the manager:

   ```java title="backend/src/main/java/carrental/CarrentalServerApplication.java"
   CarrentalManager.deleteCustomer(dummy.getGemId());
   ```

!!! warning "build() can fail"
    `build()` returns an `Optional`. It is empty if a mandatory attribute is
    missing. `get()` then throws. In this tutorial all attributes are set, so
    `get()` is safe here.

Now restart the backend: press `ctrl+c` in its console and run
`./gradlew :backend:run` again. This command first generates the Java code from
your class diagram and then starts the backend. The frontend can keep running.

The tutorial application has a page displaying a list of customers. Navigate to
the predefined page
[http://localhost:4200/gui/CustomerOverview](http://localhost:4200/gui/CustomerOverview).
It shows Jim Douglas with the corrected email address, and it does not show
"Delete me".

!!! note "Where does this page come from?"
    `CustomerOverview` and `CustomerCreate` are prepared for you, because they
    need concepts from later chapters. You will look at their models in the
    chapters about [testing](./11_E2ETestingWithSimulatedGUI.md) and
    [OCL validity checking](./12_OCLGUIValidityChecking.md).

## Enum

An enum specifies a set of named constants.

```java
enum Name {
  CONSTANT_1,
  CONSTANT_2;
}
```

It contains

- a single enum declaration with the keyword `enum`,
- an optional list of constants.

Add the following enum to the class diagram of your car rental example:

```java title="models/Carrental.umlp"
enum RentableStatus {
  AVAILABLE,
  RENTED,
  MAINTENANCE;
}
```

Unlike a class, an enum always has one instance, and its values are accessed
directly:

```java
RentableStatus.AVAILABLE
```

## Associations

Associations describe a "has a" relation between domain objects. The rough
structure is:

```java
association [cardinality1] ClassName1 (roleName1) <-> (roleName2) ClassName2 [cardinality2];
```

The association has a left and a right side, separated by an arrow:

- the cardinality defines how many items can be in the association, typical
  values are `[*]` (0 or more), `[1]`, `[0..1]` (0 or 1), `[1..*]` (1 or more),
- the class name,
- the role name with which the associated objects are referenced,
- the direction that defines which side of the association is the owner in the
  "has a" relation, typical values are `<-`, `->` and `<->` (goes both ways).

Let's first add another class to the car rental class diagram, which we will use
to create an association:

```java title="models/Carrental.umlp"
class RentalTransaction {
  LocalDateTime rentalDate;
  LocalDateTime returnDate;
  float totalCost;
}
```

A `RentalTransaction` is created when a customer rents an item, e.g. a car. In
other words, a customer *has* (performs) rental transactions if the customer
rents one or several cars.

Add the association to the car rental example:

```java title="models/Carrental.umlp"
association [*] RentalTransaction <-> Customer [1];
```

This association means that

- a rental transaction has exactly one associated customer,
- a customer may have 0, 1 or several rental transactions.

The above association can be described differently:

```java
association [*] RentalTransaction (rentalTransaction) <-> (customer) Customer [1];
```

Here the role names are set explicitly.

Role names are automatically derived from class names if not specified. The
cardinalities also have default values, however, it is recommended to specify
them. Defining associations as class attributes is possible, but it must be a
calculated decision. It is common, however, to define an enum-typed attribute,
as an enum cannot (shouldn't) have associated objects.

Associations are tricky at times. Consider similar but slightly different
definitions:

```java
association [*] RentalTransaction <- Customer [1]; // a customer has rental transactions
association [*] RentalTransaction -> Customer [1]; // a rental transaction has a customer
```

or

```java
class Customer {
  /* attributes */
  List<RentalTransaction> rentalTransaction;
}

class RentalTransaction {
  /* attributes */
  Customer customer;
}
```

The difference is subtle but important: the associations in these cases show two
different relations. For example, the association

```java
association [*] RentalTransaction <- Customer [1];
```

could mean that the customer performs transactions, and the other association

```java
association [*] RentalTransaction -> Customer [1];
```

could mean that the transaction refers to the customer that rents something to
other customers. If the intent was to specify that a `RentalTransaction` has a
creator `Customer`, defining two associations is a mistake. Use a single
bidirectional association instead.

After changing the class diagram, restart the backend and update the frontend:

```sh
./gradlew :frontend:generate
```

## Attributes and types

Previously we showed several example classes with attributes:

```java title="models/Carrental.umlp"
class Customer {
  String name;
  String email;
  int age;
}

class RentalTransaction {
  LocalDateTime rentalDate;
  LocalDateTime returnDate;
  float totalCost;
}
```

A class diagram has access to different types:

- `String`, Java primitive types (`boolean`, `int`, `float`, etc.), and their
  boxed counterparts (`Boolean`, `Integer`, etc.),
- types defined in the class diagram (`Customer customer`),
- imported Java types, e.g. via `import java.time.LocalDateTime;`.

There are, however, limitations to what types can be used. The biggest one is
that generic types are not supported, other than `java.util.List` and
`java.util.Optional`. The supported types are listed in the
[data modeling reference](../reference/02_DataModeling.md).

## Checkpoint

- The backend restarts without errors after your model changes.
- [http://localhost:4200/gui/CustomerOverview](http://localhost:4200/gui/CustomerOverview)
  shows one customer: Jim Douglas, `jim.douglas@example.com`, age 34.
- `backend/build/umlp2java/carrental/` contains `RentalTransaction.java` and
  `RentableStatus.java`.
