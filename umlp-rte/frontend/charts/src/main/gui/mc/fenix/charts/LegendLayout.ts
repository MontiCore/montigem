/* (c) https://github.com/MontiCore/monticore */

// Estimates the size of a Plotly legend before it is rendered, so that a chart can decide
// whether a legend next to the plot still fits into its container.

const LEGEND_FONT_SIZE = 12;
// symbol and spacing Plotly adds to the text of a legend entry (rather too much than too little)
const ENTRY_DECORATION = 50;
const ROW_HEIGHT = 20;
const LEGEND_PADDING = 10;
// share of the chart width a legend next to the plot may take at most
const MAX_SIDE_LEGEND_SHARE = 0.35;

let canvasContext: CanvasRenderingContext2D | null = null;

function measureTextWidth(text: string): number {
  if (typeof document === 'undefined' || !text) {
    return 0;
  }
  if (!canvasContext) {
    canvasContext = document.createElement('canvas').getContext('2d');
  }
  if (!canvasContext) {
    return text.length * LEGEND_FONT_SIZE * 0.6;
  }
  canvasContext.font = `${LEGEND_FONT_SIZE}px "Open Sans", verdana, arial, sans-serif`;
  return canvasContext.measureText(text).width;
}

function entryWidths(names: string[], fontSize: number = LEGEND_FONT_SIZE): number[] {
  const scale = fontSize / LEGEND_FONT_SIZE;
  return names.map(name => ENTRY_DECORATION + measureTextWidth(String(name ?? '').replace(/<[^>]+>/g, '')) * scale);
}

const MIN_FALLBACK_FONT_SIZE = 9;

/**
 * Horizontal legend above the plot for charts too narrow for a legend next to it. It is centered on the
 * whole chart (legend coordinates are relative to the plot area, which is shifted by the margins) and
 * uses a smaller font if a single entry would otherwise be wider than the chart.
 * Returns the legend layout and the height it needs above the plot.
 */
export function topLegendFallback(names: string[], chartWidth: number, marginLeft: number, marginRight: number)
    : { legend: any, height: number } {
  const plotWidth = Math.max(1, chartWidth - marginLeft - marginRight);
  const widest = Math.max(0, ...entryWidths(names)) + LEGEND_PADDING;
  const available = chartWidth - 4;
  const fontSize = widest > available
    ? Math.max(MIN_FALLBACK_FONT_SIZE, Math.floor(LEGEND_FONT_SIZE * available / widest))
    : LEGEND_FONT_SIZE;

  const widths = entryWidths(names, fontSize);
  let rows = 1;
  let rowWidth = 0;
  for (const w of widths) {
    if (rowWidth > 0 && rowWidth + w > available) {
      rows++;
      rowWidth = 0;
    }
    rowWidth += w;
  }

  return {
    legend: {
      orientation: 'h', yanchor: 'bottom', y: 1.02, xanchor: 'center',
      x: (chartWidth / 2 - marginLeft) / plotWidth,
      font: { size: fontSize }
    },
    height: rows * ROW_HEIGHT + LEGEND_PADDING
  };
}

/** width of a vertical legend, i.e. of its widest entry */
export function verticalLegendWidth(names: string[]): number {
  const widths = entryWidths(names);
  return widths.length > 0 ? Math.ceil(Math.max(...widths)) + LEGEND_PADDING : 0;
}

/** whether a vertical legend fits next to the plot of a chart with the given width */
export function fitsSideLegend(names: string[], chartWidth: number): boolean {
  return verticalLegendWidth(names) <= chartWidth * MAX_SIDE_LEGEND_SHARE;
}
