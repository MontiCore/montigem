import { TypeCheckerUtil } from "./type-checker.util";

/**
 * Represents the Red-Green-Blue (RGB) color model.
 *
 * @property {number} r - The red component (0-255).
 * @property {number} g - The green component (0-255).
 * @property {number} b - The blue component (0-255).
 * @property {number} [a] - The alpha (opacity) component (0-1), optional.
 */
export type RGB = {
  r: number;
  g: number;
  b: number;
  a?: number;
};

/**
 * Represents the Hue-Saturation-Lightness (HSL) color model.
 *
 * @property {number} h - The hue component (0-360 degrees).
 * @property {number} s - The saturation component (0-1).
 * @property {number} l - The lightness component (0-1).
 * @property {number} [a] - The alpha (opacity) component (0-1), optional.
 */
export type HSL = {
  h: number;
  s: number;
  l: number;
  a?: number;
};

/** Represents the Hexadecimal (HEX) color format. */
export type HEX = string;

/** Enum to define the possible color formats: RGB, HSL, HEX. */
export enum ColorFormat {
  RGB = "rgb",
  HSL = "hsl",
  HEX = "hex",
}

/** Represents a general color type that can be either RGB, HSL, or HEX. */
export type Color = RGB | HSL | HEX;

/**
 * Utility class that provides functionality for colors. Examples are conversions (i.e., {@link hexToHsl} or
 * {@link rgbToHex}), functionality to generate n distinct colors ({@link generateDistinctColors}), verify valid color
 * inputs (i.e., {@link isValidRgb}), and convert color objects into strings ({@link toString}).
 *
 * Some methods were inspired by:
 *
 * - https://stackoverflow.com/a/5624139
 * - https://stackoverflow.com/a/9493060
 */
export class ColorUtil {
  // Color Patterns
  /**
   * Regular expression pattern to match RGBA and RGB color strings. Groups capture R, G, B and optionally A components.
   * A component can be a float or integer.
   */
  public static readonly RGBA_PATTERN: RegExp =
    /rgba?\((\d+)[\s,]+(\d+)[\s,]+(\d+)(?:[\s,]+\/?[\s,]*(\d*(?:\.\d+)?|\.\d+|(\d+)%))?\)/i;

  /**
   * Regular expression pattern to match HSLA and HSL color strings. Groups capture H, S, L and optionally A components.
   * A component can be a float or integer.
   */
  public static readonly HSLA_PATTERN: RegExp =
    /hsla?\((\d+)[\s,]+(\d+)%[\s,]+(\d+)%(?:[\s,]+\/?[\s,]*(\d*(?:\.\d+)?|\.\d+|(\d+)%))?\)/i;

  /**
   * Regular expression pattern to match HEX color strings. Supports 3, 6, and 8 digit hex codes, where the last two
   * digits represent the alpha channel.
   */
  public static readonly HEX_PATTERN: RegExp = /^#(?:[\dA-Fa-f]{3}){1,2}(?:[\dA-Fa-f]{2})?$/;

  // Color range constants
  /** @constant {number} ALPHA_RANGE - Maximum value for the alpha channel. */
  private static readonly ALPHA_RANGE: number = 1;

  /** @constant {number} RGB_RANGE - Maximum value for RGB components. */
  private static readonly RGB_RANGE: number = 255;

  /** @constant {number} SATURATION_RANGE - Maximum value for the saturation component in HSL. */
  private static readonly SATURATION_RANGE: number = 100;

  /** @constant {number} LIGHTNESS_RANGE - Maximum value for the lightness component in HSL. */
  private static readonly LIGHTNESS_RANGE: number = 100;

  /** @constant {number} HUE_RANGE - Maximum value for the hue component in HSL. */
  private static readonly HUE_RANGE: number = 360;

