# 13. Testing

MontiGem supports various testing strategies to ensure the quality of your application.

## 13.1 E2E Testing with Playwright

End-to-End (E2E) tests verify the application flow from the user's perspective. MontiGem integrates with [Playwright](https://playwright.dev/) for robust browser automation.

### Setup
Enable Playwright in your `frontend/build.gradle`:
```groovy
generateTestInfrastructure {
  usePlaywright.set(true)
}
```

### Writing Tests
Tests are written in Java and located in `frontend/src/test/java`. They extend `AbstractPlaywrightE2ETest`.

```java
public class MyE2ETest extends AbstractPlaywrightE2ETest {
  @Test
  public void testLogin() {
    page.navigate("http://localhost:4200");
    page.fill("#username", "admin");
    page.click("#loginButton");
    assertThat(page.locator("#welcomeMessage")).isVisible();
  }
}
```

## 13.2 Simulated GUI Tests

For faster feedback on GUI logic without a full browser, MontiGem generates "Simulated GUI" classes. These allow you to test component interaction (clicks, input) in a pure Java environment.

### Important: `page.init()` and Singletons
In contrast to the real Angular app, where the lifecycle hooks handle initialization, **you must call `page.init()` manually** in your tests (e.g. in `@BeforeEach` or after instantiating the page and setting its parameters). Otherwise, you will likely encounter `NullPointerException`s or incomplete state.

Furthermore, `init()` often automatically fetches required Singletons (like Managers or `App` singletons) from the backend context. Therefore, manual setter calls for singletons (e.g. `page.setApp(...)`) in your test setup are usually redundant.

For more details, see [Testing in MontiGem](../unstructured/Testing.md).

## 13.3 Troubleshooting & Workarounds

### Charts in PDF Exports
When exporting a page containing charts (like ApexCharts or Chart.js) to PDF:
*   **Canvas-based charts:** Convert the canvas to an image first using `myCanvasElement.toDataURL('image/png')` and embed the image in the PDF.
*   **SVG-based charts:** Do not attempt to convert the SVG directly (tools like `html2canvas` often fail here). Instead, manually redraw the chart's geometry within the PDF using the primitive shapes API of your PDF library (e.g., `doc.rect()`, `doc.text()` in `jspdf`).

---
[< Previous: Deployment](12_Deployment.md) | [Back to Index](../MontiGem_Documentation.md) | [Next: Python Client Generator >](14_PythonClient.md)
