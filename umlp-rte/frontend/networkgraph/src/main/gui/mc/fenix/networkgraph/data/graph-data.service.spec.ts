import { TestBed } from "@angular/core/testing";
import { GraphDataService } from "./graph-data.service";
import { GraphSettingsService } from "../services/graph-settings.service";
import { SigmaGraphService } from "./sigma-graph.service";
import Graph from "graphology";

const mockGraphSettingsService = {
  getSetting: vi.fn().mockReturnValue("Unknown Group"),
};

const mockGraph = new Graph();
const mockSigmaGraphService = {
  graph: mockGraph,
};

describe("GraphDataService", () => {
  let service: GraphDataService<any, any, any>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        GraphDataService,
        { provide: GraphSettingsService, useValue: mockGraphSettingsService },
        { provide: SigmaGraphService, useValue: mockSigmaGraphService },
      ],
    });
    service = TestBed.inject(GraphDataService);
  });

  beforeEach(() => {
    mockGraph.addNode("node1", { group: "group1" });
    mockGraph.addNode("node2", { group: "group2" });
    mockGraph.addEdgeWithKey("edge1", "node1", "node2");
  });

  afterEach(() => {
    mockGraph.clear();
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  // Serialized Graph Handling
  describe("SerializedGraph Handling", () => {
    it("should get and set serializedGraph correctly", () => {
      const mockSerializedGraph = { nodes: [], edges: [] };
      service.serializedGraph = mockSerializedGraph;
      expect(service.serializedGraph).toEqual(mockSerializedGraph);
    });
  });

  // Camera State Handling
  describe("Camera State Handling", () => {
    it("should get and set cameraState correctly", () => {
      const mockCameraState = { x: 0, y: 0, ratio: 1 };
      service.cameraState = mockCameraState;
      expect(service.cameraState).toEqual(mockCameraState);
    });
  });

  // Node and Edge Highlight, Select, Drag, and Hover Handling
  describe("Node and Edge Interaction Handling", () => {
    it("should handle node highlight correctly", () => {
      service.highlightNode = "node1";
      expect(mockGraph.getNodeAttribute("node1", "highlighted")).toBeTruthy();
      expect(service.highlightNode).toEqual("node1");
    });

    it("should handle node selection correctly", () => {
      service.selectedNode = "node1";
      expect(service.selectedNode).toEqual("node1");
    });

    it("should handle edge selection correctly", () => {
      service.selectedEdge = "edge1";
      expect(service.selectedEdge).toEqual("edge1");
    });

    it("should handle node drag correctly", () => {
      service.draggedNode = "node1";
      expect(service.draggedNode).toEqual("node1");
      expect(service.isDragging).toBeTruthy();
    });

    it("should handle node hover correctly", () => {
      service.hoveredNode = "node1";
      expect(service.hoveredNode).toEqual("node1");
    });

    it("should handle edge hover correctly", () => {
      service.hoveredEdge = "edge1";
      expect(service.hoveredEdge).toEqual("edge1");
    });
  });

  // Data Reset Handling
  describe("Data Reset Handling", () => {
    it("should reset node data correctly", () => {
      service.resetNodeData();
      expect(service.uniqueNodeKeySet.size).toBe(0);
      expect(service.processedNodes).toBeNull();
    });

    it("should reset edge data correctly", () => {
      service.resetEdgeData();
      expect(service.isMultiGraph).toBe(false);
      expect(service.processedEdges).toBeNull();
    });
  });

  // Group Visibility Handling
  describe("Group Visibility Handling", () => {
    it("should show group keys correctly", () => {
      const groupKeys = new Set(["group1"]);
      service.showGroupKeys(groupKeys);
      expect(mockGraph.getNodeAttribute("node1", "hidden")).toBeFalsy();
    });
  });

  // Miscellaneous Methods
  describe("Miscellaneous Methods", () => {
    it("should get unknown group name correctly", () => {
      expect(service.getUnknownGroupName()).toEqual("Unknown Group");
    });

    it("should notify data change correctly", () => {
      const spy = vi.spyOn(service["dataChangeSubject"], "next");
      service.notifyDataChange();
      expect(spy).toHaveBeenCalled();
    });

    it("should check node visibility correctly", () => {
      expect(service["nodeIsVisible"]("node1")).toBeTruthy();
    });

    it("should check edge visibility correctly", () => {
      expect(service["edgeIsVisible"]("edge1")).toBeTruthy();
    });
  });

  // Reset Data Tests
  describe("Reset Data", () => {
    it("should reset node data", () => {
      service.resetNodeData();
      expect(service.processedNodes).toBeNull();
      expect(service.uniqueNodeKeySet.size).toBe(0);
      expect(service.groupMemberCountMap.size).toBe(0);
      expect(service.nodeSearchInformation.length).toBe(0);
    });

    it("should reset edge data", () => {
      service.resetEdgeData();
      expect(service.processedEdges).toBeNull();
      expect(service.uniqueEdgeKeySet.size).toBe(0);
      expect(service.isMultiGraph).toBeFalsy();
    });
  });

  // Show Group Keys Tests
  describe("Show Group Keys", () => {
    it("should show group keys correctly", () => {
      const groupKeys = new Set(["group1"]);
      service.showGroupKeys(groupKeys);
      // Assuming mockGraph has a method to get node attributes
      const nodeAttributes = mockGraph.getNodeAttributes("node1"); // Assume 'node1' is a node key
      expect(nodeAttributes["hidden"]).toBeFalsy();
    });
  });

  // Get Unknown Group Name Tests
  describe("Get Unknown Group Name", () => {
    it("should get unknown group name correctly", () => {
      // Assuming mockGraphSettingsService has getSetting method returning 'Unknown Group'
      const unknownGroupName = service.getUnknownGroupName();
      expect(unknownGroupName).toBe("Unknown Group");
    });
  });

  // Notify Data Change Tests
  describe("Notify Data Change", () => {
    it("should notify data change correctly", () => {
      const spy = vi.spyOn(service["dataChangeSubject"], "next");
      service.notifyDataChange();
      expect(spy).toHaveBeenCalled();
    });
  });

  // Node and Edge Visibility Tests
  describe("Node and Edge Visibility", () => {
    it("should check node visibility correctly", () => {
      // Assuming nodeIsVisible is a public method or making it public temporarily for testing
      const isVisible = (service as any).nodeIsVisible("node1"); // Assuming 'node1' is a node key
      expect(isVisible).toBeTruthy();
    });

    it("should check edge visibility correctly", () => {
      // Assuming edgeIsVisible is a public method or making it public temporarily for testing
      const isVisible = (service as any).edgeIsVisible("edge1"); // Assuming 'edge1' is an edge key
      expect(isVisible).toBeTruthy();
    });
  });
});