  /**
   * Resolves a given color value, potentially being a custom property, into an actual color value.
   *
   * @param {Color | string} value - The color value or custom property to resolve.
   * @param {ColorFormat} [desiredFormat] - The desired color format to return. Default is `ColorFormat.RGB`
   * @param {Color} [fallbackColor] - The fallback color when the color could not be resolved. Default is `#000000`
   * @returns {Color} The resolved color value in the desired format.
   */
  public static resolveColor(
    value: Color | string,
    desiredFormat: ColorFormat = ColorFormat.RGB,
    fallbackColor: Color = "#000000"
  ): { originalFormat: ColorFormat; newColor: Color } {
    const customPropertyName: string | null = TypeCheckerUtil.isString(value)
      ? this.extractCustomPropertyName(value)
      : null;

    if (!customPropertyName) {
      return this.parseColorIntoFormat(value, desiredFormat);
    }

    const computedColor: string = this.expandShorthand(
      getComputedStyle(document.documentElement).getPropertyValue(customPropertyName).trim()
    );

    // Use black as fallback color when custom property couldn't be resolved
    return this.parseColorIntoFormat(computedColor || fallbackColor, desiredFormat);
  }

  /**
   * Generates a list of "distinct" colors.
   *
   * @param {number} numberOfColors - Number of distinct colors to generate.
   * @param {ColorFormat} colorFormat - The color format of the generated colors.
   * @returns {Color[]} An array of distinct HEX colors.
   */
  public static generateDistinctColors(numberOfColors: number, colorFormat: ColorFormat = ColorFormat.RGB): Color[] {
    const saturation: number = 0.9; // Increased saturation for more vibrant colors
    const lightness: number = 0.6; // Adjusted lightness
    return Array.from({ length: numberOfColors }, (_, index: number): Color => {
      const h: number = (index * this.HUE_RANGE) / numberOfColors;
      return this.parseColorIntoFormat({ h, s: saturation, l: lightness }, colorFormat).newColor;
    });
  }

  /**
   * Converts an HSL color value to RGB.
   *
   * @param {HSL} hsl - HSL color value to be converted.
   * @returns {RGB} RGB representation.
   */
  public static hslToRgb(hsl: HSL): RGB {
    let { h, s, l, a = this.ALPHA_RANGE } = this.normalizeHsl(hsl);
    h /= this.HUE_RANGE;
    s /= this.SATURATION_RANGE;
    l /= this.LIGHTNESS_RANGE;
    let r: number, g: number, b: number;
    if (s === 0) {
      r = g = b = l; // achromatic
    } else {
      const q: number = l < 0.5 ? l * (1 + s) : l + s - l * s;
      const p: number = 2 * l - q;
      r = this.hueToRgb(p, q, h + 1 / 3);
      g = this.hueToRgb(p, q, h);
      b = this.hueToRgb(p, q, h - 1 / 3);
    }

    return {
      r: Math.round(r * this.RGB_RANGE),
      g: Math.round(g * this.RGB_RANGE),
      b: Math.round(b * this.RGB_RANGE),
      a,
    };
  }

  /**
   * Converts an RGB value to a HEX string representation.
   *
   * @param {RGB} rgb - RGB value to be converted.
   * @returns {HEX} HEX representation.
   */

  public static rgbToHex(rgb: RGB): HEX {
    const { r, g, b, a } = this.normalizeRGB(rgb);
    const alphaHex: string = a !== undefined && a !== this.ALPHA_RANGE ? this.componentToHex(Math.round(a * 255)) : "";
    return ("#" + this.componentToHex(r) + this.componentToHex(g) + this.componentToHex(b) + alphaHex).toUpperCase();
  }

  /**
   * Converts a HEX color value to RGB.
   *
   * @param {HEX} hex - HEX color value to be converted.
   * @returns {RGB} RGB representation.
   */
  public static hexToRgb(hex: HEX): RGB {
    const shorthandRegex: RegExp = /^#?([\da-f])([\da-f])([\da-f])$/i;
    hex = hex.replace(shorthandRegex, (m: string, r: string, g: string, b: string) => {
      return r + r + g + g + b + b;
    });

    const result: RegExpExecArray | null = /^#?([\da-f]{2})([\da-f]{2})([\da-f]{2})([\da-f]{2})?$/i.exec(hex);
    return result
      ? {
          r: Number.parseInt(result[1], 16),
          g: Number.parseInt(result[2], 16),
          b: Number.parseInt(result[3], 16),
          a: result[4] === undefined ? this.ALPHA_RANGE : Number.parseInt(result[4], 16) / this.RGB_RANGE,
        }
      : { r: 0, g: 0, b: 0, a: this.ALPHA_RANGE };
  }

