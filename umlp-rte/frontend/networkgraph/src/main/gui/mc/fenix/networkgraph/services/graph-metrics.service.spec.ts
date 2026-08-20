import { TestBed } from "@angular/core/testing";
import { SigmaGraphService } from "../data/sigma-graph.service";
import { Sigma } from "sigma";
import Graph from "graphology";
import { GraphSettingsService } from "./graph-settings.service";
import { GraphMetricsService } from "./graph-metrics.service";
import { GraphDataService } from "../data/graph-data.service";
import { GraphLayoutService } from "./graph-layout.service";
import { SerializedEdge, SerializedNode } from "graphology-types";

describe("GraphMetricsService", () => {
  let service: GraphMetricsService<any, any, any>;
  let sigmaRenderer: Sigma;
  let renderer: SigmaGraphService<any, any, any>;
  let dataManager: GraphDataService<any, any, any>;

  const edges: SerializedEdge[] = [
    { source: "node1", target: "node2", attributes: {} },
    { source: "node2", target: "node3", attributes: {} },
    { source: "node3", target: "node1", attributes: {} },
    { source: "node3", target: "node4", attributes: {} },
  ];
  const nodes: SerializedNode[] = [
    { key: "node1", attributes: { x: 0, y: 0, label: "Node 1", color: "#f00", group: "group1" } },
    { key: "node2", attributes: { x: 3, y: 1, label: "Node 2", color: "#0f0", group: "group1" } },
    { key: "node3", attributes: { x: 1, y: 3, label: "Node 3", color: "#00f", group: "group1" } },
    { key: "node4", attributes: { x: 2, y: 3, label: "Node 4", color: "#ff0", group: "group2" } },
  ];

  function createSigmaInstance(): void {
    const container: HTMLDivElement = document.createElement("div");
    container.style.width = "800px";
    container.style.height = "600px";
    document.body.appendChild(container);
    // Manually update container dimensions for the test environment
    Object.defineProperty(container, "clientWidth", { value: 800 });
    Object.defineProperty(container, "clientHeight", { value: 600 });

    // Create a sample Graph
    const graph = new Graph({ type: "directed" });

    // Sample graph data to import
    const graphData = {
      nodes: nodes,
      edges: edges,
    };

    // Import the graph data
    graph.import(graphData);

    // Create a real Sigma instance with basic configurations and graph data
    sigmaRenderer = new Sigma(graph, container, { allowInvalidContainer: true });
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [GraphSettingsService, GraphMetricsService, SigmaGraphService, GraphDataService, GraphLayoutService],
    });

    service = TestBed.inject(GraphMetricsService);
    renderer = TestBed.inject(SigmaGraphService);
    dataManager = TestBed.inject(GraphDataService);
    dataManager.processedEdges = edges;
    dataManager.processedNodes = nodes;
    createSigmaInstance();
    vi.spyOn(renderer, "renderer", "get").mockReturnValue(sigmaRenderer);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  describe("Individual Metric Methods", () => {
    it("should get node count", () => {
      const result = service["getNodesCount"]();
      expect(result).toBe(4);
    });

    it("should get edge count", () => {
      const result = service["getEdgesCount"]();
      expect(result).toBe(4);
    });

    it("should get density", () => {
      const result = service["getDensity"]();
      expect(result).toBeCloseTo(0.33);
    });

    it("should get modularity", () => {
      const result = service["getModularity"]();
      expect(result).toBe(0);
    });

    it("should get stress", () => {
      const result = service["getStress"]();
      expect(result).toBeCloseTo(13.35);
    });
  });

  describe("getMetricInformation Method", () => {
    it("should get all metric information by default", () => {
      const result = service.getMetricInformation();
      expect(result).toHaveProperty("Nodes Count", 4);
      expect(result).toHaveProperty("Edges Count", 4);
    });

    it("should get specified metric information", () => {
      const result = service.getMetricInformation(["nodeCount", "density"]);
      expect(result).toEqual({
        "Nodes Count": 4,
        Density: expect.any(Number), // The exact value may vary based on sample graph data
      });
    });
  });
});
