import { getDefaultGraphSettings } from "../consts/graph-settings.const";
import { prettyLabelRenderer } from "./pretty-label.renderer";
import { NodeAttributes } from "../types/graph.types";
import { GraphSettings } from "../types/graph-settings.types";
import { ColorUtil } from "../shared/utils/color.util";
import * as RenderUtils from "./render.utils";
import { vi, MockInstance } from 'vitest';

describe("prettyLabelRenderer", () => {
  let context: CanvasRenderingContext2D;
  let data: NodeAttributes;
  let settings: GraphSettings;

  beforeEach(() => {
    vi.resetAllMocks();

    const canvas: HTMLCanvasElement = document.createElement("canvas");
    context = canvas.getContext("2d") as CanvasRenderingContext2D;
    data = {
      color: "#ff0000",
      group: "Unknown",
      type: "circle",
      label: "Node 1",
      x: 50,
      y: 50,
      size: 10,
    };
    settings = getDefaultGraphSettings();
  });

  it("should render label correctly", () => {
    // Fill text gets called
    const textSpy = vi.spyOn(context, "fillText");
    // Fill (for the rounded rect around the label) gets called
    const rectSpy = vi.spyOn(context, "fill");
    prettyLabelRenderer(context, data, settings);

    expect(textSpy).toHaveBeenCalledWith(data.label, expect.any(Number), expect.any(Number));
    expect(rectSpy).toHaveBeenCalled();
  });

  it("should not render anything if label is missing", () => {
    const textSpy = vi.spyOn(context, "fillText");
    const rectSpy = vi.spyOn(context, "fill");
    data.label = "";
    prettyLabelRenderer(context, data, settings);

    expect(textSpy).not.toHaveBeenCalled();
    expect(rectSpy).not.toHaveBeenCalled();
  });

  it("should apply correct styles", () => {
    const contextState: Array<any> = [];
    // Correct the type signature for fillOriginal
    const fillOriginal: {
      (fillRule?: CanvasFillRule | undefined): void;
      (path: Path2D, fillRule?: CanvasFillRule | undefined): void;
    } = context.fill;
    const fillTextOriginal = context.fillText; // Bind the original method to context

    // Mock fill and fillText to capture the context state
    context.fill = function (...args: any[]) {
      contextState.push({
        globalAlpha: this.globalAlpha,
        fillStyle: this.fillStyle,
        method: "fill",
      });
      fillOriginal.apply(this, args);
    };

    context.fillText = function (...args) {
      contextState.push({
        globalAlpha: this.globalAlpha,
        fillStyle: this.fillStyle,
        font: this.font,
        method: "fillText",
      });
      fillTextOriginal.apply(this, args);
    };

    prettyLabelRenderer(context, data, settings);

    // Restore original methods
    context.fill = fillOriginal;
    context.fillText = fillTextOriginal;

    // Check the context state at each rendering step
    expect(contextState).toEqual([
      {
        globalAlpha: settings.prettyLabelsConfiguration.labelBackgroundOpacity,
        fillStyle: ColorUtil.parseIntoHEX(settings.prettyLabelsConfiguration.labelBackgroundColor),
        method: "fill",
      },
      {
        globalAlpha: 1, // Assuming originalAlpha was 1
        fillStyle: RenderUtils.resolveLabelColor(data, settings),
        font: `${settings.labelSize}px ${settings.labelFont}`, // Assuming a default font family of sans-serif
        method: "fillText",
      },
    ]);
  });
});