  /**
   * Converts a HEX color value to HSL.
   *
   * @param {HEX} hex - HEX color value to be converted.
   * @returns {HSL} HSL representation.
   */
  public static hexToHsl(hex: HEX): HSL {
    return this.rgbToHsl(this.hexToRgb(hex));
  }

  /**
   * Converts an HSL color value to HEX.
   *
   * @param {HSL} hsl - HSL color value to be converted.
   * @returns {HEX} HEX representation.
   */
  public static hslToHex(hsl: HSL): HEX {
    return this.rgbToHex(this.hslToRgb(hsl));
  }

  /**
   * Converts an RGB color value to HSL representation. The conversion formula involves calculating the maximum,
   * minimum, and the difference between them to derive the Hue, Saturation, and Lightness.
   *
   * @param {RGB} rgb - RGB color value to be converted.
   * @returns {HSL} HSL representation.
   */
  public static rgbToHsl(rgb: RGB): HSL {
    let { r, g, b, a = this.ALPHA_RANGE } = this.normalizeRGB(rgb);
    r /= this.RGB_RANGE;
    g /= this.RGB_RANGE;
    b /= this.RGB_RANGE;

    const vmax: number = Math.max(r, g, b);
    const vmin: number = Math.min(r, g, b);
    let h: number = 0;
    let s: number;
    const l: number = (vmax + vmin) / 2;

    if (vmax === vmin) {
      h = 0;
      s = 0; // achromatic
    } else {
      const d: number = vmax - vmin;
      s = l > 0.5 ? d / (2 - vmax - vmin) : d / (vmax + vmin);
      if (vmax === r) h = (g - b) / d + (g < b ? 6 : 0);
      if (vmax === g) h = (b - r) / d + 2;
      if (vmax === b) h = (r - g) / d + 4;
      h /= 6;
    }

    return {
      h: h * this.HUE_RANGE,
      s: s * this.SATURATION_RANGE,
      l: l * this.LIGHTNESS_RANGE,
      a,
    };
  }

  /**
   * Change the lightness of an HSL color by a given percentage
   *
   * @param {HSL} hsl - The original HSL color
   * @param {number} percent - The percentage to lighten/darken the color, range -100 to 100
   * @returns {HSL} - The modified HSL color
   */
  public static changeLightness(hsl: HSL, percent: number): HSL {
    let { h, s, l, a } = this.normalizeHsl(hsl);
    l /= this.LIGHTNESS_RANGE;
    const newL: number = Math.min(1, Math.max(0, l + (1 - l) * (percent / 100))) * this.LIGHTNESS_RANGE;
    return { h: h, s: s, l: newL, a };
  }

  /**
   * Lighten a given color
   *
   * @param {Color} color - The original color in any supported format
   * @param {number} percent - The percentage to lighten the color, range 0 to 100
   * @returns {Color} - The lightened color in the original format
   */
  public static lightenColor(color: Color, percent: number): Color {
    if (!color) return color;
    const { originalFormat, newColor } = this.parseColorIntoFormat(color, ColorFormat.HSL);
    const lighterColor: HSL = this.changeLightness(newColor as HSL, percent);
    return this.parseColorIntoFormat(lighterColor, originalFormat).newColor;
  }

  /**
   * Darken a given color
   *
   * @param {Color} color - The original color in any supported format
   * @param {number} percent - The percentage to darken the color, range 0 to 100
   * @returns {Color} - The darkened color in the original format
   */
  public static darkenColor(color: Color, percent: number): Color {
    return this.lightenColor(color, -percent);
  }

  /**
   * Parses a color string into its RGB, HSL, or HEX representation. Calls the 'parseColorWithFormat' method and
   * extracts only the color.
   *
   * @param {string} value - Color value string to be parsed.
   * @returns {Color} Parsed color representation.
   */
  public static parseColor(value: string): Color {
    return this.parseColorWithFormat(value).color;
  }

