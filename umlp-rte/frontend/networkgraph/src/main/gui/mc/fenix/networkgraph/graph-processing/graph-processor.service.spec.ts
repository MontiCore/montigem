import { TestBed } from "@angular/core/testing";
import { GraphProcessorService } from "./graph-processor.service";
import { EdgeProcessorService } from "./edge-processor.service";
import { NodeProcessorService } from "./node-processor.service";
import { GroupMappingService } from "./group-mapping.service";
import { GraphDataService } from "../data/graph-data.service";
import { GraphSettingsService } from "../services/graph-settings.service";
import { SigmaGraphService } from "../data/sigma-graph.service";

describe("GraphProcessorService", () => {
  let service: GraphProcessorService<any, any, any>;
  let edgeProcessor: EdgeProcessorService<any, any, any>;
  let nodeProcessor: NodeProcessorService<any, any, any>;
  let groupMapping: GroupMappingService<any, any, any>;
  let dataStorage: GraphDataService<any, any, any>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SigmaGraphService,
        GraphProcessorService,
        EdgeProcessorService,
        NodeProcessorService,
        GroupMappingService,
        GraphDataService,
        GraphSettingsService,
      ],
    });

    service = TestBed.inject(GraphProcessorService);
    edgeProcessor = TestBed.inject(EdgeProcessorService);
    nodeProcessor = TestBed.inject(NodeProcessorService);
    groupMapping = TestBed.inject(GroupMappingService);
    dataStorage = TestBed.inject(GraphDataService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  describe("setNodeData", () => {
    it("should set raw node data", () => {
      const nodes = [{ id: "1" }, { id: "2" }];
      service.setNodeData(nodes);
      expect(nodeProcessor.rawNodeData).toEqual(nodes);
    });
  });

  describe("setEdgeData", () => {
    it("should set raw edge data", () => {
      const edges = [
        { source: "1", target: "2" },
        { source: "2", target: "3" },
      ];
      service.setEdgeData(edges);
      expect(edgeProcessor.rawEdgeData).toEqual(edges);
    });
  });

  describe("processGraphData", () => {
    it("should process graph data", () => {
      const processNodeDataSpy = vi.spyOn(nodeProcessor, "processNodeData");
      const processEdgeDataSpy = vi.spyOn(edgeProcessor, "processEdgeData");
      const assignGroupColorsSpy = vi.spyOn(groupMapping, "assignGroupColors");

      const nodes = [
        { id: "1" },
        { key: "2", color: "red", group: "testGroup" },
        { name: 3 },
        { group: "another group" },
      ];
      service.setNodeData(nodes);

      const edges = [
        { source: "1", target: "2", attributes: { name: "edgeWithKey" } },
        { source: "2", target: "3" },
        { source: "1", target: "3", directed: false, attributes: { color: "green" } },
        { from: "node_auto_id_0", to: "node_auto_id_1" },
      ];
      service.setEdgeData(edges);

      service.processGraphData();

      expect(assignGroupColorsSpy).toHaveBeenCalled();
      expect(processNodeDataSpy).toHaveBeenCalled();
      expect(processEdgeDataSpy).toHaveBeenCalled();
      expect(dataStorage.processedNodes).toEqual([
        {
          attributes: {
            color: "#999999",
            group: "unknown",
            hidden: false,
            label: "1",
            size: 10,
            type: "circle",
            x: 0,
            y: 0,
          },
          key: "1",
        },
        {
          attributes: {
            color: "red",
            group: "testGroup",
            hidden: false,
            label: "2",
            size: 10,
            type: "circle",
            x: 0,
            y: 0,
          },
          key: "2",
        },
        {
          attributes: {
            color: "#999999",
            group: "unknown",
            hidden: false,
            label: "3",
            size: 10,
            type: "circle",
            x: 0,
            y: 0,
          },
          key: "node_auto_id_0",
        },
        {
          attributes: {
            color: "#88DBE4",
            group: "another group",
            hidden: false,
            label: "node_auto_id_1",
            size: 10,
            type: "circle",
            x: 0,
            y: 0,
          },
          key: "node_auto_id_1",
        },
      ]);
      expect(dataStorage.processedEdges).toEqual([
        {
          attributes: {
            color: "rgba(158, 158, 158, 1)",
            group: "unknown",
            hidden: false,
            label: "edgeWithKey",
            size: 1,
            type: "line",
          },
          key: "edge_auto_id_0",
          source: "1",
          target: "2",
          undirected: true,
        },
        {
          attributes: {
            color: "rgba(158, 158, 158, 1)",
            group: "unknown",
            hidden: false,
            label: "edge_auto_id_3",
            size: 1,
            type: "line",
          },
          key: "edge_auto_id_3",
          source: "node_auto_id_0",
          target: "node_auto_id_1",
          undirected: true,
        },
      ]);
    });
  });

  describe("reset", () => {
    it("should reset all related services", () => {
      const resetEdgeSpy = vi.spyOn(edgeProcessor, "reset");
      const resetNodeSpy = vi.spyOn(nodeProcessor, "reset");
      const resetGroupMappingSpy = vi.spyOn(groupMapping, "reset");

      service.reset();

      expect(resetEdgeSpy).toHaveBeenCalled();
      expect(resetNodeSpy).toHaveBeenCalled();
      expect(resetGroupMappingSpy).toHaveBeenCalled();
    });
  });
});
