import { TestBed } from "@angular/core/testing";
import { EdgeProcessorService } from "./edge-processor.service";
import { GroupMappingService } from "./group-mapping.service";
import { GraphDataService } from "../data/graph-data.service";
import { GraphSettingsService } from "../services/graph-settings.service";
import { Attributes } from "graphology-types";
import { EdgeAttributes, Internal_GraphEdge, NodeAttributes } from "../types/graph.types";
import { SigmaGraphService } from "../data/sigma-graph.service";

function orient(source: string, target: string, reverse: boolean): { source: string; target: string } {
  return reverse ? { source: target, target: source } : { source, target };
}

describe("EdgeProcessorService", () => {
  let service: EdgeProcessorService<NodeAttributes, EdgeAttributes, Attributes>;
  let dataStorage: GraphDataService<NodeAttributes, EdgeAttributes, Attributes>;
  let settings: GraphSettingsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SigmaGraphService, EdgeProcessorService, GroupMappingService, GraphDataService, GraphSettingsService],
    });
    service = TestBed.inject(EdgeProcessorService);
    dataStorage = TestBed.inject(GraphDataService);
    settings = TestBed.inject(GraphSettingsService);
    vi.spyOn(dataStorage.uniqueNodeKeySet, "has").mockReturnValue(true); // Assuming all nodes are valid
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  describe("reset", () => {
    it("should reset edge data", () => {
      service.rawEdgeData = [{ source: "1", target: "2" }];
      (service as any).edgeIdCounter = 1;

      service.reset();

      expect(dataStorage.uniqueEdgeKeySet).toEqual(new Set());
      expect(dataStorage.processedEdges).toBeNull();
      expect((service as any).edgeIdCounter).toEqual(0);
    });
  });

  describe.each([false, true])("processEdgeData (reverseEdgeDirection: %s)", reverse => {
    beforeEach(() => {
      settings.updateSetting("reverseEdgeDirection", reverse);
    });

    it("should process edge data correctly", () => {
      service.rawEdgeData = [
        {
          source: "1",
          target: "2",
          weight: 3,
          edgeProgram: "arrow",
        },
        {
          from: "2",
          to: "3",
          size: 2,
        },
        {
          attributes: {
            source: "1",
            target: "3",
            size: 2,
            directed: true,
          },
        },
        {
          key: "testEdge",
          source: "1",
          target: "9",
        },
        {
          name: "namedEdge",
          from: "0",
          to: "3",
        },
      ];

      service.processEdgeData();

      expect(dataStorage.processedEdges).toEqual([
        {
          attributes: {
            color: "rgba(207, 207, 207, 1)",
            group: "unknown",
            hidden: false,
            label: "edge_auto_id_0",
            size: 3,
            type: "arrow",
          },
          key: "edge_auto_id_0",
          ...orient("1", "2", reverse),
          undirected: true,
        },
        {
          attributes: {
            color: "rgba(207, 207, 207, 1)",
            group: "unknown",
            hidden: false,
            label: "edge_auto_id_1",
            size: 2,
            type: "line",
          },
          key: "edge_auto_id_1",
          ...orient("2", "3", reverse),
          undirected: true,
        },
        {
          attributes: {
            color: "rgba(207, 207, 207, 1)",
            group: "unknown",
            hidden: false,
            label: "edge_auto_id_2",
            size: 2,
            type: "arrow",
          },
          key: "edge_auto_id_2",
          ...orient("1", "3", reverse),
          undirected: false,
        },
        {
          attributes: {
            color: "rgba(207, 207, 207, 1)",
            group: "unknown",
            hidden: false,
            label: "testEdge",
            size: 1,
            type: "line",
          },
          key: "testEdge",
          ...orient("1", "9", reverse),
          undirected: true,
        },
        {
          attributes: {
            color: "rgba(207, 207, 207, 1)",
            group: "unknown",
            hidden: false,
            label: "namedEdge",
            size: 1,
            type: "line",
          },
          key: "edge_auto_id_3",
          ...orient("0", "3", reverse),
          undirected: true,
        },
      ]);
    });
  });

  describe.each([false, true])("createEdge (reverseEdgeDirection: %s)", reverse => {
    beforeEach(() => {
      settings.updateSetting("reverseEdgeDirection", reverse);
    });

    it("should create an edge", () => {
      const key = "key1";
      const edge: Internal_GraphEdge = {
        source: "1",
        target: "2",
        weight: 3,
      };
      const createdEdge = (service as any).createEdge(key, edge);

      expect(createdEdge).toEqual({
        attributes: {
          color: "rgba(207, 207, 207, 1)",
          group: "unknown",
          hidden: false,
          label: "key1",
          size: 3,
          type: "line",
        },
        key: "key1",
        ...orient("1", "2", reverse),
        undirected: true,
      });
    });
  });

  describe("logEdgeWarnings", () => {
    it("should log warnings for duplicated edge keys, duplicated edges, and invalid pairs", () => {
      const consoleWarnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
      (service as any).logEdgeWarnings(["(1, 2)"], ["key1"], ["(3, 4)"], ["(3, 3)"]);
      expect(consoleWarnSpy).toHaveBeenCalled();
      consoleWarnSpy.mockRestore();
    });
  });
});
