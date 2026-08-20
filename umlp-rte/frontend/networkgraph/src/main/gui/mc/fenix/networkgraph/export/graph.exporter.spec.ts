import { downloadChartAsPNG } from "./graph.exporter";
import { vi, type MockInstance } from 'vitest';
import { ExportUtils } from "../shared/utils/export.utils";
import Sigma from "sigma";
import Graph from "graphology";

describe("downloadChartAsPNG", () => {
  let graph: Graph;
  let sigmaInstance: Sigma;
  let container: HTMLElement;

  let getDimensionsSpy: MockInstance;
  let getSettingsSpy: MockInstance;
  let downloadCanvasAsPNGSpy: MockInstance;

  beforeEach(() => {
    Object.defineProperty(window, "devicePixelRatio", { value: 1, configurable: true });

    container = document.createElement("div");
    container.style.width = "800px";
    container.style.height = "600px";
    document.body.appendChild(container);

    // Manually update container dimensions for the test environment
    Object.defineProperty(container, "clientWidth", { value: 800 });
    Object.defineProperty(container, "clientHeight", { value: 600 });

    // Create a sample Graph
    graph = new Graph();

    // Sample graph data to import
    const graphData = {
      nodes: [
        { key: "n1", attributes: { x: 0, y: 0, label: "Node 1", color: "#f00" } },
        { key: "n2", attributes: { x: 3, y: 1, label: "Node 2", color: "#0f0" } },
        { key: "n3", attributes: { x: 1, y: 3, label: "Node 3", color: "#00f" } },
        { key: "n4", attributes: { x: 2, y: 3, label: "Node 4", color: "#ff0" } },
      ],
      edges: [
        { source: "n1", target: "n2", attributes: {} },
        { source: "n2", target: "n3", attributes: {} },
        { source: "n3", target: "n1", attributes: {} },
        { source: "n3", target: "n4", attributes: {} },
      ],
    };

    // Import the graph data
    graph.import(graphData);

    // Create a real Sigma instance with basic configurations and graph data
    sigmaInstance = new Sigma(graph, container, {
      allowInvalidContainer: true,
    });

    // Spy on Sigma methods
    getDimensionsSpy = vi.spyOn(sigmaInstance, "getDimensions");
    getSettingsSpy = vi.spyOn(sigmaInstance, "getSettings");
    downloadCanvasAsPNGSpy = vi.spyOn(ExportUtils, "downloadCanvasAsPNG");
  });

  it("should correctly call dependencies", () => {
    const fileName = "test.png";
    const createElementSpy = vi.spyOn(document, "createElement");
    const appendSpy = vi.spyOn(document.body, "append");

    downloadChartAsPNG(sigmaInstance, fileName);

    // Test if Sigma methods are called
    expect(sigmaInstance.getDimensions).toHaveBeenCalled();
    expect(sigmaInstance.getSettings).toHaveBeenCalled();

    // Test if DOM manipulation methods are called
    expect(createElementSpy).toHaveBeenCalledWith("DIV");
    expect(appendSpy).toHaveBeenCalled();

    // Test if ExportUtils.downloadObjectAsPng is called
    expect(downloadCanvasAsPNGSpy).toHaveBeenCalled();
  });
});
