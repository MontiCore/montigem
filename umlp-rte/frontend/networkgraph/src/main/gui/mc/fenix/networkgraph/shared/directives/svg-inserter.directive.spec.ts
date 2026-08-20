import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Component, DebugElement } from "@angular/core";
import { By } from "@angular/platform-browser";
import { SvgInserterDirective } from "./svg-inserter.directive";

@Component({
  template: ` <div [svgInserter]="svgUrl" [svgAlt]="svgAlt"></div>`,
  standalone: true,
  imports: [SvgInserterDirective],
})
class TestHostComponent {
  svgUrl: string | undefined;
  svgAlt: string = "";
}

describe("SvgInserterDirective", () => {
  let component: TestHostComponent;
  let fixture: ComponentFixture<TestHostComponent>;
  let divEl: DebugElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TestHostComponent, SvgInserterDirective],
    });

    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    divEl = fixture.debugElement.query(By.css("div"));
  });

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  it("should create an instance", () => {
    const directive = new SvgInserterDirective(divEl);
    expect(directive).toBeTruthy();
  });

  it("should fetch and insert SVG when svgUrl input changes", async () => {
    globalThis.fetch = vi.fn(() =>
      Promise.resolve({
        ok: true,
        text: () => "<svg></svg>",
      } as unknown as Response)
    );

    component.svgUrl = "http://example.com/test.svg";
    fixture.detectChanges();

    await fixture.whenStable();

    expect(divEl.nativeElement.innerHTML).toContain("<svg></svg>");
  });

  it("should set accessibility attributes when svgAlt is provided", async () => {
    component.svgUrl = "http://example.com/test.svg";
    component.svgAlt = "Test SVG";
    fixture.detectChanges();

    await fixture.whenStable();

    expect(divEl.nativeElement.getAttribute("role")).toBe("img");
    expect(divEl.nativeElement.getAttribute("aria-label")).toBe("Test SVG");
  });
});
