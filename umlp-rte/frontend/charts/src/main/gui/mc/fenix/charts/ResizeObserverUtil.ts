/* (c) https://github.com/MontiCore/monticore */

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
      }, 0);
    }
  });
  resizeObserver.observe(element);
  return resizeObserver;
}
