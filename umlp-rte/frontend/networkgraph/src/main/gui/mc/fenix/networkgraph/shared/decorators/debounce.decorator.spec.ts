import { applyDebounce, Debounce } from "./debounce.decorator";
import { vi, Mock } from 'vitest';

describe("Debounce", () => {
  const timeout: number = 100;

  describe("applyDebounce", () => {
    let mockFunction: Mock;
    let debouncedFunction: any;

    beforeEach(() => {
      vi.useFakeTimers();
      mockFunction = vi.fn();
      debouncedFunction = applyDebounce(mockFunction, timeout);
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it("should not call the function immediately", () => {
      debouncedFunction();

      expect(mockFunction).not.toHaveBeenCalled();
    });

    it("should call the function after the timeout", () => {
      debouncedFunction();

      vi.advanceTimersByTime(timeout);

      expect(mockFunction).toHaveBeenCalled();
    });
  });

  describe("Debounce Decorator", () => {
    class TestComponent {
      public testProperty: string = "";

      @Debounce(timeout)
      method(): void {
        this.testProperty = "method called";
      }
    }

    let component: TestComponent;

    beforeEach(() => {
      vi.useFakeTimers();
      component = new TestComponent();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it("should not call the method immediately", () => {
      component.method();

      expect(component.testProperty).toEqual("");
    });

    it("should call the method after the timeout", () => {
      component.method();

      vi.advanceTimersByTime(timeout);

      expect(component.testProperty).toEqual("method called");
    });
  });
});