  /**
   * Parses a color string into its RGB, HSL, or HEX representation and also returns the format. It can handle different
   * color string formats like rgba, hsla, and hex and returns the corresponding color object.
   *
   * @param {string} value - Color value string to be parsed.
   * @returns {{ color: Color; format: ColorFormat }} - Parsed color representation including the format as a string.
   */
  public static parseColorWithFormat(value: string): { color: Color; format: ColorFormat } {
    value = this.expandShorthand(value); // Expand shorthand if present

    if (this.RGBA_PATTERN.test(value)) {
      // Parse RGBA color
      const [, r, g, b, a = this.ALPHA_RANGE] = value.match(this.RGBA_PATTERN)!;
      return { color: this.normalizeRGB({ r: +r, g: +g, b: +b, a: this.parseAlpha(a) }), format: ColorFormat.RGB };
    }

    if (this.HSLA_PATTERN.test(value)) {
      // Parse HSLA color
      const [, h, s, l, a = this.ALPHA_RANGE] = value.match(this.HSLA_PATTERN)!;
      return { color: this.normalizeHsl({ h: +h, s: +s, l: +l, a: this.parseAlpha(a) }), format: ColorFormat.HSL };
    }

    if (this.HEX_PATTERN.test(value)) return { color: value, format: ColorFormat.HEX }; // Return HEX color

    // Fall back to resolving CSS named colors (e.g. "red") via the browser's own CSS parser.
    const normalized: string | undefined = this.resolveNamedColor(value);
    if (normalized) {
      if (this.HEX_PATTERN.test(normalized)) return { color: normalized, format: ColorFormat.HEX };
      if (this.RGBA_PATTERN.test(normalized)) {
        const [, r, g, b, a = this.ALPHA_RANGE] = normalized.match(this.RGBA_PATTERN)!;
        return { color: this.normalizeRGB({ r: +r, g: +g, b: +b, a: this.parseAlpha(a) }), format: ColorFormat.RGB };
      }
    }

    throw new Error("Invalid color value: " + value);
  }

  /**
   * Resolves a CSS named color (e.g. "red", "rebeccapurple") into its normalized HEX/`rgb()`/`rgba()` representation
   * using the browser's own CSS parser. Returns `undefined` if the value is not a valid CSS color.
   *
   * @param {string} value - The color value to resolve.
   * @returns {string | undefined} The normalized color string, or `undefined` if invalid.
   */
  private static resolveNamedColor(value: string): string | undefined {
    if (typeof CSS === "undefined" || !CSS.supports("color", value)) return undefined;
    const context: CanvasRenderingContext2D = document.createElement("canvas").getContext("2d")!;
    context.fillStyle = value;
    return context.fillStyle;
  }

  /**
   * Parses a color into the desired format (RGB, HSL, or HEX) and returns an object that contains the original format
   * and the new color. This method first identifies the original format of the provided color, and then based on the
   * desired format, converts the color accordingly.
   *
   * @param {Color | string} color - Color value in any format (RGB, HSL, HEX, or string) to be parsed.
   * @param {ColorFormat} desiredFormat - Desired format (RGB, HSL, or HEX) for the conversion.
   * @returns {{ originalFormat: ColorFormat; newColor: Color }} Object containing the original format and the new
   *   color.
   */
  public static parseColorIntoFormat(
    color: Color | string,
    desiredFormat: ColorFormat
  ): {
    originalFormat: ColorFormat;
    newColor: Color;
  } {
    let originalFormat: ColorFormat;
    // Determine the original format of the color. If string, parse it using 'parseColorWithFormat'.
    if (TypeCheckerUtil.isString(color)) {
      const { color: parsedColor, format } = this.parseColorWithFormat(color);
      color = parsedColor;
      originalFormat = format;
    } else {
      originalFormat = this.isValidRgb(color)
        ? ColorFormat.RGB
        : this.isValidHsl(color)
          ? ColorFormat.HSL
          : ColorFormat.HEX;
    }

    // Convert the color to the desired format based on the original format.
    switch (desiredFormat) {
      case ColorFormat.RGB: {
        if (originalFormat === ColorFormat.HEX) return { originalFormat, newColor: this.hexToRgb(color as HEX) };
        if (originalFormat === ColorFormat.HSL) return { originalFormat, newColor: this.hslToRgb(color as HSL) };
        return { originalFormat, newColor: color as RGB };
      }
      case ColorFormat.HSL: {
        if (originalFormat === ColorFormat.HEX) return { originalFormat, newColor: this.hexToHsl(color as HEX) };
        if (originalFormat === ColorFormat.RGB) return { originalFormat, newColor: this.rgbToHsl(color as RGB) };
        return { originalFormat, newColor: color as HSL };
      }
      case ColorFormat.HEX: {
        if (originalFormat === ColorFormat.RGB) return { originalFormat, newColor: this.rgbToHex(color as RGB) };
        if (originalFormat === ColorFormat.HSL) return { originalFormat, newColor: this.hslToHex(color as HSL) };
        return { originalFormat, newColor: color as HEX };
      }
      default: {
        throw new Error("Invalid desired format.");
      }
    }
  }

