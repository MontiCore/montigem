/* (c) https://github.com/MontiCore/monticore */
import { config, MonacoEditorComponent } from '../../../../../../build/gui2java2ts/mc/fenix/editor/MonacoEditorComponent';
import {Component, EventEmitter, Output, Provider, ElementRef, AfterViewInit, OnDestroy, HostBinding, NgZone} from '@angular/core';
import { FormsModule } from '@angular/forms';
import {MonacoEditorModule, NGX_MONACO_EDITOR_CONFIG, NgxMonacoEditorConfig } from 'ngx-monaco-editor-v2';
import * as monacoTypes from "monaco-editor";

// This is necessary so we use the same static functions as the ngx monaco editor instance
// See: https://stackoverflow.com/questions/79536928/cannot-make-the-model-markers-to-appear-in-monaco-editor-inside-angular-compone
declare var monaco: typeof monacoTypes;

/** Light themes that are replaced by their dark counterpart while the dark mode of the app is active */
const DARK_THEME_OF: { [theme: string]: string } = {
  'vs': 'vs-dark',
  'vs-light': 'vs-dark',
  'Light +': 'vs-dark',
  'hc-light': 'hc-black',
};

/** The dark mode of the app is set by GemDarkModeSwitch */
function isDarkMode(): boolean {
  return document.body.classList.contains('dark-theme');
}

export function provideMonacoEditor(config: NgxMonacoEditorConfig = {}): Provider[] {
  return [
    { provide: NGX_MONACO_EDITOR_CONFIG, useValue: config }
  ];
}

import {InlineDecoration} from "../../../../../../build/gui2java2ts/mc/fenix/editor/monacoeditortypes/InlineDecoration";
import {HoverContent} from "../../../../../../build/gui2java2ts/mc/fenix/editor/monacoeditortypes/HoverContent";
import {LineClickEvent} from "../../../../../../build/gui2java2ts/mc/fenix/editor/monacoeditortypes/LineClickEvent";
import {LineClickEventBuilder} from "../../../../../../build/gui2java2ts/mc/fenix/editor/monacoeditortypes/LineClickEventBuilder";

@Component({
  standalone:true,
  imports:[...config.imports, MonacoEditorModule, FormsModule],
  selector: config.selector,
  providers: [
    provideMonacoEditor ({
      baseUrl: 'assets',
      defaultOptions: {
        scrollBeyondLastLine: false,
        theme: 'vs-dark'
      }
    })
  ],
  templateUrl: "MonacoEditor.html",
  styleUrls: []
})

export class MonacoEditor extends MonacoEditorComponent implements AfterViewInit, OnDestroy {

  @HostBinding('style.width') get styleWidth() { return this.getWidth(); }
  @HostBinding('style.height') get styleHeight() {
      if (this.isAutoHeight()) return 'auto';
      return this.getHeight();
  }

  protected editor: monacoTypes.editor.ICodeEditor;
  protected cacheCheck : string = "";
  protected highlightingCache;
  private resizeObserver: ResizeObserver;
  private contentSizeListener: monacoTypes.IDisposable;
  private decorationsCollection: monacoTypes.editor.IEditorDecorationsCollection | null = null;
  private darkModeObserver: MutationObserver;

  /**
   * Adapt the component here if you want to add any options
   */
  public _options : any = {
    value: this._entry,
    language: this._language,
    theme: this.getEffectiveTheme(),
    readOnly: this._disabled,
    glyphMargin: this._glyphMargin,
    automaticLayout: false,
    hover: {
      enabled: true,
      sticky: true,
      above: false
    },
    minimap: {
      enabled: this._showMinimap
    }
  };

  public constructor(private elementRef: ElementRef, private ngZone: NgZone) {
    setTimeout(() => {
      monaco.languages.registerHoverProvider("*", {
        provideHover: async (model, position, token) => {
          const hoverContents = this.getHoverContents();
          if (!hoverContents) {
            return null;
          }
          for (const hC of hoverContents) {
            const range = hC.getRange();
            const isInRange =
              (position.lineNumber > range.getStartLin() ||
                (position.lineNumber === range.getStartLin() &&
                  position.column >= range.getStartCol())) &&
              (position.lineNumber < range.getEndLin() ||
                (position.lineNumber === range.getEndLin() &&
                  position.column <= range.getEndCol()));
            if (!isInRange) {
              continue;
            }
            const contents: { value: string }[] = [];
            for await (const content of hC.getHoverContentList()) {
              if (token.isCancellationRequested) {
                return null;
              }
              contents.push({ value: content });
            }
            return { contents };
          }
          return null;
        }
      });
    }, 1000);

    super();
  }

  ngAfterViewInit() {
    this.resizeObserver = new ResizeObserver(() => {
      if (this.editor && !this.isAutoHeight()) {
        this.editor.layout();
      }
    });
    this.resizeObserver.observe(this.elementRef.nativeElement);

    // follow the dark mode of the app, the theme is global for all monaco editors
    this.darkModeObserver = new MutationObserver(() => {
      if (typeof monaco !== 'undefined') {
        monaco.editor.setTheme(this.getEffectiveTheme());
      }
    });
    this.darkModeObserver.observe(document.body, { attributes: true, attributeFilter: ['class'] });
  }

  /** The theme of this editor, or its dark counterpart while the dark mode of the app is active */
  public getEffectiveTheme(): string {
    if (isDarkMode() && DARK_THEME_OF[this._theme] != null) {
      return DARK_THEME_OF[this._theme];
    }
    return this._theme;
  }

