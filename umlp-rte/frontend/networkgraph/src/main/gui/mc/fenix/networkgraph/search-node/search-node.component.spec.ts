import { ComponentFixture, TestBed } from "@angular/core/testing";
import { SearchNodeComponent } from "./search-node.component";
import { GraphManagerService } from "../data/graph-manager.service";
import {vi, Mocked} from "vitest"

describe("SearchNodeComponent", () => {
  let component: SearchNodeComponent;
  let fixture: ComponentFixture<SearchNodeComponent>;
  let mockGraphManagerService: Mocked<GraphManagerService>;
  const nodes = [
    { searchLabel: "Node1", nodeKey: "1", group: "Group1", groupColor: "#ff0000" },
    { searchLabel: "Node2", nodeKey: "2", group: "Group2", groupColor: "#00ff00" },
    { searchLabel: "Node3", nodeKey: "3", group: "Group3", groupColor: "#0000ff" },
  ];

  beforeEach(async () => {
    mockGraphManagerService = {
      graphData: {
        getNodeSearchInformation: vi.fn().mockReturnValue(nodes),
        selectedNode: "1",
      },
      zoomToNode: vi.fn(),
      setSelectedNode: vi.fn(),
      onSigmaInit$: { subscribe: vi.fn() },
    } as unknown as Mocked<GraphManagerService>;

    await TestBed.configureTestingModule({
      imports: [SearchNodeComponent],
      providers: [{ provide: GraphManagerService, useValue: mockGraphManagerService }],
    }).compileComponents();

    fixture = TestBed.createComponent(SearchNodeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should emit the selected node key when nodeSelected is called", () => {
    const emitSpy = vi.spyOn(mockGraphManagerService, "setSelectedNode");
    const zoomSpy = vi.spyOn(mockGraphManagerService, "zoomToNode");

    component.nodeSelected({
      value: "mockNode",
      originalEvent: {} as Event,
    });

    expect(zoomSpy).toHaveBeenCalledWith("mockNode");
    expect(emitSpy).toHaveBeenCalledWith("mockNode");
  });

  it("should call updateSearchNodes on init", () => {
    expect((component as any).searchNodes).toEqual(nodes);
  });

  it("should update the searchNodes correctly", () => {
    (component as any).updateSearchNodes();
    expect((component as any).searchNodes.length).toBe(3); // Assuming the mock data has 3 nodes
    expect((component as any).visibleSearchNodes.length).toBe(3); // Adjust according to your actual logic
  });
});
