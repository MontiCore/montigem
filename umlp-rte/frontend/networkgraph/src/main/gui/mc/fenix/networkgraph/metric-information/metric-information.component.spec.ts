import { ComponentFixture, TestBed } from "@angular/core/testing";
import { By } from "@angular/platform-browser";
import { DebugElement } from "@angular/core";
import { MetricInformationComponent } from "./metric-information.component";
import { GraphManagerService } from "../data/graph-manager.service";
import { SigmaGraphService } from "../data/sigma-graph.service";
import { GraphMetricsService } from "../services/graph-metrics.service";
import { GraphSettingsService } from "../services/graph-settings.service";
import { GraphDataService } from "../data/graph-data.service";
import { GraphProcessorService } from "../graph-processing/graph-processor.service";
import { EdgeProcessorService } from "../graph-processing/edge-processor.service";
import { GroupMappingService } from "../graph-processing/group-mapping.service";
import { NodeProcessorService } from "../graph-processing/node-processor.service";
import { GraphLayoutService } from "../services/graph-layout.service";
import { vi, MockInstance } from 'vitest';

describe("MetricInformationComponent", () => {
  let component: MetricInformationComponent;
  let fixture: ComponentFixture<MetricInformationComponent>;
  let debugElement: DebugElement;
  let graphManager: GraphManagerService;
  let getAllInformationSpy: MockInstance;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MetricInformationComponent],
      providers: [
        GraphManagerService,
        SigmaGraphService,
        GraphSettingsService,
        GraphDataService,
        GraphProcessorService,
        EdgeProcessorService,
        GroupMappingService,
        NodeProcessorService,
        GraphMetricsService,
        GraphLayoutService,
      ],
    }).compileComponents();

    graphManager = TestBed.inject(GraphManagerService);
    getAllInformationSpy = vi.spyOn(graphManager.graphMetrics, "getAllInformationMetricKeys");

    fixture = TestBed.createComponent(MetricInformationComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;
    fixture.detectChanges();
  });

  it("should create the component", () => {
    expect(component).toBeTruthy();
  });

  it("should toggle overlay panel when button is clicked", () => {
    const overlayPanel = component["overlayPanel"];
    expect(overlayPanel).toBeDefined();
    const spy = vi.spyOn(overlayPanel!, "toggle");
    const button = debugElement.query(By.css(".graph-icon-button"));

    button.triggerEventHandler("click", null);
    fixture.detectChanges();

    expect(spy).toHaveBeenCalled();
  });

  it("should hide overlay panel when escape key is pressed", () => {
    const overlayPanel = component["overlayPanel"];
    expect(overlayPanel).toBeDefined();
    const spy = vi.spyOn(overlayPanel!, "hide");

    fixture.whenStable().then(() => {
      const event = new KeyboardEvent("keydown", {
        key: "Escape",
      });
      window.dispatchEvent(event);

      expect(spy).toHaveBeenCalled();
    });
  });

  it("should update metric information when overlay panel is shown", () => {
    const spy = vi.spyOn(component as any, "updateMetricInformation");
    // Create a mouse click event
    const mouseEvent = new MouseEvent("click", {
      bubbles: true,
      cancelable: true,
      view: window,
      button: 0,
    });

    // Find the button element and dispatch the mouse event
    const buttonElement = debugElement.query(By.css(".graph-icon-button")).nativeElement;
    buttonElement.dispatchEvent(mouseEvent);

    fixture.detectChanges();

    fixture.whenStable().then(() => {
      // Check if the updateMetricInformation method was called
      expect(spy).toHaveBeenCalled();
    });
  });

  it("should initialize metric keys from GraphManagerService", () => {
    fixture.detectChanges();
    expect(getAllInformationSpy).toHaveBeenCalled();
  });
});
