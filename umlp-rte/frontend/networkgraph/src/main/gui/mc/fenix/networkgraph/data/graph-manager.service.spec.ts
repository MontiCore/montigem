import { TestBed } from "@angular/core/testing";
import { GraphManagerService } from "./graph-manager.service";
import { SigmaGraphService } from "./sigma-graph.service";
import { GraphDataService } from "./graph-data.service";
import { GraphProcessorService } from "../graph-processing/graph-processor.service";
import { EdgeProcessorService } from "../graph-processing/edge-processor.service";
import { NodeProcessorService } from "../graph-processing/node-processor.service";
import { GroupMappingService } from "../graph-processing/group-mapping.service";
import { GraphLayoutService } from "../services/graph-layout.service";
import { GraphMetricsService } from "../services/graph-metrics.service";
import { GraphSettingsService } from "../services/graph-settings.service";
import { ElementRef } from "@angular/core";
import Graph from "graphology";
import { Sigma } from "sigma";
import { MouseCoords } from "sigma/types";

describe("GraphManagerService", () => {
  let service: GraphManagerService;
  let sigmaRenderer: Sigma;

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
        { source: "node1", target: "node2", attributes: { type: "line" } },
        { source: "node2", target: "node3", attributes: { type: "line" } },
        { source: "node3", target: "node1", attributes: { type: "line" } },
        { source: "node3", target: "node4", attributes: { type: "line" } },
      ],
    };

    // Import the graph data
    graph.import(graphData);

    // Create a real Sigma instance with basic configurations and graph data
    sigmaRenderer = new Sigma(graph, container, { allowInvalidContainer: true });
    service.setContainer(container);
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        GraphManagerService,
        GraphLayoutService,
        GraphDataService,
        GraphProcessorService,
        EdgeProcessorService,
        NodeProcessorService,
        GroupMappingService,
        GraphMetricsService,
        GraphSettingsService,
        SigmaGraphService,
        { provide: ElementRef, useValue: { nativeElement: document.createElement("div") } },
      ],
    });
    service = TestBed.inject(GraphManagerService);
    const graphSettings = TestBed.inject(GraphSettingsService);
    graphSettings.updateSetting("allowInvalidContainer", true);

    createSigmaInstance();
    // Mock Sigma Graph Renderer
    vi.spyOn(service.sigmaGraph, "renderer", "get").mockReturnValue(sigmaRenderer as any);
  });

  afterEach(() => {
    // Do not kill sigmaRenderer here: TestBed's automatic teardown calls SigmaGraphService.ngOnDestroy(),
    // which kills the renderer returned by the mocked "renderer" getter (i.e. this same sigmaRenderer).
    // Killing it twice throws, since the WebGL context is already torn down after the first kill().
    service.layoutService.stopLayouting();
    sigmaRenderer.getContainer().remove();
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  describe("continueLayouting", () => {
    it("should call layoutService.continueLayout when graph is not null", () => {
      const layoutSpy = vi.spyOn(service.layoutService, "continueLayout");
      service.applyGraphLayout();
      service.continueLayouting();
      expect(layoutSpy).toHaveBeenCalled();
    });
  });

  describe("stopLayouting", () => {
    it("should call layoutService.stopLayouting", () => {
      const layoutSpy = vi.spyOn(service.layoutService, "stopLayouting");
      service.stopLayouting();
      expect(layoutSpy).toHaveBeenCalled();
    });
  });

  describe("resetSelection", () => {
    it("should reset all selections", () => {
      const setSelectedNodeSpy = vi.spyOn(service, "setSelectedNode");
      const setSelectedEdgeSpy = vi.spyOn(service, "setSelectedEdge");
      const setHoveredEdgeSpy = vi.spyOn(service, "setHoveredEdge");
      const setHoveredNodeSpy = vi.spyOn(service, "setHoveredNode");

      service.resetSelection();

      expect(setSelectedNodeSpy).toHaveBeenCalledWith(null);
      expect(setSelectedEdgeSpy).toHaveBeenCalledWith(null);
      expect(setHoveredEdgeSpy).toHaveBeenCalledWith(null);
      expect(setHoveredNodeSpy).toHaveBeenCalledWith(null);
    });
  });

  describe("applyGraphLayout", () => {
    it("should call layoutService methods when graph is not null", () => {
      const applyInitialLayoutSpy = vi.spyOn(service.layoutService, "applyInitialLayout");
      const startLayoutingSpy = vi.spyOn(service.layoutService, "startLayouting");

      service.applyGraphLayout();

      expect(applyInitialLayoutSpy).toHaveBeenCalled();
      expect(startLayoutingSpy).toHaveBeenCalled();
    });
  });

  describe("zoomIn", () => {
    it("should call sigmaGraph.animatedZoom", () => {
      const zoomSpy = vi.spyOn(service.sigmaGraph, "animatedZoom");
      service.zoomIn();
      expect(zoomSpy).toHaveBeenCalled();
    });
  });

  describe("zoomOut", () => {
    it("should call sigmaGraph.animatedUnzoom", () => {
      const zoomSpy = vi.spyOn(service.sigmaGraph, "animatedUnzoom");
      service.zoomOut();
      expect(zoomSpy).toHaveBeenCalled();
    });
  });

  describe("resetZoom", () => {
    it("should call sigmaGraph.resetZoom and highlightNode", () => {
      const resetZoomSpy = vi.spyOn(service.sigmaGraph, "resetZoom");
      const highlightNodeSpy = vi.spyOn(service, "highlightNode");
      service.resetZoom();
      expect(resetZoomSpy).toHaveBeenCalled();
      expect(highlightNodeSpy).toHaveBeenCalledWith(null);
    });
  });

  describe("zoomToNode", () => {
    it("should call highlightNode and sigmaGraph.animateCamera when nodeKey is not null", () => {
      const highlightNodeSpy = vi.spyOn(service, "highlightNode");
      const animateCameraSpy = vi.spyOn(service.sigmaGraph, "animateCamera");
      const nodeKey = "node1";
      service.zoomToNode(nodeKey);
      expect(highlightNodeSpy).toHaveBeenCalledWith(nodeKey);
      expect(animateCameraSpy).toHaveBeenCalled(); // Additional checks can be made on the arguments
    });

    it("should only call highlightNode when nodeKey is null", () => {
      const highlightNodeSpy = vi.spyOn(service, "highlightNode");
      const animateCameraSpy = vi.spyOn(service.sigmaGraph, "animateCamera");
      service.zoomToNode(null);
      expect(highlightNodeSpy).toHaveBeenCalledWith(null);
      expect(animateCameraSpy).not.toHaveBeenCalled();
    });
  });

  describe("setContainer", () => {
    it("should set container and update containerReadySignal", () => {
      const container = document.createElement("div");
      const containerReadySignalSpy = vi.spyOn((service as any).containerReadySignal, "set");
      service.setContainer(container);
      expect((service as any).container).toBe(container);
      expect(containerReadySignalSpy).toHaveBeenCalled();
    });
  });

  describe("needsInitialization", () => {
    it("should update needsInitializationSignal", () => {
      service.needsInitialization(true);
      // assuming you have a way to check needsInitializationSignal value or it's publicly accessible
      expect(service.needsInitializationSignalTracked).toBeTruthy();
    });
  });

  describe("reload", () => {
    it("should call initializeChartIfNeeded if signal is true", () => {
      const spy = vi.spyOn(service as any, "initializeChartIfNeeded").mockImplementation(() => {});
      service.needsInitialization(true); // set the signal to true
      service.reload();
      expect(spy).toHaveBeenCalled();
    });
  });

  describe("handleNodeDragging", () => {
    it("should update node position if conditions are met", () => {
      // This is a simplified test and may not cover all cases
      service.graphSettings.getSetting = vi.fn().mockReturnValue(true);
      service.graphData.isDragging = true;
      service.graphData.draggedNode = "node1";

      const mockEvent: MouseCoords = {
        x: 0,
        y: 0,
        sigmaDefaultPrevented: false,
        preventSigmaDefault: vi.fn(),
        original: new MouseEvent("mousemove"),
      };

      if (service.graph) {
        const nodeAttributesSpy = vi.spyOn(service.graph, "setNodeAttribute");
        (service as any).handleNodeDragging(mockEvent);
        expect(nodeAttributesSpy).toHaveBeenCalledWith("node1", "y", expect.any(Number));
        expect(nodeAttributesSpy).toHaveBeenCalledWith("node1", "x", expect.any(Number));
      }
    });
  });

  describe("setSelectedNode", () => {
    it("should set selected node", () => {
      const nodeKey = "node1";
      service.setSelectedNode(nodeKey);
      expect(service.graphData.selectedNode).toBe(nodeKey);
    });
  });

  describe("resize", () => {
    it("should call sigmaGraph.resize", () => {
      const resizeSpy = vi.spyOn(service.sigmaGraph, "resize");
      service.resize();
      expect(resizeSpy).toHaveBeenCalled();
    });
  });

  describe("redraw", () => {
    it("should call sigmaGraph.redraw", () => {
      const redrawSpy = vi.spyOn(service.sigmaGraph, "refresh");
      service.redraw();
      expect(redrawSpy).toHaveBeenCalled();
    });
  });

  describe("reload", () => {
    it("should call initializeChartIfNeeded if signal is true", () => {
      const spy = vi.spyOn(service as any, "initializeChartIfNeeded").mockImplementation(() => {});
      service.needsInitialization(true); // set the signal to true
      service.reload();
      expect(spy).toHaveBeenCalled();
    });
  });
});
