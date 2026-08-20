import { TestBed } from "@angular/core/testing";
import { GraphLayoutService } from "./graph-layout.service";
import { SigmaGraphService } from "../data/sigma-graph.service";
import { COMPLEX_LAYOUT_MAPPING, INITIAL_LAYOUT_MAPPING } from "../consts/graph.const";
import { ComplexGraphLayouts, InitialGraphLayouts } from "../types/graph-layout.types";
import { Sigma } from "sigma";
import Graph from "graphology";
import { GraphSettingsService } from "./graph-settings.service";

describe("GraphLayoutService", () => {
  let service: GraphLayoutService<any, any, any>;
  let sigmaRenderer: Sigma;
  let renderer: SigmaGraphService<any, any, any>;

  function createSigmaInstance(): void {
    const container: HTMLDivElement = document.createElement("div");
    container.style.width = "800px";
    container.style.height = "600px";
    document.body.appendChild(container);
    // Manually update container dimensions for the test environment
    Object.defineProperty(container, "clientWidth", { value: 800 });
    Object.defineProperty(container, "clientHeight", { value: 600 });

    // Create a sample Graph
    const graph = new Graph();

    // Sample graph data to import
    const graphData = {
      nodes: [
        { key: "node1", attributes: { x: 0, y: 0, label: "Node 1", color: "#f00" } },
        { key: "node2", attributes: { x: 3, y: 1, label: "Node 2", color: "#0f0" } },
        { key: "node3", attributes: { x: 1, y: 3, label: "Node 3", color: "#00f" } },
        { key: "node4", attributes: { x: 2, y: 3, label: "Node 4", color: "#ff0" } },
      ],
      edges: [
        { source: "node1", target: "node2", attributes: {} },
        { source: "node2", target: "node3", attributes: {} },
        { source: "node3", target: "node1", attributes: {} },
        { source: "node3", target: "node4", attributes: {} },
      ],
    };

    // Import the graph data
    graph.import(graphData);

    // Create a real Sigma instance with basic configurations and graph data
    sigmaRenderer = new Sigma(graph, container, { allowInvalidContainer: true });
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [GraphSettingsService, GraphLayoutService, SigmaGraphService],
    });

    service = TestBed.inject(GraphLayoutService);
    renderer = TestBed.inject(SigmaGraphService);
    createSigmaInstance();
    vi.spyOn(renderer, "renderer", "get").mockReturnValue(sigmaRenderer);
  });

  afterEach(() => {
    // Do not kill sigmaRenderer here: TestBed's automatic teardown calls SigmaGraphService.ngOnDestroy(),
    // which kills the renderer returned by the mocked "renderer" getter (i.e. this same sigmaRenderer).
    // Killing it twice throws, since the WebGL context is already torn down after the first kill().
    service.stopLayouting();
    sigmaRenderer.getContainer().remove();
    vi.restoreAllMocks();
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  describe("applyInitialLayout", () => {
    it("should apply initial layout if provided", () => {
      const initialLayoutSpy = vi.spyOn(INITIAL_LAYOUT_MAPPING, InitialGraphLayouts.RANDOM);
      service.applyInitialLayout({ mode: InitialGraphLayouts.RANDOM });
      expect(initialLayoutSpy).toHaveBeenCalledWith(renderer.graph, {});
    });

    it("should not apply initial layout if not provided", () => {
      const initialLayoutSpy = vi.spyOn(INITIAL_LAYOUT_MAPPING, InitialGraphLayouts.RANDOM);
      service.applyInitialLayout(null);
      expect(initialLayoutSpy).not.toHaveBeenCalled();
    });
  });

  describe("startLayouting", () => {
    it("should start layouting process with provided configuration", () => {
      const complexLayoutSpy = vi.spyOn(COMPLEX_LAYOUT_MAPPING, ComplexGraphLayouts.FORCEATLAS2);
      service.startLayouting({
        mode: ComplexGraphLayouts.FORCEATLAS2,
        parameters: { settings: { linLogMode: true } },
        maxIterations: 100,
      });
      expect(complexLayoutSpy).toHaveBeenCalledWith(renderer.graph, { settings: { linLogMode: true } });
    });

    it("should stop layouting process if no configuration provided", () => {
      const stopLayoutingSpy = vi.spyOn(service, "stopLayouting");
      service.startLayouting(null);
      expect(stopLayoutingSpy).toHaveBeenCalled();
    });
  });

  describe("continueLayout", () => {
    it("should continue layouting with last configuration", () => {
      const startLayoutingSpy = vi.spyOn(service, "startLayouting");
      service["lastComplexLayoutConfig"] = {
        mode: ComplexGraphLayouts.FORCEATLAS2,
        parameters: {},
        maxIterations: 100,
      };
      service.continueLayout();
      expect(startLayoutingSpy).toHaveBeenCalledWith({
        mode: ComplexGraphLayouts.FORCEATLAS2,
        parameters: {},
        maxIterations: 100,
      });
    });

    it("should stop layouting if no last configuration", () => {
      const stopLayoutingSpy = vi.spyOn(service, "stopLayouting");
      service["lastComplexLayoutConfig"] = null;
      service.continueLayout();
      expect(stopLayoutingSpy).toHaveBeenCalled();
    });
  });

  describe("stopLayouting", () => {
    it("should stop the layouting process", () => {
      vi.spyOn(window, "cancelAnimationFrame").mockImplementation(() => {});
      service["frameID"] = 1;
      service.stopLayouting();
      expect(window.cancelAnimationFrame).toHaveBeenCalledWith(1);
    });
  });

  describe("reset", () => {
    it("should reset the service to its initial state", () => {
      const stopLayoutingSpy = vi.spyOn(service, "stopLayouting");
      service.reset();
      expect(stopLayoutingSpy).toHaveBeenCalled();
      expect(service["elapsedLayoutTime"]).toEqual(0);
    });
  });

  describe("getIterationCount", () => {
    it("should return the current iteration count", () => {
      service["_iterationCount"].next(5);
      expect(service.getIterationCount()).toEqual(5);
    });
  });

  describe("isRunning", () => {
    it("should return true if the layouting process is running", () => {
      service["isLayoutStopped"] = false;
      expect(service.isRunning).toEqual(true);
    });

    it("should return false if the layouting process is not running", () => {
      service["isLayoutStopped"] = true;
      expect(service.isRunning).toEqual(false);
    });
  });
});
