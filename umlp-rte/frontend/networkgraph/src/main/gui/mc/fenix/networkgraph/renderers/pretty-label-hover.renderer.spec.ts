import { getDefaultGraphSettings } from "../consts/graph-settings.const";
import { prettyLabelHoverRenderer } from "./pretty-label-hover.renderer";
import { NodeAttributes } from "../types/graph.types";
import { GraphSettings } from "../types/graph-settings.types";
import { vi, MockInstance } from 'vitest';

// Note: spying on named exports of "./render.utils" (e.g. `vi.spyOn(RenderUtils, "drawShadowAndRect")`) does not work
// under real browser testing: ES module namespace bindings are non-configurable in a real JS engine, so
// Object.defineProperty on them throws "Cannot redefine property". Assertions below observe the canvas context
// directly instead, since CanvasRenderingContext2D methods are regular, spy-able object properties.
describe("prettyLabelHoverRenderer", () => {
  let context: CanvasRenderingContext2D;
  let data: NodeAttributes;
  let settings: GraphSettings;
  let textSpy: MockInstance;
  let fillSpy: MockInstance;
  let measureTextSpy: MockInstance;

  beforeEach(() => {
    const canvas: HTMLCanvasElement = document.createElement("canvas");
    context = canvas.getContext("2d") as CanvasRenderingContext2D;
    data = {
      color: "#ff0000",
      group: "Unknown",
      type: "circle",
      label: "Node 1",
      tooltip: "Tooltip",
      x: 50,
      y: 50,
      size: 10,
    };
    settings = getDefaultGraphSettings();

    textSpy = vi.spyOn(context, "fillText");
    fillSpy = vi.spyOn(context, "fill");
    measureTextSpy = vi.spyOn(context, "measureText");
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("should render hover label correctly", () => {
    prettyLabelHoverRenderer(context, data, settings);

    // The shadow/background rectangle is drawn once.
    expect(fillSpy).toHaveBeenCalled();

    // Label, tooltip, and group are each measured and drawn.
    expect(measureTextSpy).toHaveBeenCalledTimes(3);
    expect(textSpy).toHaveBeenCalledTimes(3);
    expect(textSpy.mock.calls.map((call) => call[0])).toEqual([data.label, data.tooltip, data.group]);
  });

  it("should not render anything if label, tooltip, and group are missing", () => {
    data.label = "";
    data.tooltip = "";
    data.group = "";

    prettyLabelHoverRenderer(context, data, settings);

    // No shadow/background rectangle without a label, and no text to render.
    expect(fillSpy).not.toHaveBeenCalled();
    expect(textSpy).not.toHaveBeenCalled();
  });
});
