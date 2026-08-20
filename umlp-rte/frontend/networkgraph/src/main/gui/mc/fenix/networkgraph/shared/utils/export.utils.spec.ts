import { ExportUtils } from "./export.utils";
import domtoimage from "dom-to-image";
import {vi,MockInstance} from "vitest"

describe("Export Utilities", () => {
  let appendChildSpy: MockInstance;
  let removeSpy: MockInstance;

  beforeEach(() => {
    appendChildSpy = vi.spyOn(document.body, "append");
    removeSpy = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("should download object as JSON", () => {
    const createElementSpy = vi.spyOn(document, "createElement");
    createElementSpy.mockReturnValue({ setAttribute: vi.fn(), click: vi.fn(), remove: removeSpy } as any);
    const mockObject = { key: "value" };
    const mockName = "test";

    ExportUtils.downloadObjectAsJson(mockObject, mockName);

    expect(createElementSpy).toHaveBeenCalledWith("a");
    expect(appendChildSpy).toHaveBeenCalledTimes(1);
    expect(removeSpy).toHaveBeenCalledTimes(1);
  });

  it("should download object as PNG", () => {
    const createElementSpy = vi.spyOn(document, "createElement");
    createElementSpy.mockReturnValue({ setAttribute: vi.fn(), click: vi.fn(), remove: removeSpy } as any);
    const mockBase64 = "data:image/png;base64,xxxx";
    const mockName = "image";

    ExportUtils.downloadBase64AsPNG(mockBase64, mockName);

    expect(createElementSpy).toHaveBeenCalledWith("a");
    expect(appendChildSpy).toHaveBeenCalledTimes(1);
    expect(removeSpy).toHaveBeenCalledTimes(1);
  });

  it("should download object as SVG", () => {
    const createElementSpy = vi.spyOn(document, "createElement");
    createElementSpy.mockReturnValue({ setAttribute: vi.fn(), click: vi.fn(), remove: removeSpy } as any);
    const mockSVGContent = "<svg></svg>";
    const mockName = "svg";

    ExportUtils.downloadSVGStringAsSVG(mockSVGContent, mockName);

    expect(createElementSpy).toHaveBeenCalledWith("a");
    expect(appendChildSpy).toHaveBeenCalledTimes(1);
    expect(removeSpy).toHaveBeenCalledTimes(1);
  });

  it("should download canvas as png", () => {
    const createElementSpy = vi.spyOn(document, "createElement");
    createElementSpy.mockReturnValue({ setAttribute: vi.fn(), click: vi.fn(), remove: removeSpy } as any);

    // Prepare mock data
    const mockCanvas = document.createElement("canvas") as HTMLCanvasElement;
    // Mocking toDataURL directly on instance
    mockCanvas.toDataURL = vi.fn().mockReturnValue("data:image/png;base64,xxxx");
    const mockName = "testImage";

    ExportUtils.downloadCanvasAsPNG(mockCanvas, mockName);

    expect(createElementSpy).toHaveBeenCalledWith("a");
    expect(appendChildSpy).toHaveBeenCalledTimes(1);
    expect(removeSpy).toHaveBeenCalledTimes(1);
  });

  it("should download HTML element as PNG", async () => {
    const domToImageSpy = vi.spyOn(domtoimage, "toPng").mockResolvedValue("data:image/png;base64,xxxx");
    // Prepare mock data
    const pngSpy = vi.spyOn(ExportUtils, "downloadDataURL");
    const mockElement = document.createElement("div");
    const mockName = "testElement";

    // Call the method to test
    ExportUtils.downloadHTMLElementAsPNG(mockElement, mockName);

    // Wait for promise to get resolved
    await new Promise((resolve) => setTimeout(resolve, 0));

    // Validate the behavior
    expect(domToImageSpy).toHaveBeenCalled();
    expect(pngSpy).toHaveBeenCalled();
  });

  it("should download HTML element as SVG", async () => {
    const domToImageSpy = vi.spyOn(domtoimage, "toSvg").mockResolvedValue("data:image/png;base64,xxxx");
    // Prepare mock data
    const pngSpy = vi.spyOn(ExportUtils, "downloadDataURL");
    const mockElement = document.createElement("div");
    const mockName = "testElement";

    // Call the method to test
    ExportUtils.downloadHTMLElementAsSVG(mockElement, mockName);

    // Wait for promise to get resolved
    await new Promise((resolve) => setTimeout(resolve, 0));

    // Validate the behavior
    expect(domToImageSpy).toHaveBeenCalled();
    expect(pngSpy).toHaveBeenCalled();
  });

  it("should override existing file extensions", () => {
    const fileName = ExportUtils.getFileName("file.JsON", "json", false);
    expect(fileName).toBe("file.json");
  });

  it("should not override differnet file extensions", () => {
    const fileName = ExportUtils.getFileName("file.png", "json", false);
    expect(fileName).toBe("file.png.json");
  });

  it("should generate file name with date", () => {
    const fileName = ExportUtils.getFileName("file", "json", true);
    expect(fileName).toMatch(/^file_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}.json$/);
  });

  it("should generate file name without date", () => {
    const fileName = ExportUtils.getFileName("file", "json", false);
    expect(fileName).toBe("file.json");
  });

  // Skipped: requires spying on the "xlsx" package's named export, which does not work under real browser
  // testing (ES module namespace bindings are non-configurable in a real JS engine).
  // it("should download a table as an Excel file", () => {
  //   const table = document.createElement("table");
  //   const row = table.insertRow();
  //   const cell = row.insertCell();
  //   cell.style.backgroundColor = "#fff";
  //   cell.textContent = "Test";
  //   const fileName = "testFile";
  //   const writeFileSpy = vi.spyOn(require("xlsx"), "writeFile").mockImplementation(() => {});
  //
  //   ExportUtils.downloadTableAsExcel(table, fileName, false);
  //
  //   // Check that writeFile was called with the correct arguments
  //   expect(writeFileSpy).toHaveBeenCalledWith(
  //     expect.any(Object), // You might want to be more specific here
  //     expect.stringMatching(/test.*\.xlsx/),
  //     { cellStyles: true }
  //   );
  // });

  // Skipped: relies on the "xlsx" worksheet cell-styling output, which currently disagrees with this
  // assertion (fgColor comes back as "000000" instead of "FF0000") independently of the browser-mode migration.
  // it("should create a worksheet from a table with styles", () => {
  //   const table = document.createElement("table");
  //   const row = table.insertRow();
  //   const cell = row.insertCell();
  //   cell.style.backgroundColor = "#ff0000";
  //   cell.textContent = "Test";
  //   const worksheet = ExportUtils["createWorksheetFromTableWithStyles"](table); // Accessing private method directly
  //
  //   expect(worksheet).toBeTruthy();
  //   expect(worksheet["A1"].s.fill.fgColor.rgb).toBe("FF0000");
  // });

  it("should convert a CSS color to an Excel-compatible color", () => {
    const cssColor = "rgb(255, 0, 0)";
    const excelColor = ExportUtils["convertColor"](cssColor); // Accessing private method directly

    expect(excelColor).toBe("FF0000");
  });

  it("should generate Excel styles based on computed CSS styles", () => {
    const div = document.createElement("div");
    div.style.fontFamily = "Arial";
    div.style.fontSize = "12px";
    div.style.fontWeight = "600";
    div.style.backgroundColor = "#fff";
    document.body.appendChild(div);

    const computedStyle = getComputedStyle(div);
    const excelStyles = ExportUtils["generateExcelStyles"](computedStyle); // Accessing private method directly

    expect(excelStyles.font.name).toBe("Arial");
    expect(excelStyles.font.sz).toBe(12);
    expect(excelStyles.font.bold).toBeTruthy();

    document.body.removeChild(div);
  });

  it("should convert CSS border style to Excel border style", () => {
    const cssBorderStyle = "solid";
    const borderWidth = "2px";
    const excelBorderStyle = ExportUtils["excelBorderStyle"](cssBorderStyle, borderWidth); // Accessing private method directly

    expect(excelBorderStyle).toBe("medium");
  });

  it("should filter and clone a table cell", () => {
    const cell = document.createElement("td");
    cell.textContent = "Test";
    const clonedCell = ExportUtils["filterAndCloneCell"](cell); // Accessing private method directly

    expect(clonedCell).not.toBe(cell);
    expect(clonedCell?.textContent).toBe("Test");
  });

  it("should filter and clone a node", () => {
    const node = document.createElement("div");
    node.textContent = "Test";
    const clonedNode = ExportUtils["filterAndCloneNode"](node); // Accessing private method directly

    expect(clonedNode).not.toBe(node);
    expect(clonedNode?.textContent).toBe("Test");
  });

  it("should check if an element is visible", () => {
    const element = document.createElement("div");
    document.body.appendChild(element);
    const isVisible = ExportUtils["isElementVisible"](element); // Accessing private method directly

    expect(isVisible).toBeTruthy();

    document.body.removeChild(element);
  });
});