  /**
   * Converts a color object into its string representation.
   *
   * @param {Color} color - Color object to be converted.
   * @returns {string} String representation of the color.
   */
  public static toString(color: Color): string {
    if (this.isValidRgb(color)) {
      color = this.normalizeRGB(color);
      return `rgba(${color.r}, ${color.g}, ${color.b}, ${color.a ?? 1})`;
    } else if (this.isValidHsl(color)) {
      color = this.normalizeHsl(color);
      return `hsla(${color.h}, ${color.s}%, ${color.l}%, ${color.a ?? 1})`;
    } else if (this.isValidHex(color)) {
      return color;
    }

    throw new Error("Unknown color object: " + color);
  }

  /**
   * Expands shorthand RGB, HEX and HSL values to their full representations.
   *
   * @param {string} value - Value to be expanded.
   * @returns {string} Expanded color representation.
   */
  public static expandShorthand(value: string): string {
    if (this.HEX_PATTERN.test(value) && value.length === 4) {
      return value.replaceAll(/([\dA-Fa-f])/g, "$1$1");
    }
    if (this.RGBA_PATTERN.test(value)) {
      const match: RegExpMatchArray | null = value.match(this.RGBA_PATTERN);
      const [, r, g, b, a = "1"] = match!;
      return `rgba(${r}, ${g}, ${b}, ${a})`;
    }
    if (this.HSLA_PATTERN.test(value)) {
      const match: RegExpMatchArray | null = value.match(this.HSLA_PATTERN);
      const [, h, s, l, a = "1"] = match!;
      return `hsla(${h}, ${s}%, ${l}%, ${a})`;
    }
    return value;
  }

  /**
   * Validates if a given value adheres to the RGB format.
   *
   * @param {any} value - Value to be checked.
   * @returns {boolean} True if the value is a valid RGB, otherwise false.
   */
  public static isValidRgb(value: any): value is RGB {
    const alpha: number = this.parseAlpha(value.a);
    return (
      typeof value === "object" &&
      Number.isInteger(value?.r) &&
      value.r >= 0 &&
      value.r <= this.RGB_RANGE &&
      Number.isInteger(value?.g) &&
      value.g >= 0 &&
      value.g <= this.RGB_RANGE &&
      Number.isInteger(value?.b) &&
      value.b >= 0 &&
      value.b <= this.RGB_RANGE &&
      (alpha === undefined || (alpha >= 0 && alpha <= this.ALPHA_RANGE))
    );
  }

  /**
   * Validates if a given value adheres to the HSL format.
   *
   * @param {any} value - Value to be checked.
   * @returns {boolean} True if the value is a valid HSL, otherwise false.
   */
  public static isValidHsl(value: any): value is HSL {
    const alpha: number = this.parseAlpha(value.a);
    return (
      typeof value === "object" &&
      typeof value?.h === "number" &&
      value.h >= 0 &&
      value.h <= this.HUE_RANGE &&
      typeof value?.s === "number" &&
      value.s >= 0 &&
      value.s <= this.SATURATION_RANGE &&
      typeof value?.l === "number" &&
      value.l >= 0 &&
      value.l <= this.LIGHTNESS_RANGE &&
      (alpha === undefined || (alpha >= 0 && alpha <= this.ALPHA_RANGE))
    );
  }

  /**
   * Normalizes RGB values. If values are fractions (0-1), converts them to the range 0-255.
   *
   * @param {RGB} rgb - RGB value to be normalized.
   * @returns {RGB} Normalized RGB value.
   */
  public static normalizeRGB(rgb: RGB): RGB {
    let { r, g, b, a = this.ALPHA_RANGE } = rgb;
    a = this.parseAlpha(a);

    if (r <= 1 && g <= 1 && b <= 1) {
      // We only multiply by the range if all components are <= 1. In that case, we assume "rgb(1, 0, 0)" rather than
      // "rgb(255, 0, 0)"
      r *= this.RGB_RANGE;
      g *= this.RGB_RANGE;
      b *= this.RGB_RANGE;
    }

    return {
      r: Math.round(Math.min(Math.max(0, r), this.RGB_RANGE)),
      g: Math.round(Math.min(Math.max(0, g), this.RGB_RANGE)),
      b: Math.round(Math.min(Math.max(0, b), this.RGB_RANGE)),
      a,
    };
  }

