import { ComponentFixture, TestBed } from "@angular/core/testing";
import { GraphManagerService } from "../data/graph-manager.service";
import { FilterGroupComponent } from "./filter-group.component";
import { GraphMultiSelectChangeEvent } from "../shared/ui/graph-multi-select/graph-multi-select.types";
import { SortedFilterGroupInformation } from "../types/filter-group.type";
import { CommonModule } from "@angular/common";
import { vi, Mocked, Mock } from 'vitest';

describe("FilterGroupComponent", () => {
  let component: FilterGroupComponent;
  let fixture: ComponentFixture<FilterGroupComponent>;
  let mockGraphManagerService: Mocked<GraphManagerService>;

  beforeEach(async () => {
    mockGraphManagerService = {
      graphData: {
        getNodeFilterGroups: vi.fn().mockReturnValue(
          new Map([
            ["group1", { count: 3, groupColor: "red" }],
            ["group2", { count: 5, groupColor: "green" }],
            ["group3", { count: 2, groupColor: "blue" }],
          ])
        ),
      },
      showGroupKeys: vi.fn(),
      onSigmaInit$: { subscribe: vi.fn() },
    } as unknown as Mocked<GraphManagerService>;

    await TestBed.configureTestingModule({
      imports: [FilterGroupComponent, CommonModule],
      providers: [{ provide: GraphManagerService, useValue: mockGraphManagerService }],
    }).compileComponents();

    fixture = TestBed.createComponent(FilterGroupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should sort and prepare groups correctly", () => {
    const expected: SortedFilterGroupInformation[] = [
      { groupKey: "group2", width: 100, count: 5, groupColor: "green" },
      { groupKey: "group1", width: 60, count: 3, groupColor: "red" },
      { groupKey: "group3", width: 40, count: 2, groupColor: "blue" },
    ];

    expect((component as any).processedGroups).toEqual(expected);
  });

  it("should emit group selection change on multi-select", () => {
    const mockEvent: GraphMultiSelectChangeEvent = { value: ["group1", "group3"], originalEvent: {} as Event };

    (component as any).selectGroup(mockEvent);

    expect(mockGraphManagerService.showGroupKeys).toHaveBeenCalledWith(new Set(["group1", "group3"]));
  });

  it("should update groups upon initialization", () => {
    expect(mockGraphManagerService.graphData.getNodeFilterGroups).toHaveBeenCalled();
    expect((component as any).processedGroups).toBeDefined();
    expect((component as any).selectedGroups).toEqual(["group1", "group2", "group3"]);
  });

  it("should subscribe to onSigmaInit$ and update groups upon Sigma initialization", () => {
    const sigmaInitSubscriber = (mockGraphManagerService.onSigmaInit$.subscribe as Mock).mock.calls[0][0];
    sigmaInitSubscriber();
    expect(mockGraphManagerService.graphData.getNodeFilterGroups).toHaveBeenCalledTimes(2); // Once on init, once on Sigma init
    expect((component as any).processedGroups).toBeDefined();
    expect((component as any).selectedGroups).toEqual(["group1", "group2", "group3"]);
  });
});
