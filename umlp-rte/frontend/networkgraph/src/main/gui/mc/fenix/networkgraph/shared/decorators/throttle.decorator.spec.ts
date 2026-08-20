import { applyThrottle, Throttle } from "./throttle.decorator";
import { FunctionType } from "../utils/types"; // Replace with your actual path

vi.useFakeTimers();

describe("Throttle", () => {
  describe("applyThrottle function", () => {
    let counter: number;
    let obj: { increment: FunctionType };

    beforeEach(() => {
      counter = 0;
      obj = {
        increment: function () {
          counter++;
        },
      };
    });

    it("should throttle function execution without trailing", () => {
      obj.increment = applyThrottle(obj.increment, 1000, false);
      obj.increment();
      obj.increment();
      obj.increment();
      expect(counter).toEqual(1);
      vi.advanceTimersByTime(1000);
      obj.increment();
      expect(counter).toEqual(2);
    });

    it("should throttle function execution with trailing", () => {
      obj.increment = applyThrottle(obj.increment, 1000, true);
      obj.increment();
      obj.increment();
      expect(counter).toEqual(1);
      vi.advanceTimersByTime(999);
      expect(counter).toEqual(1);
      vi.advanceTimersByTime(1);
      expect(counter).toEqual(2);
    });
  });

  describe("Throttle decorator", () => {
    class TestClass {
      public counter: number = 0;

      @Throttle(1000, false)
      incrementCounterWithoutTrailing(): void {
        this.counter++;
      }

      @Throttle(1000, true)
      incrementCounterWithTrailing(): void {
        this.counter++;
      }
    }

    let instance: TestClass;

    beforeEach(() => {
      instance = new TestClass();
    });

    it("should throttle function execution without trailing", () => {
      instance.incrementCounterWithoutTrailing();
      instance.incrementCounterWithoutTrailing();
      instance.incrementCounterWithoutTrailing();
      expect(instance.counter).toEqual(1);
      vi.advanceTimersByTime(1000);
      instance.incrementCounterWithoutTrailing();
      expect(instance.counter).toEqual(2);
    });

    it("should throttle function execution with trailing", () => {
      instance.incrementCounterWithTrailing();
      instance.incrementCounterWithTrailing();
      expect(instance.counter).toEqual(1);
      vi.advanceTimersByTime(999);
      expect(instance.counter).toEqual(1);
      vi.advanceTimersByTime(1);
      expect(instance.counter).toEqual(2);
    });
  });
});
