import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Type, ɵresolveComponentResources as resolveComponentResources } from '@angular/core';
import { By } from '@angular/platform-browser';
import { PlotlyModule } from 'angular-plotly.js';
import * as Plotly from 'plotly.js-dist-min';
import { RouterTestingModule } from '@angular/router/testing';
import { DomainManagerClientImpl } from 'domain/DomainManagerClientImpl';
import { DomainComponentObserverManager } from 'DomainComponentObserverManager';

PlotlyModule.plotlyjs = Plotly;

export function chartLayoutTest<T>(
    pageComponent: Type<T>,
    chartSelector: string,
    exclusionSelector: string = 'text, gem-chart-options *'
) {
  describe(`${pageComponent.name} Chart Layout`, () => {
    let fixture: ComponentFixture<T>;
    let chartElement: HTMLElement;
    let containerElement: HTMLElement;

    beforeEach(async () => {
      containerElement = null as any;
      DomainManagerClientImpl.init();
      DomainComponentObserverManager.init();
      // Resolve templateUrl for standalone components before configureTestingModule,
      // because configureTestingModule inspects the ɵcmp getter which throws if
      // templateUrl hasn't been resolved yet (Angular JIT compilation issue).
      await resolveComponentResources(fetch);
      await TestBed.configureTestingModule({
        imports: [pageComponent, PlotlyModule, RouterTestingModule],
      }).compileComponents();

      fixture = TestBed.createComponent(pageComponent);
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      const chartDebugElement = fixture.debugElement.query(By.css(chartSelector));
      if (!chartDebugElement) {
        throw new Error(`Chart element with selector "${chartSelector}" not found`);
      }
      chartElement = chartDebugElement.nativeElement;

      // Find the test container wrapper
      let currentElement = chartElement;
      while (currentElement.parentElement) {
        if (currentElement.classList.contains('test-container')) {
          containerElement = currentElement;
          break;
        }
        currentElement = currentElement.parentElement;
      }

      if (!containerElement) {
        // Fallback to a default container if the border is not found
        containerElement = document.createElement('div');
        containerElement.style.width = '500px';
        containerElement.style.height = '500px';
        containerElement.style.border = '1px solid black';
        containerElement.style.position = 'relative';
        const parent = fixture.nativeElement.parentElement;
        if (parent) {
          parent.insertBefore(containerElement, fixture.nativeElement);
          containerElement.appendChild(fixture.nativeElement);
        } else {
          document.body.appendChild(containerElement);
          containerElement.appendChild(fixture.nativeElement);
        }
      }

      // Wait for the backend data to load and the chart to fully render
      // We poll and wait until the number of SVG elements significantly increases.
      // Wrap detectChanges in try-catch because components might throw when data is not yet fully loaded (e.g. persons is null).
      let retries = 0;
      let previousCount = 0;
      let stableCount = 0;
      while (retries < 60) {
        await new Promise(resolve => setTimeout(resolve, 500));

        try {
          fixture.detectChanges();
        } catch (e) {
          // Ignore errors like "persons is null" during the loading phase
          retries++;
          continue;
        }

        const currentCount = chartElement.querySelectorAll('path, rect, circle, line, polygon, polyline, text').length;

        // If the count is greater than 5 (arbitrary threshold for a non-empty chart) and hasn't changed, it's likely stable.
        if (currentCount > 5 && currentCount === previousCount) {
          stableCount++;
          if (stableCount >= 5) break; // Stable for 5 iterations (2.5 seconds)
        } else {
          stableCount = 0;
        }
        previousCount = currentCount;
        retries++;
      }
    }, 40000);

    afterEach(async () => {
      // Give the backend a brief moment to recover before the next test
      await new Promise(resolve => setTimeout(resolve, 1000));

      if (containerElement && containerElement.parentNode && containerElement.parentNode.contains(containerElement)) {
        containerElement.parentNode.removeChild(containerElement);
      }
      if (fixture) {
        fixture.destroy();
      }
    }, 40000);

    const checkNoOverflow = async (condition: (rect: DOMRect, containerRect: DOMRect, tolerance: number) => boolean) => {
      await new Promise(resolve => setTimeout(resolve, 500));

      const containerRect = containerElement.getBoundingClientRect();
      const elements = chartElement.querySelectorAll('path, rect, circle, line, polygon, polyline, text');
      const tolerance = 2.0;

      for (let i = 0; i < elements.length; i++) {
        const el = elements[i];
        if (el.closest && el.closest('gem-chart-options')) continue;
        if (exclusionSelector !== '' && el.matches && el.matches(exclusionSelector)) continue;

        // Exclude Plotly-internal drag handles and clip rects — these are invisible
        // interaction overlays that intentionally extend beyond the plot area.
        const elClass = el.getAttribute('class') || '';
        if (elClass.includes('drag') || elClass.includes('zoomlayer') || elClass.includes('gridlayer')) continue;
        // Exclude Plotly background/clip rects with no class inside main-svg layers
        if (el.tagName.toLowerCase() === 'rect' && elClass === '' && el.closest && el.closest('.main-svg')) continue;

        const rect = el.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) continue;

        if (condition(rect, containerRect, tolerance)) {
          const errMsg = `Overflowing element: <${el.tagName} class="${el.getAttribute('class') || ''}">. Bounds: top=${rect.top}, left=${rect.left}, bottom=${rect.bottom}, right=${rect.right}. Container: top=${containerRect.top}, left=${containerRect.left}, bottom=${containerRect.bottom}, right=${containerRect.right}`;
          console.error(errMsg);
          return errMsg;
        }
      }
      return null;
    };

    it('should not have elements overflowing the LEFT border', async () => {
      const noLeftOverflow = await checkNoOverflow((rect, containerRect, tolerance) => rect.left < containerRect.left - tolerance);
      if (noLeftOverflow === null) {
        console.log(`\x1b[32m[PASSED]\x1b[0m ${pageComponent.name} - No overflow at the LEFT border.`);
      } else {
        console.error(`\x1b[31m[FAILED]\x1b[0m ${pageComponent.name} - Overflow detected at the LEFT border!`);
      }
      expect(noLeftOverflow, `LEFT border of ${pageComponent.name} overflowed`).toBeNull();
    }, 40000);

    it('should not have elements overflowing the RIGHT border', async () => {
      const noRightOverflow = await checkNoOverflow((rect, containerRect, tolerance) => rect.right > containerRect.right + tolerance);
      if (noRightOverflow === null) {
        console.log(`\x1b[32m[PASSED]\x1b[0m ${pageComponent.name} - No overflow at the RIGHT border.`);
      } else {
        console.error(`\x1b[31m[FAILED]\x1b[0m ${pageComponent.name} - Overflow detected at the RIGHT border!`);
      }
      expect(noRightOverflow, `RIGHT border of ${pageComponent.name} overflowed`).toBeNull();
    }, 40000);

    it('should not have elements overflowing the TOP border', async () => {
      const noTopOverflow = await checkNoOverflow((rect, containerRect, tolerance) => rect.top < containerRect.top - tolerance);
      if (noTopOverflow === null) {
        console.log(`\x1b[32m[PASSED]\x1b[0m ${pageComponent.name} - No overflow at the TOP border.`);
      } else {
        console.error(`\x1b[31m[FAILED]\x1b[0m ${pageComponent.name} - Overflow detected at the TOP border!`);
      }
      expect(noTopOverflow, `TOP border of ${pageComponent.name} overflowed`).toBeNull();
    }, 40000);

    it('should not have elements overflowing the BOTTOM border', async () => {
      const noBottomOverflow = await checkNoOverflow((rect, containerRect, tolerance) => rect.bottom > containerRect.bottom + tolerance);
      if (noBottomOverflow === null) {
        console.log(`\x1b[32m[PASSED]\x1b[0m ${pageComponent.name} - No overflow at the BOTTOM border.`);
      } else {
        console.error(`\x1b[31m[FAILED]\x1b[0m ${pageComponent.name} - Overflow detected at the BOTTOM border!`);
      }
      expect(noBottomOverflow, `BOTTOM border of ${pageComponent.name} overflowed`).toBeNull();
    }, 40000);

    it('should not have elements overflowing bounds after window resize', async () => {
      // Shrink whatever container was found to roughly 60% of its current rendered
      // size so the chart components' ResizeObserver actually detects a change.
      const originalRect = containerElement.getBoundingClientRect();
      containerElement.style.minWidth = '0';
      containerElement.style.width = `${Math.max(originalRect.width * 0.6, 150)}px`;
      containerElement.style.height = `${Math.max(originalRect.height * 0.6, 150)}px`;

      // We rely on the components' internal ResizeObserver to detect the size change
      // and trigger a redraw. Dispatching window resize triggers leaked listeners in angular-plotly.

      // Trigger Angular change detection so the layout dimensions propagate
      fixture.detectChanges();

      // Wait for ResizeObserver callbacks and Plotly re-render to settle.
      // Use a polling loop similar to beforeEach to wait until SVG elements stabilise.
      let previousCount = 0;
      let stableCount = 0;
      for (let i = 0; i < 20; i++) {
        await new Promise(resolve => setTimeout(resolve, 500));
        try { fixture.detectChanges(); } catch (_) { /* ignore */ }
        const currentCount = chartElement.querySelectorAll('path, rect, circle, line, polygon, polyline').length;
        if (currentCount > 0 && currentCount === previousCount) {
          stableCount++;
          if (stableCount >= 4) break; // Stable for 2 seconds
        } else {
          stableCount = 0;
        }
        previousCount = currentCount;
      }

      // Extra wait for Plotly's async rendering pipeline to fully flush
      await new Promise(resolve => setTimeout(resolve, 1000));
      fixture.detectChanges();

      const noLeftOverflow = await checkNoOverflow((rect, containerRect, tolerance) => rect.left < containerRect.left - tolerance);
      const noRightOverflow = await checkNoOverflow((rect, containerRect, tolerance) => rect.right > containerRect.right + tolerance);
      const noTopOverflow = await checkNoOverflow((rect, containerRect, tolerance) => rect.top < containerRect.top - tolerance);
      const noBottomOverflow = await checkNoOverflow((rect, containerRect, tolerance) => rect.bottom > containerRect.bottom + tolerance);

      expect(noLeftOverflow, `LEFT border of ${pageComponent.name} overflowed after resize`).toBeNull();
      expect(noRightOverflow, `RIGHT border of ${pageComponent.name} overflowed after resize`).toBeNull();
      expect(noTopOverflow, `TOP border of ${pageComponent.name} overflowed after resize`).toBeNull();
      expect(noBottomOverflow, `BOTTOM border of ${pageComponent.name} overflowed after resize`).toBeNull();
    }, 40000);
  });
}
