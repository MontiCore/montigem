/* (c) https://github.com/MontiCore/monticore */
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');

// Exercise the component's real selection handling without an Angular renderer or server.
const source = fs.readFileSync(path.join(__dirname,
  '../collabtext/src/main/gui/mc/fenix/collabtext/GemCollabTextarea.ts'), 'utf8');
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, experimentalDecorators: true },
}).outputText;
const decorator = () => () => {};
const sandbox = {
  exports: {}, queueMicrotask: () => {}, document: { activeElement: null },
  require: name => name === '@angular/core' ? {
    Component: decorator, HostBinding: decorator, HostListener: decorator,
    Input: decorator, ViewChild: decorator, forwardRef: fn => fn,
    inject: () => ({}), ChangeDetectionStrategy: {}, ViewEncapsulation: {},
  } : { GemCollabTextareaComponent: class {}, config: { imports: [] } },
};
vm.runInNewContext(compiled, sandbox);

function editor(value, start, end = start, direction = 'none') {
  const component = new sandbox.exports.GemCollabTextarea();
  const published = {};
  component.ownCursor = {
    setAnchor: value => { published.anchor = value; },
    setHead: value => { published.head = value; },
  };
  const input = {
    value, selectionStart: start, selectionEnd: end, selectionDirection: direction,
    scrollTop: 24,
    setSelectionRange(start, end, direction) {
      this.selectionStart = start;
      this.selectionEnd = end;
      this.selectionDirection = direction;
    },
  };
  component.input = { nativeElement: input };
  component.reportedAnchor = direction === 'backward' ? end : start;
  component.reportedHead = direction === 'backward' ? start : end;
  return { component, input, published };
}

test('insertion before an unfocused cursor publishes its shifted text position', () => {
  const { component, input, published } = editor('hello world', 8);
  component.showText('hello big world');
  assert.equal(input.selectionStart, 12);
  assert.deepEqual(published, { anchor: 12, head: 12 });
  assert.equal(input.scrollTop, 24);
});

test('deletion before a backward selection publishes both endpoints and preserves direction', () => {
  const { component, input, published } = editor('hello big world', 10, 14, 'backward');
  component.showText('hello world');
  assert.deepEqual(published, { anchor: 10, head: 6 });
  assert.equal(input.selectionDirection, 'backward');
});

test('an edit after the cursor does not publish an unchanged selection', () => {
  const { component, input, published } = editor('hello world', 2);
  component.showText('hello big world');
  assert.equal(input.selectionStart, 2);
  assert.deepEqual(published, {});
});

test('focused selection reporting still publishes a user movement', () => {
  const { component, input, published } = editor('hello world', 2);
  sandbox.document.activeElement = input;
  input.selectionStart = input.selectionEnd = 4;
  component.reportSelection();
  assert.deepEqual(published, { anchor: 4, head: 4 });
  sandbox.document.activeElement = null;
});
