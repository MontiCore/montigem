import { GraphSettingsService } from "./graph-settings.service";
import { getDefaultGraphSettings } from "../consts/graph-settings.const";
import { GraphSettings } from "../types/graph-settings.types";

describe("GraphSettingsService", () => {
  let service: GraphSettingsService;

  beforeEach(() => {
    service = new GraphSettingsService();
  });

  it("should be created with default settings", () => {
    expect(service).toBeTruthy();
    expect(JSON.stringify(service.getSettings())).toEqual(JSON.stringify(getDefaultGraphSettings()));
  });

  describe("updateSettings", () => {
    it("should update multiple settings and return true if changed", () => {
      const newSettings: Partial<GraphSettings> = {
        defaultNodeSize: 15,
        edgeLabelSize: 10,
      };
      const result = service.updateSettings(newSettings);
      expect(result).toBe(true);
      expect(service.getSetting("defaultNodeSize")).toBe(15);
      expect(service.getSetting("edgeLabelSize")).toBe(10);
    });

    it("should return false if settings are not changed", () => {
      const result = service.updateSettings({});
      expect(result).toBe(false);
    });
  });

  describe("updateSetting", () => {
    it("should update a single setting and return true if changed", () => {
      const result = service.updateSetting("defaultNodeSize", 20);
      expect(result).toBe(true);
      expect(service.getSetting("defaultNodeSize")).toBe(20);
    });

    it("should return false if setting is not changed", () => {
      const result = service.updateSetting("defaultNodeSize", service.getSetting("defaultNodeSize"));
      expect(result).toBe(false);
    });
  });

  describe("getSettings", () => {
    it("should return current graph settings", () => {
      const settings = service.getSettings();
      expect(JSON.stringify(settings)).toStrictEqual(JSON.stringify(getDefaultGraphSettings()));
    });
  });

  describe("getSetting", () => {
    it("should return the setting associated with the given key", () => {
      const setting = service.getSetting("defaultNodeSize");
      expect(setting).toBe(getDefaultGraphSettings().defaultNodeSize);
    });
  });

  describe("computeAreaDependentScaling", () => {
    it("should update settings based on container area", () => {
      const container = document.createElement("div");
      container.style.width = "200px";
      container.style.height = "200px";
      document.body.appendChild(container);

      // Mocking getBoundingClientRect in Jest
      container.getBoundingClientRect = vi.fn(() => {
        return {
          x: 0,
          y: 0,
          width: 200,
          height: 200,
          top: 0,
          left: 0,
          bottom: 0,
          right: 0,
          toJSON: () => {},
        };
      });

      service.updateSettings({ areaDependentScaling: true });
      service.computeAreaDependentScaling(container);

      // Assuming expected scaleFactor and zoomToSizeRatioFunction results
      // Update with actual expected values as needed
      const expectedScaleFactor = 92.68 * Math.exp(-0.59 * Math.log10(200 * 200 + 1));
      const expectedZoomToSizeRatioFunctionResult = Math.sqrt(1) * expectedScaleFactor;

      expect(service.getSetting("zoomToSizeRatioFunction")(1)).toBeCloseTo(expectedZoomToSizeRatioFunctionResult);

      document.body.removeChild(container);
    });
  });

  describe("computeStagePadding", () => {
    it("should update stage padding", () => {
      service.setLargestNodeSize(20);
      service.updateSettings({
        zoomToSizeRatioFunction: (ratio: number) => {
          return Math.sqrt(ratio) * 10; // Assume a simple function for illustration
        },
      });
      service.computeStagePadding();
      expect(service.getSetting("stagePadding")).toBe(2); // Expected 20 / 10
    });
  });

  describe("setLargestNodeSize", () => {
    it("should set the size of the largest node", () => {
      service.setLargestNodeSize(25);
      expect(service["largestNodeSize"]).toBe(25);
    });
  });

  describe("computeScaleFactor", () => {
    it("should compute scaling factor based on logarithmic area", () => {
      const logArea = Math.log10(200 * 200 + 1);
      const result = service["computeScaleFactor"](logArea);
      const expected = 92.68 * Math.exp(-0.59 * logArea);
      expect(result).toBeCloseTo(expected);
    });
  });
});
