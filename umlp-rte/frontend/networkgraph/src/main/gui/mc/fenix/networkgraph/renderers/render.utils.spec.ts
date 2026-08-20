import { createTextObject,drawShadowAndRect,measureTextDimensions,drawRoundedRect, resolveLabelColor } from "./render.utils";
import { NodeAttributes } from "../types/graph.types";
import { GraphSettings } from "../types/graph-settings.types";
import { TextObject } from "../types/custom-renderer.types";
import { ColorUtil } from "../shared/utils/color.util";
import { getDefaultGraphSettings } from "../consts/graph-settings.const";
import { Settings } from "sigma/settings";
import {vi} from "vitest"

describe("Rendering Utilities", () => {
  let context: CanvasRenderingContext2D;
  let data: NodeAttributes;
  let settings: GraphSettings;
  let textObject: TextObject;

  beforeEach(() => {
    vi.resetAllMocks();

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
    textObject = createTextObject(settings as Settings, {
      content: "Test",
      fontSize: 14,
      color: "#000000",
    });
  });

  it("should draw a rounded rectangle correctly", () => {
    const rectSpy = vi.spyOn(context, "fill");
    drawRoundedRect(context, 10, 10, 100, 50, 10);
    expect(rectSpy).toHaveBeenCalled();
  });

  it("should measure text dimensions correctly", () => {
    const spy = vi.spyOn(context, "measureText");
    measureTextDimensions(context, textObject);
    expect(spy).toHaveBeenCalled();
  });

  it("should create a TextObject correctly", () => {
    expect(textObject).toMatchObject({
      baseLine: "bottom",
      width: 0,
      height: 0,
      font: settings.labelFont,
      weight: settings.labelWeight,
      content: "Test",
      fontSize: 14,
      color: "#000000",
    });
  });

  it("should resolve label color correctly", () => {
    const resolvedColor = resolveLabelColor(data, settings);
    expect(resolvedColor).toBe(ColorUtil.parseIntoHEX(settings.labelColor.color));
  });

  it("should draw shadow and rectangle correctly", () => {
    const rectSpy = vi.spyOn(context, "fill");
    drawShadowAndRect(context, data, settings, 100, 50);
    expect(rectSpy).toHaveBeenCalled();
  });
});
