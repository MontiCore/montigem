# 7. Backend Customization

## 7.1 Startup Logic

You can customize the server startup by overriding the `init()` method in your `...ServerApplication` class (HWC). This is the place to create dummy data.

```java
@PostConstruct
@Override
public void init() throws IOException {
  super.init();
  if (CarrentalManager.getCarList().isEmpty()) {
      // Create dummy data
      CarrentalManager.carBuilder().model("Tesla").build();
  }
}
```

## 7.2 Command Implementation

For every service method defined in the class diagram, a Command class is generated. You must implement the `doAction()` method in the HWC class.

```java
public class RentItem extends RentItemTOP {
  @Override
  public SimpleResult<RentalTransaction> doAction() {
    // Business logic here
    return new SimpleResult<>(getId(), result, RentalTransaction.class.getName());
  }
}
```

## 7.3 Custom REST Endpoints

To add custom REST endpoints, create a standard Spring Boot `@RestController` class anywhere in your backend source path.

```java
@RestController
public class MyEndpoint {
  @GetMapping("/api/hello")
  public String sayHello() {
    return "Hello";
  }
}
```

## 7.4 Executable JAR Library Names

The backend Gradle plugin prefixes dependency JAR filenames in `BOOT-INF/lib/` with
their Maven group by default. This keeps distinct libraries with the same artifact
name and version from colliding when `bootJar` packages them.

To retain the original filenames, set this in `gradle.properties`:

```properties
bootJarIncludeGroup=false
```

You can also disable it for a single build with `-PbootJarIncludeGroup=false`.

---
[< Previous: Styling](06_Styling.md) | [Back to Index](../MontiGem_Documentation.md) | [Next: TOP Mechanism >](08_TOPMechanism.md)
