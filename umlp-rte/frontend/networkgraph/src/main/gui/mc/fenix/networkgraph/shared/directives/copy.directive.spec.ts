import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { vi } from "vitest";

import { CopyDirective } from "./copy.directive";

@Component({
  imports: [CopyDirective],
  template: `<button [copy]="dataToCopy">Click me</button>`,
})
class TestComponent {
  dataToCopy = { key: "value" };
}

describe("CopyDirective", () => {
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;
  let button: HTMLButtonElement;

  beforeEach(async () => {
    vi.stubGlobal("navigator", {
      ...navigator,
      clipboard: {
        writeText: vi.fn(),
      },
    });

    await TestBed.configureTestingModule({
      imports: [TestComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    button = fixture.nativeElement.querySelector("button");
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    TestBed.resetTestingModule();
  });

  it("should create an instance", () => {
    expect(button).toBeDefined();
    expect(button.textContent).toContain("Click me");
  });

  it("should copy data to clipboard", async () => {
    button.click();
    fixture.detectChanges();

    await fixture.whenStable();

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
      JSON.stringify({ key: "value" }, null, 2),
    );
  });
});
