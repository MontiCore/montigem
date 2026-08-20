import { TestBed } from "@angular/core/testing";
import { SigmaGraphService } from "./sigma-graph.service";
import { GraphSettingsService } from "../services/graph-settings.service";
import Graph from "graphology";
import { Sigma } from "sigma";
import { EdgeAttributes, NodeAttributes } from "../types/graph.types";
import { Attributes, SerializedGraph } from "graphology-types";

describe("SigmaGraphService", <N extends NodeAttributes = NodeAttributes, E extends
  EdgeAttributes = EdgeAttributes, G extends Attributes = Attributes>() => {
  let service: SigmaGraphService<N, E, G>;

  const container: HTMLDivElement = document.createElement("div");
  container.style.width = "800px";
  container.style.height = "600px";

  function createSigmaInstance(): void {
    document.body.appendChild(container);
    // Manually update container dimensions for the test environment
    Object.defineProperty(container, "clientWidth", { value: 800 });
    Object.defineProperty(container, "clientHeight", { value: 600 });

    // Create a sample Graph
    const graph: Graph<N, E, G> = new Graph();

    // Sample graph data to import
    const graphData: Partial<SerializedGraph<N, E, G>> = {
      nodes: [
        { key: "node1", attributes: { x: 0, y: 0, label: "Node 1", color: "#f00" } as N },
        { key: "node2", attributes: { x: 3, y: 1, label: "Node 2", color: "#0f0" } as N },
        { key: "node3", attributes: { x: 1, y: 3, label: "Node 3", color: "#00f" } as N },
        { key: "node4", attributes: { x: 2, y: 3, label: "Node 4", color: "#ff0" } as N },
      ],
      edges: [
        { source: "node1", target: "node2", attributes: { type: "line" } as E },
        { source: "node2", target: "node3", attributes: { type: "line" } as E },
        { source: "node3", target: "node1", attributes: { type: "line" } as E },
        { source: "node3", target: "node4", attributes: { type: "line" } as E },
      ],
    };

    // Import the graph data
    graph.import(graphData);

    // Create a real Sigma instance with basic configurations and graph data
    service.createRenderer(graph, container, { allowInvalidContainer: true });
  }

  afterEach(() => {
    vi.useRealTimers();
  });

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [SigmaGraphService, GraphSettingsService],
    });

    service = TestBed.inject(SigmaGraphService);
    createSigmaInstance();

    const graphSettings = TestBed.inject(GraphSettingsService);
    graphSettings.updateSetting("allowInvalidContainer", true);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  describe("createRenderer", () => {
    it("should create a Sigma renderer", () => {
      expect(service.renderer).toBeInstanceOf(Sigma);
      expect(service.renderer).toBe(service.renderer);
    });
  });

  describe("setGraph", () => {
    it("should set the graph for the renderer", () => {
      const newGraph: Graph<N, E, G> = new Graph();
      service.setGraph(newGraph);
      expect(service.graph).toBe(newGraph);
    });
  });

  describe("resetZoom", () => {
    it("should reset the zoom level of the camera", () => {
      const camera = service.camera;
      expect(camera).toBeDefined();
      const animatedZoomSpy = vi.spyOn(camera!, "animatedZoom");
      service.animatedZoom({ factor: 2, duration: 1 });
      vi.runAllTimers();

      // Ensure that our zoom ratio has changed
      expect(animatedZoomSpy).toHaveBeenCalled();
      expect(camera!.ratio).not.toEqual(1);

      // Reset zoom and ensure that the ratio is "1" again
      service.resetZoom({ duration: 1 });
      vi.runAllTimers();

      expect(camera!.ratio).toEqual(1);
    });
  });

  describe("resize", () => {
    it("should resize the graph rendering area", () => {
      expect(service.renderer).toBeDefined();
      const resizeSpy = vi.spyOn(service.renderer!, "resize");
      service.resize();
      expect(resizeSpy).toHaveBeenCalled();
    });
  });

  describe("refresh", () => {
    it("should redraw the graph", () => {
      expect(service.renderer).toBeDefined();
      const refreshSpy = vi.spyOn(service.renderer!, "refresh");
      service.refresh();
      expect(refreshSpy).toHaveBeenCalled();
    });
  });

  describe("animateCamera", () => {
    it("should animate the camera to a new state", () => {
      const camera = service.camera;
      expect(camera).toBeDefined();
      vi.spyOn(camera!, "animate");
      service.animateCamera({ x: 10, y: 10 }, { duration: 200 });
      expect(camera!.animate).toHaveBeenCalledWith({ ratio: 0.1, x: 10, y: 10 }, { duration: 200 }, undefined);
    });
  });
});
