import { Color, ColorFormat, ColorUtil, HEX, HSL, RGB } from "./color.util";

describe("ColorUtils", () => {
  // Mock getComputedStyle
  beforeAll(() => {
    // Define a custom property "--my-color" with value "rgb(255, 0, 0)"
    Object.defineProperty(window, "getComputedStyle", {
      value: vi.fn(() => ({
        getPropertyValue: (prop: string) => (prop === "--my-color" ? "rgb(255, 0, 0)" : ""),
      })),
    });
  });

  // Test RGBA_PATTERN
  it("should match RGBA color pattern", () => {
    expect(ColorUtil.RGBA_PATTERN.test("rgba(255,255,255,1)")).toBe(true);
    expect(ColorUtil.RGBA_PATTERN.test("rgb(255,255,255)")).toBe(true);
    expect(ColorUtil.RGBA_PATTERN.test("rgba(0,0,0,0.5)")).toBe(true);
    expect(ColorUtil.RGBA_PATTERN.test("rgb(255, 255, 255)")).toBe(true);
    expect(ColorUtil.RGBA_PATTERN.test("rgb(255 255 255)")).toBe(true);
    expect(ColorUtil.RGBA_PATTERN.test("rgb(255, 255, 255 1)")).toBe(true);
    expect(ColorUtil.RGBA_PATTERN.test("rgb(255 255 255 / 80%)")).toBe(true);
    expect(ColorUtil.RGBA_PATTERN.test("rgba(255, 255, 255 1)")).toBe(true);
    expect(ColorUtil.RGBA_PATTERN.test("rgba(255 255 255 / 80%)")).toBe(true);
    expect(ColorUtil.RGBA_PATTERN.test("rgba(255, 255, 255, 27%)")).toBe(true);
  });

  // Test HSLA_PATTERN
  it("should match HSLA color pattern", () => {
    expect(ColorUtil.HSLA_PATTERN.test("hsla(120,100%,50%,1)")).toBe(true);
    expect(ColorUtil.HSLA_PATTERN.test("hsl(120,100%,50%)")).toBe(true);
    expect(ColorUtil.HSLA_PATTERN.test("hsla(0,0%,0%,0.5)")).toBe(true);
    expect(ColorUtil.HSLA_PATTERN.test("hsl(120, 100%, 50%)")).toBe(true);
    expect(ColorUtil.HSLA_PATTERN.test("hsl(120 100% 50%)")).toBe(true);
    expect(ColorUtil.HSLA_PATTERN.test("hsl(120, 100%, 50% 1)")).toBe(true);
    expect(ColorUtil.HSLA_PATTERN.test("hsl(120 100% 50% / 80%)")).toBe(true);
    expect(ColorUtil.HSLA_PATTERN.test("hsla(120, 100%, 50% 1)")).toBe(true);
    expect(ColorUtil.HSLA_PATTERN.test("hsla(120 100% 50% / 80%)")).toBe(true);
    expect(ColorUtil.HSLA_PATTERN.test("hsla(120, 100%, 50%, 27%)")).toBe(true);
  });

  // Test HEX_PATTERN
  it("should match HEX color pattern", () => {
    expect(ColorUtil.HEX_PATTERN.test("#FFFFFF")).toBe(true);
    expect(ColorUtil.HEX_PATTERN.test("#FFF")).toBe(true);
    expect(ColorUtil.HEX_PATTERN.test("#FFFFFFFF")).toBe(true);
  });

  describe("generateDistinctColors", () => {
    it("should generate distinct colors", () => {
      const colors = ColorUtil.generateDistinctColors(5);
      expect(colors.length).toEqual(5);
      // Further tests can compare the generated colors to expected values
    });
  });

  // Testing resolveColor method
  describe("resolveColor", () => {
    it("should resolve RGB color", () => {
      const result = ColorUtil.resolveColor("rgb(255, 0, 0, 1)");
      expect(result).toEqual({
        originalFormat: ColorFormat.RGB,
        newColor: { r: 255, g: 0, b: 0, a: 1 },
      });
    });

    it("should resolve Custom Property", () => {
      const result = ColorUtil.resolveColor("var(--my-color)");
      expect(result).toEqual({
        originalFormat: ColorFormat.RGB,
        newColor: { r: 255, g: 0, b: 0, a: 1 },
      });
    });

    it("should fallback to black color for undefined custom property", () => {
      expect(ColorUtil.resolveColor("var(--undefined-color)")).toEqual({
        newColor: { b: 0, g: 0, r: 0, a: 1 },
        originalFormat: ColorFormat.HEX,
      });
    });
  });

  it("should correctly parse RGB color strings", () => {
    const testCases = [
      { input: "RGB(255, 255, 255)", expected: { r: 255, g: 255, b: 255, a: 1 } },
      { input: "RGB(255 255 255)", expected: { r: 255, g: 255, b: 255, a: 1 } },
      { input: "RGB(255, 255, 255 1)", expected: { r: 255, g: 255, b: 255, a: 1 } },
      { input: "RGB(255 255 255 / 80%)", expected: { r: 255, g: 255, b: 255, a: 0.8 } },
      { input: "RGBA(255, 255, 255 1)", expected: { r: 255, g: 255, b: 255, a: 1 } },
      { input: "RGBA(255 255 255 / 80%)", expected: { r: 255, g: 255, b: 255, a: 0.8 } },
      { input: "RGBA(255, 255, 255, 27%)", expected: { r: 255, g: 255, b: 255, a: 0.27 } },
    ];

    for (const testCase of testCases) {
      const result = ColorUtil.parseColorWithFormat(testCase.input);
      expect(result.color).toEqual(testCase.expected);
      expect(result.format).toEqual(ColorFormat.RGB);
    }
  });

  it("should correctly parse HSL color strings", () => {
    const testCases = [
      { input: "HSL(120, 100%, 50%)", expected: { h: 120, s: 100, l: 50, a: 1 } },
      { input: "HSL(120 100% 50%)", expected: { h: 120, s: 100, l: 50, a: 1 } },
      { input: "HSL(120, 100%, 50% 1)", expected: { h: 120, s: 100, l: 50, a: 1 } },
      { input: "HSL(120 100% 50% / 80%)", expected: { h: 120, s: 100, l: 50, a: 0.8 } },
      { input: "HSLA(120, 100%, 50% 1)", expected: { h: 120, s: 100, l: 50, a: 1 } },
      { input: "HSLA(120 100% 50% / 80%)", expected: { h: 120, s: 100, l: 50, a: 0.8 } },
      { input: "HSLA(120, 100%, 50%, 27%)", expected: { h: 120, s: 100, l: 50, a: 0.27 } },
    ];

    for (const testCase of testCases) {
      const result = ColorUtil.parseColorWithFormat(testCase.input);
      expect(result.color).toEqual(testCase.expected);
      expect(result.format).toEqual(ColorFormat.HSL);
    }
  });

  describe("Alpha Channel Tests", () => {
    // Test RGB with alpha
    it("should correctly parse RGB with alpha", () => {
      const rgb: RGB = ColorUtil.parseColor("rgba(0, 255, 0, 0.5)") as RGB;
      expect(rgb).toEqual({ r: 0, g: 255, b: 0, a: 0.5 });
    });

    // Test HSL with alpha
    it("should correctly parse HSL with alpha", () => {
      const hsl: HSL = ColorUtil.parseColor("hsla(120, 100%, 50%, 0.5)") as HSL;
      expect(hsl).toEqual({ h: 120, s: 100, l: 50, a: 0.5 });
    });

    // Test HEX with alpha
    it("should correctly parse HEX with alpha", () => {
      const hex: HEX = ColorUtil.parseColor("#00FF0080") as HEX;
      expect(hex).toEqual("#00FF0080");
    });

    // Test conversion that includes alpha channel
    it("should retain alpha channel during RGB to HSL conversion", () => {
      const original: RGB = { r: 0, g: 255, b: 0, a: 0.5 } as RGB;
      const converted = ColorUtil.rgbToHsl(original);
      expect(converted.a).toBeCloseTo(original.a ?? 1);
    });

    it("should retain alpha channel during HSL to RGB conversion", () => {
      const original: HSL = { h: 1 / 3, s: 1, l: 0.5, a: 0.5 } as HSL;
      const converted = ColorUtil.hslToRgb(original);
      expect(converted.a).toBeCloseTo(original.a ?? 1);
    });

    it("should retain alpha channel during RGB to HEX conversion", () => {
      const original: RGB = { r: 0, g: 255, b: 0, a: 0.5 } as RGB;
      const converted = ColorUtil.rgbToHex(original);
      expect(converted).toBe("#00FF0080");
    });

    it("should retain alpha channel during HEX to RGB conversion", () => {
      const original: HEX = "#00FF0080";
      const converted = ColorUtil.hexToRgb(original);
      expect(converted.a).toBeCloseTo(0.5);
    });
  });

  // Testing extractCustomPropertyName method
  describe("extractCustomPropertyName", () => {
    it("should extract custom property name", () => {
      const result = (ColorUtil as any).extractCustomPropertyName("var(--my-custom-prop)");
      expect(result).toBe("--my-custom-prop");
    });

    it("should return null for non-custom property", () => {
      const result = (ColorUtil as any).extractCustomPropertyName("rgb(255, 0, 0)");
      expect(result).toBe(null);
    });
  });

  describe("hslToRgb", () => {
    it("should convert HSL to RGB", () => {
      const rgb: RGB = ColorUtil.hslToRgb({ h: 120, s: 1, l: 0.5 });
      expect(rgb).toEqual({ r: 0, g: 255, b: 0, a: 1 });
    });
  });

  describe("rgbToHex", () => {
    it("should convert RGB to HEX", () => {
      const hex: HEX = ColorUtil.rgbToHex({ r: 0, g: 255, b: 0 });
      expect(hex).toEqual("#00FF00");
    });
  });

  describe("hexToRgb", () => {
    it("should convert HEX to RGB", () => {
      const rgb: RGB = ColorUtil.hexToRgb("#00FF00");
      expect(rgb).toEqual({ r: 0, g: 255, b: 0, a: 1 });
    });
  });

  describe("hexToHsl", () => {
    it("should convert HEX to HSL", () => {
      const hsl: HSL = ColorUtil.hexToHsl("#00FF00");
      expect(hsl).toEqual({ h: 120, s: 100, l: 50, a: 1 });
    });
  });

  describe("hslToHex", () => {
    it("should convert HSL to HEX", () => {
      const hex: HEX = ColorUtil.hslToHex({ h: 120, s: 1, l: 0.5 });
      expect(hex).toEqual("#00FF00");
    });
  });

  describe("rgbToHsl", () => {
    it("should convert RGB to HSL", () => {
      const hsl: HSL = ColorUtil.rgbToHsl({ r: 0, g: 255, b: 0 });
      expect(hsl).toEqual({ h: 120, s: 100, l: 50, a: 1 });
    });
  });

  describe("expandShorthand", () => {
    it("should expand RGB shorthand to full form", () => {
      const expanded: string = ColorUtil["expandShorthand"]("rgb(0,255,0)");
      expect(expanded).toEqual("rgba(0, 255, 0, 1)");
    });

    it("should expand HSL shorthand to full form", () => {
      const expanded: string = ColorUtil["expandShorthand"]("hsl(120,100%,50%)");
      expect(expanded).toEqual("hsla(120, 100%, 50%, 1)");
    });

    it("should return the input if it's not a shorthand", () => {
      const input = "rgba(0, 255, 0, 1)";
      const expanded: string = ColorUtil["expandShorthand"](input);
      expect(expanded).toEqual(input);
    });
  });

  describe("parseColor", () => {
    it("should parse RGB string without alpha to RGB object", () => {
      const rgb: RGB | HSL | HEX = ColorUtil.parseColor("rgb(0, 255, 0)");
      expect(rgb).toEqual({ r: 0, g: 255, b: 0, a: 1 }); // Assuming alpha defaults to 1
    });

    it("should parse HSL string without alpha to HSL object", () => {
      const hsl: HSL | RGB | HEX = ColorUtil.parseColor("hsl(120, 100%, 50%)");
      expect(hsl).toEqual({ h: 120, s: 100, l: 50, a: 1 }); // Assuming alpha defaults to 1
    });
  });
  describe("parseColorWithFormat", () => {
    it("should parse RGBA color correctly", () => {
      const result = ColorUtil.parseColorWithFormat("rgba(255,0,0,1)");
      expect(result.color).toEqual({ r: 255, g: 0, b: 0, a: 1 });
      expect(result.format).toEqual(ColorFormat.RGB);
    });

    it("should parse HSLA color correctly", () => {
      const result = ColorUtil.parseColorWithFormat("hsla(0,100%,50%,1)");
      expect(result.color).toEqual({ h: 0, s: 100, l: 50, a: 1 });
      expect(result.format).toEqual(ColorFormat.HSL);
    });

    it("should parse HEX color correctly", () => {
      const result = ColorUtil.parseColorWithFormat("#FF0000");
      expect(result.color).toEqual("#FF0000");
      expect(result.format).toEqual(ColorFormat.HEX);
    });

    it("should throw error for invalid color", () => {
      expect(() => ColorUtil.parseColorWithFormat("invalid")).toThrowError();
    });
  });

  describe("parseColorIntoFormat", () => {
    it("should convert RGB to HEX correctly", () => {
      const result = ColorUtil.parseColorIntoFormat({ r: 255, g: 0, b: 0 }, ColorFormat.HEX);
      expect(result.originalFormat).toEqual(ColorFormat.RGB);
      expect(result.newColor).toEqual("#FF0000");
    });

    it("should convert HSL to RGB correctly", () => {
      const result = ColorUtil.parseColorIntoFormat({ h: 0, s: 100, l: 50 }, ColorFormat.RGB);
      expect(result.originalFormat).toEqual(ColorFormat.HSL);
      // Assuming your conversion method works correctly:
      expect(result.newColor).toEqual({ r: 255, g: 0, b: 0, a: 1 });
    });

    it("should throw error for invalid desired format", () => {
      expect(() =>
        ColorUtil.parseColorIntoFormat(
          {
            r: 255,
            g: 0,
            b: 0,
          },
          "invalidFormat" as any
        )
      ).toThrowError("Invalid desired format.");
    });
  });

  describe("toString", () => {
    it("should convert HSL object to HSL string", () => {
      const colorStr: string = ColorUtil.toString({ h: 120, s: 100, l: 50, a: 1 });
      expect(colorStr).toEqual("hsla(120, 100%, 50%, 1)");
    });

    it("should convert HEX object to HEX string", () => {
      const colorStr: string = ColorUtil.toString("#00FF00");
      expect(colorStr).toEqual("#00FF00");
    });

    // Test RGB to string with standard values (0-255)
    it("should correctly convert standard RGB to string", () => {
      const rgb: RGB = { r: 255, g: 0, b: 0 };
      const result = ColorUtil.toString(rgb);
      expect(result).toBe("rgba(255, 0, 0, 1)");
    });

    // Test RGB to string with fractional values (0-1)
    it("should correctly convert fractional RGB to string", () => {
      const rgb: RGB = { r: 1, g: 0, b: 0 };
      const result = ColorUtil.toString(rgb);
      expect(result).toBe("rgba(255, 0, 0, 1)");
    });

    // Test HSL to string with standard values (0-360 for hue and 0-100 for saturation and lightness)
    it("should correctly convert standard HSL to string", () => {
      const hsl: HSL = { h: 0, s: 100, l: 50 };
      const result = ColorUtil.toString(hsl);
      expect(result).toBe("hsla(0, 100%, 50%, 1)");
    });

    // Test HSL to string with fractional values (0-1 for hue, saturation, and lightness)
    it("should correctly convert fractional HSL to string", () => {
      const hsl: HSL = { h: 1, s: 1, l: 1 };
      const result = ColorUtil.toString(hsl);
      expect(result).toBe("hsla(1, 100%, 100%, 1)");
    });

    // Test HEX to string
    it("should correctly convert HEX to string", () => {
      const hex: HEX = "#FF0000";
      const result = ColorUtil.toString(hex);
      expect(result).toBe("#FF0000");
    });

    // Test invalid input
    it("should throw an error for invalid input", () => {
      const invalidColor = { r: 300, g: 0, b: 0 };
      expect(() => ColorUtil.toString(invalidColor)).toThrow();
    });
  });

  describe("Complex Conversions", () => {
    it("should convert RGB -> HEX -> HSL -> HEX -> RGB -> HSL -> RGB and stay close to the original value", () => {
      const originalRgb: RGB = { r: 123, g: 45, b: 67 };
      const result = ColorUtil.hslToRgb(
        ColorUtil.rgbToHsl(
          ColorUtil.hexToRgb(
            ColorUtil.rgbToHex(
              ColorUtil.hexToRgb(
                ColorUtil.hslToHex(ColorUtil.rgbToHsl(ColorUtil.hexToRgb(ColorUtil.rgbToHex(originalRgb))))
              )
            )
          )
        )
      );
    });

    it("should convert HEX -> HSL -> RGB -> HSL -> HEX -> RGB -> HEX and stay close to the original value", () => {
      const originalHex: HEX = "#7B2D43";
      const result = ColorUtil.rgbToHex(
        ColorUtil.hexToRgb(
          ColorUtil.hslToHex(
            ColorUtil.rgbToHsl(
              ColorUtil.hexToRgb(ColorUtil.hslToHex(ColorUtil.rgbToHsl(ColorUtil.hexToRgb(originalHex))))
            )
          )
        )
      );
      expect(result).toEqual(originalHex);
    });

    it("should convert HSL -> RGB -> HEX -> RGB -> HSL -> HEX -> HSL and stay close to the original value", () => {
      const originalHsl: HSL = { h: 345, s: 50, l: 50 };
      const result = ColorUtil.hexToHsl(
        ColorUtil.hslToHex(
          ColorUtil.rgbToHsl(
            ColorUtil.hexToRgb(
              ColorUtil.rgbToHex(ColorUtil.hslToRgb(ColorUtil.rgbToHsl(ColorUtil.hslToRgb(originalHsl))))
            )
          )
        )
      );
      expect(result.h).toBeCloseTo(originalHsl.h, 0);
      expect(result.s).toBeCloseTo(originalHsl.s, 0);
      expect(result.l).toBeCloseTo(originalHsl.l, 0);
    });
  });

  describe("Lighten and Darken Colors", () => {
    const testData: Array<{ color: Color; percent: number; description: string }> = [
      { color: { r: 255, g: 0, b: 0 }, percent: 20, description: "Lighten red" },
      { color: { h: 120, s: 100, l: 50 }, percent: -20, description: "Darken green in HSL" },
      { color: "#0000FF", percent: 10, description: "Lighten blue in HEX" },
    ];

    test.each(testData)("should lighten and darken colors correctly - $description", ({ color, percent }) => {
      const lightened = ColorUtil.lightenColor(color, percent);
      const darkened = ColorUtil.darkenColor(color, percent);

      expect(lightened).toBeTruthy();
      expect(darkened).toBeTruthy();
    });
  });
});
