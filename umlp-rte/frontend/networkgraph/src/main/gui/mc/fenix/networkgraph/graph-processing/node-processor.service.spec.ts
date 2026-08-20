import { TestBed } from "@angular/core/testing";
import { NodeProcessorService } from "./node-processor.service";
import { GroupMappingService } from "./group-mapping.service";
import { GraphDataService } from "../data/graph-data.service";
import { GraphSettingsService } from "../services/graph-settings.service";
import { Internal_GraphNode } from "../types/graph.types";
import { SigmaGraphService } from "../data/sigma-graph.service";

describe("NodeProcessorService", () => {
  let service: NodeProcessorService<any, any, any>;
  let dataStorage: GraphDataService<any, any, any>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SigmaGraphService, NodeProcessorService, GraphDataService, GraphSettingsService, GroupMappingService],
    });
    service = TestBed.inject(NodeProcessorService);
    dataStorage = TestBed.inject(GraphDataService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  describe("processNodeData", () => {
    it("should process node data correctly", () => {
      service.rawNodeData = [
        {
          name: "testName",
        },
        {
          name: "duplicated",
        },
        {
          name: "duplicated",
        },
        {
          key: "testKey",
        },
        {
          id: 0,
        },
        {
          id: "1",
          attributes: {
            label: "HR",
            size: 20,
            tooltip: "Human Resources",
            type: "Management",
          },
        },
        {
          id: "2",
          label: "HR",
          size: 20,
          tooltip: "Human Resources",
          type: "Management",
        },
        {
          attributes: {
            id: "3",
            label: "HR",
            size: 20,
            tooltip: "Human Resources",
            type: "Management",
          },
        },
      ];

      service.processNodeData();

      expect(dataStorage.processedNodes).toEqual([
        {
          attributes: {
            color: "#999999",
            group: "unknown",
            hidden: false,
            label: "testName",
            size: 10,
            type: "circle",
            x: 0,
            y: 0,
          },
          key: "node_auto_id_0",
        },
        {
          attributes: {
            color: "#999999",
            group: "unknown",
            hidden: false,
            label: "duplicated",
            size: 10,
            type: "circle",
            x: 0,
            y: 0,
          },
          key: "node_auto_id_1",
        },
        {
          attributes: {
            color: "#999999",
            group: "unknown",
            hidden: false,
            label: "duplicated",
            size: 10,
            type: "circle",
            x: 0,
            y: 0,
          },
          key: "node_auto_id_2",
        },
        {
          attributes: {
            color: "#999999",
            group: "unknown",
            hidden: false,
            label: "testKey",
            size: 10,
            type: "circle",
            x: 0,
            y: 0,
          },
          key: "testKey",
        },
        {
          attributes: {
            color: "#999999",
            group: "unknown",
            hidden: false,
            label: "0",
            size: 10,
            type: "circle",
            x: 0,
            y: 0,
          },
          key: "0",
        },
        {
          attributes: {
            color: "#999999",
            group: "Management",
            hidden: false,
            label: "HR",
            size: 20,
            tooltip: "Human Resources",
            type: "circle",
            x: 0,
            y: 0,
          },
          key: "1",
        },
        {
          attributes: {
            color: "#999999",
            group: "Management",
            hidden: false,
            label: "HR",
            size: 20,
            tooltip: "Human Resources",
            type: "circle",
            x: 0,
            y: 0,
          },
          key: "2",
        },
        {
          attributes: {
            color: "#999999",
            group: "Management",
            hidden: false,
            label: "HR",
            size: 20,
            tooltip: "Human Resources",
            type: "circle",
            x: 0,
            y: 0,
          },
          key: "3",
        },
      ]);
    });
  });

  describe("reset", () => {
    it("should reset node data", () => {
      service.rawNodeData = [{ id: "1" }];
      (service as any).nodeIdCounter = 1;

      service.reset();

      expect(dataStorage.uniqueNodeKeySet).toEqual(new Set());
      expect(dataStorage.processedNodes).toBeNull();
      expect(dataStorage.groupMemberCountMap).toEqual(new Map());
      expect(dataStorage.nodeSearchInformation).toEqual([]);
      expect((service as any).nodeIdCounter).toEqual(0);
    });
  });

  describe("createNode", () => {
    it("should create a node", () => {
      const key = "key1";
      const node: Internal_GraphNode = {
        id: "1",
        label: "Node 1",
        size: 20,
        type: "Management",
      };
      const createdNode = (service as any).createNode(key, node);

      expect(createdNode).toEqual({
        key: "key1",
        attributes: {
          color: "#999999",
          group: "Management",
          hidden: false,
          label: "Node 1",
          size: 20,
          type: "circle",
          x: 0,
          y: 0,
        },
      });
    });
  });

  describe("extractUIInformation", () => {
    it("should extract UI information", () => {
      const node = {
        key: "key1",
        attributes: {
          color: "#999999",
          group: "Management",
          label: "Node 1",
        },
      };
      (service as any).extractUIInformation(node);

      const groupInfo = dataStorage.groupMemberCountMap.get("Management");
      const nodeSearchInfo = dataStorage.nodeSearchInformation.find((info) => info.nodeKey === "key1");

      expect(groupInfo).toEqual({ count: 1, groupColor: "#999999" });
      expect(nodeSearchInfo).toEqual({
        searchLabel: "Node 1",
        group: "Management",
        groupColor: "#999999",
        nodeKey: "key1",
      });
    });
  });

  describe("logNodeWarnings", () => {
    it("should log warnings for duplicated node keys", () => {
      const consoleWarnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
      (service as any).logNodeWarnings(["key1", "key2"]);
      expect(consoleWarnSpy).toHaveBeenCalled();
      consoleWarnSpy.mockRestore();
    });
  });
});
