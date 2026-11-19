/* (c) https://github.com/MontiCore/monticore */

import * as Plotly from 'plotly.js-dist-min';

export function setupResizeObserver(
  element: HTMLElement,
  handleResize: (width: number, height: number) => void
): ResizeObserver {
  // Manually trigger resize logic for initial autoscaling
  setTimeout(() => {
    const initialRect = element.getBoundingClientRect();
    if (initialRect.width > 0 && initialRect.height > 0) {
      handleResize(initialRect.width, initialRect.height);
    }
  }, 0);

  const resizeObserver = new ResizeObserver(entries => {
    for (let entry of entries) {
      setTimeout(() => {
        handleResize(entry.contentRect.width, entry.contentRect.height);
        resizePlots(element);
      }, 0);
    }
  });
  resizeObserver.observe(element);
  return resizeObserver;
}

/**
 * Lets the plotly plots in the element adopt the element's new size. Plotly (and its useResizeHandler) only
 * re-measures its container on window resize, so a plot rendered while its container was larger (e.g., during
 * a layout animation) would otherwise keep that size and be clipped.
 * Runs in the next frame, after the chart re-rendered with the layout changed by handleResize.
 */
function resizePlots(element: HTMLElement) {
  requestAnimationFrame(() => {
    element.querySelectorAll<HTMLElement>('.js-plotly-plot').forEach(plot => {
      if (plot.offsetWidth > 0 && plot.offsetHeight > 0) {
        (Plotly as any).Plots.resize(plot);
      }
    });
  });
}
