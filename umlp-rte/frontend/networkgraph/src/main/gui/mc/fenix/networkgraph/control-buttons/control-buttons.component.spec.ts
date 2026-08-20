import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ControlButtonsComponent } from "./control-buttons.component";
import { GraphManagerService } from "../data/graph-manager.service";
import { RegularControlButton } from "../types/control-buttons.types";
import { GraphTooltipOptions } from "../shared/types/tooltip.types";

// Mock implementation of GraphManagerService
const mockGraphManagerService = {
  stopLayouting: vi.fn(),
  continueLayouting: vi.fn(),
  reload: vi.fn(),
  zoomIn: vi.fn(),
  zoomOut: vi.fn(),
  resetZoom: vi.fn(),
  requestFullscreen: vi.fn(),
  exitFullscreen: vi.fn(),
};

describe("ControlButtonsComponent", () => {
  let component: ControlButtonsComponent;
  let fixture: ComponentFixture<ControlButtonsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ControlButtonsComponent],
      providers: [{ provide: GraphManagerService, useValue: mockGraphManagerService }],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ControlButtonsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it('should call stopLayouting when "stopLayout" button is clicked', () => {
    const button = (component as any).controlButtons.find(
      (button: RegularControlButton) => button.key === "stopLayout"
    );
    (component as any).performAction(button as any);
    expect(mockGraphManagerService.stopLayouting).toHaveBeenCalled();
  });

  it("should toggle state when toggleable button is clicked", () => {
    const button = {
      key: "toggleFullscreen",
      isToggleable: true,
      toggled: false,
      onTooltip: "Escape Fullscreen",
      offTooltip: "View Fullscreen",
      onIcon: "pi pi-window-minimize",
      offIcon: "pi pi-window-maximize",
    };
    (component as any).toggleAction(button as any);
    expect(button.toggled).toBe(true);
  });

  it('should calculate radius correctly when speedDialConfig.radius is "auto"', () => {
    component.speedDialConfig = { radius: "auto" };
    (component as any).speedDialItems = new Array(10);
    const radius = component.getRadius();
    expect(radius).toBeGreaterThanOrEqual(40);
  });

  // Initialization and Default State Test
  it("should initialize with correct default state", () => {
    expect((component as any).controlButtons.length).toBeGreaterThan(0);
    expect(component.layout).toBeDefined();
  });

  // Dynamic Button Addition and Removal
  it("should add and remove buttons dynamically", () => {
    component.extendControlButtons = [{ key: "newButton", tooltip: "New Button", icon: "pi pi-plus" }];
    component.removeControlButtons = ["stopLayout"];
    (component as any).updateControlButtons();
    expect((component as any).controlButtons.some((button: RegularControlButton) => button.key === "newButton")).toBe(
      true
    );
    expect((component as any).controlButtons.some((button: RegularControlButton) => button.key === "stopLayout")).toBe(
      false
    );
  });

  // Tooltip Options Test
  it("should set tooltip options correctly", () => {
    component.tooltipOptions = { tooltipPosition: "top" };
    const button = (component as any).controlButtons[0];
    const result: GraphTooltipOptions = (component as any).getTooltipOptions(button);
    expect(result.tooltipPosition).toBe("top");
  });

  // Layout Change Tests
  it("should update layout correctly", () => {
    component.layout = "horizontal";
    expect(component.isVerticalOrHorizontalLayout).toBe(true);
  });

  // SpeedDial Configuration Test
  it("should update speedDialConfig correctly", () => {
    const newConfig = { visible: true };
    component.speedDialConfig = newConfig;
    expect(component.speedDialConfig?.visible).toBe(true);
  });

  it("should handle regular button click", () => {
    const button = (component as any).controlButtons.find((button: RegularControlButton) => button.key === "zoomIn");
    (component as any).handleButtonClick(button as any);
    expect(mockGraphManagerService.zoomIn).toHaveBeenCalled();
  });

  it("should handle toggleable button click", () => {
    const button = {
      key: "toggleFullscreen",
      isToggleable: true,
      toggled: false,
      onTooltip: "Escape Fullscreen",
      offTooltip: "View Fullscreen",
      onIcon: "pi pi-window-minimize",
      offIcon: "pi pi-window-maximize",
    };
    (component as any).handleButtonClick(button as any);
    expect(button.toggled).toBe(true);
  });

  it("should transform control buttons to speed dial items correctly", () => {
    (component as any).updateSpeedDialItems();
    expect((component as any).speedDialItems.length).toBe((component as any).controlButtons.length);
  });

  it("should calculate radius correctly when speedDialConfig.radius is a specific value", () => {
    component.speedDialConfig = { radius: 100 };
    const radius = component.getRadius();
    expect(radius).toBe(100);
  });
});