  /**
   * Validates if a given value adheres to the HEX format.
   *
   * @param {any} value - Value to be checked.
   * @returns {boolean} True if the value is a valid HEX, otherwise false.
   */
  public static isValidHex(value: unknown): value is HEX {
    return typeof value === "string" && this.HEX_PATTERN.test(value);
  }

  /**
   * Parses the given color into a HEX format, or returns the provided fallback color if the color is undefined.
   *
   * @param {Color | undefined | null} color - The color to parse.
   * @param {HEX} [fallbackColor] - The fallback color to use if the color is undefined, default is "#000".
   * @returns {HEX | undefined} The HEX representation of the color, or the fallback color.
   */
  public static parseIntoHEX(color: Color | undefined | null, fallbackColor: HEX = "#000"): HEX {
    if (color == null) return fallbackColor;
    return (ColorUtil.parseColorIntoFormat(color, ColorFormat.HEX).newColor as HEX) || fallbackColor;
  }

  /**
   * Helper method to extract the alpha value for float or percentage alpha values.
   *
   * @param {string} alpha - The alpha value to extract from.
   * @returns {number} - The alpha value.
   */
  private static parseAlpha(alpha: unknown): number {
    if (TypeCheckerUtil.isNumber(alpha)) return alpha;
    if (!TypeCheckerUtil.isString(alpha)) return this.ALPHA_RANGE;
    if (alpha.endsWith("%")) return Number.parseFloat(alpha) / 100;
    return Number.parseFloat(alpha);
  }

  /**
   * Converts a numeric RGB component to its HEX string representation.
   *
   * @param {number} c - RGB component value.
   * @returns {string} HEX representation of the RGB component.
   */
  private static componentToHex(c: number): string {
    const hex: string = c.toString(16);
    return hex.length === 1 ? "0" + hex : hex;
  }

  /**
   * Helper function used during the HSL to RGB conversion.
   *
   * @param {number} p - First parameter involved in the hue conversion.
   * @param {number} q - Second parameter involved in the hue conversion.
   * @param {number} t - Third parameter involved in the hue conversion.
   * @returns {number} RGB value after hue conversion.
   */
  private static hueToRgb(p: number, q: number, t: number): number {
    if (t < 0) t += 1;
    if (t > 1) t -= 1;
    if (t < 1 / 6) return p + (q - p) * 6 * t;
    if (t < 1 / 2) return q;
    if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
    return p;
  }

  /**
   * Normalizes HSL values. If saturation and lightness are percentage (0-100%), converts them to the range [0-1].
   *
   * @param {HSL} hsl - HSL value to be normalized.
   * @returns {HSL} Normalized HSL value.
   */
  private static normalizeHsl(hsl: HSL): HSL {
    let { h, s, l, a = this.ALPHA_RANGE } = hsl;
    a = this.parseAlpha(a);

    // Ensure hue is within the range of 0 to 360
    h %= this.HUE_RANGE;
    if (h < 0) h += this.HUE_RANGE;

    // Multiply by the given ranges when s & l are <= 1. Then we assume "hsl(120, 1, .5)" instead of "hsl(120, 100%,
    // 50%)".
    if (s <= 1 && l <= 1) {
      s *= this.SATURATION_RANGE;
      l *= this.LIGHTNESS_RANGE;
    }
    // Ensure saturation and lightness are within the range of 0 to 100
    s = Math.min(Math.max(s, 0), this.SATURATION_RANGE);
    l = Math.min(Math.max(l, 0), this.LIGHTNESS_RANGE);

    return {
      h,
      s,
      l,
      a,
    };
  }

  /**
   * Extracts the custom property name from a given value string.
   *
   * @param {string} value - The string from which to extract the custom property name.
   * @returns {string | null} The extracted custom property name, or null if not a custom property.
   */
  private static extractCustomPropertyName(value: string): string | null {
    const match: RegExpMatchArray | null = value.match(/var\(--([\w-]+)\)/);
    return match ? `--${match[1]}` : null;
  }
}