  ngOnDestroy() {
    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
    }
    this.darkModeObserver?.disconnect();
    if (this.contentSizeListener) {
        this.contentSizeListener.dispose();
    }
    super.ngOnDestroy();
  }

  @Output() public entryChange = new EventEmitter<string>();

  onEntryChange(event: string) {
    this.entryChange.emit(event);
    if(this.editor) {
      this.highlightInitialization(this.editor)
      this.scrollToLine(this.editor)
    }
  }

  @Output() public override lineClick = new EventEmitter<LineClickEvent>();

  public saveEditor(editorInstance: monacoTypes.editor.IStandaloneCodeEditor) : void {

    if(this.editor != editorInstance){
      // new instance, force redraw on new instance
      this.decorationsCollection = null;
      this.highlightingCache = "";
    }
    this.editor = editorInstance;
    if(this.getHighlighting()) {
      this.highlightInitialization(editorInstance)
      this.scrollToLine(this.editor)
    }

    if (this.isAutoHeight()) {
        this.updateHeight();
        this.contentSizeListener = this.editor.onDidContentSizeChange(() => {
            this.updateHeight();
        });
    }

    this.editor.onMouseDown((e: monacoTypes.editor.IEditorMouseEvent)=>{
      if(e.target && e.target.position){
        if(this.lineClick.observed) {
          const clickEventBuilder = new LineClickEventBuilder();
          clickEventBuilder.lineNumber(e.target.position.lineNumber);
          clickEventBuilder.column(e.target.position.column);
          const clickEvent = clickEventBuilder.build();
          // Force event execution in Angular Zone for proper change detection of any changes triggered in a user
          // callback
          this.ngZone.run(() => this.lineClick.emit(clickEvent));
        }
      }
    });

    // Initial layout
    setTimeout(() => this.editor.layout(), 0);
  }

  private updateHeight() {
      if (!this.editor) return;
      const contentHeight = this.editor.getContentHeight();
      const lineHeight = this.editor.getOption(monaco.editor.EditorOption.lineHeight);
      const maxLines = this.getMaxLines() || 30; // Default 30 if not set
      const maxHeight = maxLines * lineHeight;

      const height = Math.min(contentHeight, maxHeight);

      const width = this.elementRef.nativeElement.clientWidth;
      this.elementRef.nativeElement.style.height = `${height}px`;
      this.editor.layout({ width: width, height: height });
  }

  public highlightInitialization(editor: monacoTypes.editor.ICodeEditor): void {
    if(!this.getHighlighting()) {
      return;
    }
    const highlighting = this.getHighlighting().map(inlineHighlight => {
      return {
        range: {
          startLineNumber: inlineHighlight.getRange().getStartLin(),
          startColumn: inlineHighlight.getRange().getStartCol(),
          endLineNumber: inlineHighlight.getRange().getEndLin(),
          endColumn: inlineHighlight.getRange().getEndCol(),
        },
        options: {
          inlineClassName: inlineHighlight.getCssClass()
        }
      };
    });

    const cache = JSON.stringify(highlighting);

    if(cache === this.highlightingCache) {
      return;
    }
    this.highlightingCache = cache;

    if (!this.decorationsCollection) {
      // First set of decorations on this editor instance
      this.decorationsCollection = editor.createDecorationsCollection(highlighting);
    } else {
      // Update existing collection without stacking on previous
      this.decorationsCollection.set(highlighting);
    }


  }

  public scrollToLine(editor: monacoTypes.editor.ICodeEditor): void {
    if(!this.getInitialScrollToLine()) {
      return;
    }
    editor.revealLineNearTop(this.getInitialScrollToLine())
  }

  public override setLanguage(val: string) {
    super.setLanguage(val);
    this.updateOptions();
  }

  public override setTheme(val: string) {
    super.setTheme(val);
    this.updateOptions();
  }

  public override setDisabled(val: boolean) {
    super.setDisabled(val);
    this.updateOptions();
  }

  public override setHoverContents(val: HoverContent[]) {
    super.setHoverContents(val)
    if(!val) {
      return;
    }
  }

  public override setHighlighting(val: InlineDecoration[]) {
    super.setHighlighting(val);
    if(this.editor) {
      this.highlightInitialization(this.editor);
    }
  }

  public override setAutoHeight(val: boolean) {
      super.setAutoHeight(val);
      this.updateOptions();
      if (val && this.editor && !this.contentSizeListener) {
          this.updateHeight();
          this.contentSizeListener = this.editor.onDidContentSizeChange(() => {
            this.updateHeight();
        });
      } else if (!val && this.contentSizeListener) {
          this.contentSizeListener.dispose();
          this.contentSizeListener = undefined;
      }
  }

  public override setJumpToLine(val: number) {
    super.setJumpToLine(val);
    if (this.editor && val > 0) {
      this.editor.revealLineInCenter(val);
      this.editor.setPosition({ lineNumber: val, column: 1 });
    }
  }

  public override setShowMinimap(val: boolean) {
    super.setShowMinimap(val);
    this.updateOptions();
  }

  public updateOptions() : void {
    this._options = {
      value: this._entry,
      language: this._language,
      theme: this.getEffectiveTheme(),
      readOnly: this._disabled,
      glyphMargin: this._glyphMargin,
      automaticLayout: false,
      scrollBeyondLastLine: !this.isAutoHeight(),
      hover: {
        enabled: true,
        sticky: true,
        above: false
      },
      minimap: {
        enabled: this._showMinimap
      }
    };
  }
}
