import { Subscription } from "rxjs";
import { AutoUnsubscribe, AutoUnsubscriber } from "./auto-unsubscribe.decorator";
import { vi, MockInstance } from 'vitest';

describe("AutoUnsubscriber", () => {
  describe("performUnsubscription", () => {
    let mockSubscription: Subscription;
    let unsubscribeSpy: MockInstance;

    beforeEach(() => {
      mockSubscription = new Subscription();
      unsubscribeSpy = vi.spyOn(mockSubscription, "unsubscribe");
    });

    it("should call unsubscribe when performUnsubscription is called with a Subscription", () => {
      AutoUnsubscriber.performUnsubscription(mockSubscription);
      expect(unsubscribeSpy).toHaveBeenCalled();
    });

    it("should not call unsubscribe when performUnsubscription is called with a non-Subscription", () => {
      AutoUnsubscriber.performUnsubscription(null as any);
      expect(unsubscribeSpy).not.toHaveBeenCalled();
    });

    it("should call unsubscribe for all subscriptions when unsubscribeFromArray is called", () => {
      AutoUnsubscriber.unsubscribeFromArray([mockSubscription, mockSubscription]);
      expect(unsubscribeSpy).toHaveBeenCalledTimes(2);
    });

    it("should throw an error if unsubscribeFromArray is called with a non-array", () => {
      expect(() => AutoUnsubscriber.unsubscribeFromArray(null as any)).toThrowError(TypeError);
    });
  });

  describe("AutoUnsubscribe Decorator", () => {
    let testSubscriptionSpy: MockInstance;

    @AutoUnsubscribe()
    class TestComponent {
      testSubscription = new Subscription();

      ngOnDestroy() {
        // Original ngOnDestroy logic
      }
    }

    let component: TestComponent;

    beforeEach(() => {
      component = new TestComponent();
      testSubscriptionSpy = vi.spyOn(component.testSubscription, "unsubscribe");
    });

    it("should call unsubscribe on all subscriptions when ngOnDestroy is called", () => {
      component.ngOnDestroy();
      expect(testSubscriptionSpy).toHaveBeenCalled();
    });
  });
});
