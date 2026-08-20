/* (c) https://github.com/MontiCore/monticore */

import {config, WizardComponent} from "wizard/WizardComponent";
import {Component, ViewChild, ViewContainerRef, AfterViewInit} from "@angular/core";
import {KeyValuePipe, NgComponentOutlet} from "@angular/common";

import {GET_WIZARD_DATA_BY_CLASS, WIZARD_DATA} from "wizard/WizardData";
import {WizardContainer} from "wizard/WizardContainer";
import {InspectorService} from "wizard/InspectorService";
import {WizardModal} from "wizard/WizardModal";

/**
 * A simple GUI-Wizard  allowing WYSIWYG GUI Design.
 * Available components are described in the WizardData
 *
 * Export to GuiDSL is extreeeemly whacky and importing is NYI.
 * Both should be easily doable when JSweet allows us the use of the GuiDSL AST
 */
@Component({
  standalone: true,
  imports: [...config.imports, KeyValuePipe, WizardModal, WizardContainer],
  selector: config.selector,
  template: `

    <wizard-container #wizardContainer
    humanReadableName="root page"
    [containerInputs]="rootContainerInputs"
    />

    <hr/>

    <div style="position: sticky; bottom: 0; left: 50px; height: 20px; wxidth: 200px; z-index: 999; display: flex">
        <button (click)="toggleOrder()">Toggle Controls</button>
        <button (click)="export()">Export</button>
        <button (click)="store()">save as default</button>
        <button (click)="restore()">restore default</button>
    </div>


    <wizard-modal [open]="this.currentEditingInfo && this.showControls">
        <small>Click a line to modify a value</small>
        <ng-container *ngFor="let prop of this.currentEditingInfo | keyvalue">
          <span (click)="changeProp(prop.key)">{{prop.key}} : "{{prop.value.value}}" ({{prop.value.format}}) </span> <br/>
        </ng-container>
    </wizard-modal>

    <wizard-modal [open]="true">
        <h5>New Element: <small>Drag & drop a component from here onto the page</small></h5>
        <button *ngFor="let k of wizard_keys"   (dragstart)="addElemDragStart(k, $event)" draggable="true" title="Drag me">{{k}}</button>

    </wizard-modal>
  `,
  styles: config.styles,
  styleUrls: config.styleUrls
})
export class Wizard extends WizardComponent implements AfterViewInit {

  wizard_keys = Object.keys(WIZARD_DATA);

  constructor(private viewRef: ViewContainerRef, private inspectorService: InspectorService) {
    super()

    let inspect = this.inspect.bind(this);
    inspectorService.inspectObservable().subscribe({
      next(x) {
        inspect(x);
      }
    });
    let that = this;
    inspectorService.newElementRequesterObservable().subscribe({
      next(cb) {
        that.newElemCallback = cb;
      }
    });
  }

  // root container, which is then printed
  @ViewChild('wizardContainer', {static: true}) wizardContainer;

  rootContainerInputs: any = {};

  showControls = true;

  newElemCallback: ((clazz: any, inputs: any) => void) | undefined = undefined;


  currentEditing: any | undefined = undefined;
  currentEditingInfo: { [key: string]: { value: any, format: string } } | undefined = undefined;


  toggleOrder(): void {
    this.showControls = !this.showControls;
    this.inspectorService.showControls.next(this.showControls);
  }

  export(): void {
    prompt("Result", this.makeString(this.wizardContainer));
    console.log("res", this.makeObj(this.wizardContainer));
    console.log(this.wizardContainer)
  }

  store() {
    window.localStorage.setItem("gui_wizard", JSON.stringify(this.makeObj(this.wizardContainer)))
  }

  ngAfterViewInit(): void {
    this.restore()
  }

  addElemDragStart(key: string, $event: DragEvent) {
    const dragEnd = (e) => {
      $event.target.removeEventListener('dragend', dragEnd);
      this.inspectorService.showDropZone.next(false);
    }
    $event.target.addEventListener('dragend', dragEnd);
    $event.dataTransfer.dropEffect = 'copy';

    setTimeout(() =>     this.inspectorService.showDropZone.next(true), 100);
    $event.dataTransfer.effectAllowed = "copy";
    this.inspectorService.draggedObject = {
      newType: key
    }

  }

  restore() {

    let d = {
      "components": [
        {
          "type": "GemText",
          "params": {
            "value": "Page Title of the GUI wizard"
          }
        },
        {
          "type": "GemCard",
          "params": {
            "title": "Test"
          },
          "components": {
            "titleComponent": [
              {
                "type": "GemText",
                "params": {
                  "value": "This is the title of a card."
                }
              }
            ],
            "component": [
              {
                "type": "GemColumn",
                "params": {},
                "components": {
                  "components": [
                    {
                      "type": "GemRow",
                      "params": { },
                      "components": {
                        components: [{
                          "type": "GemText",
                          "params": {
                            "value": "By pressing toggle controls, you can hide/show the control buttons"
                          }
                        }]
                      }
                    },
                    {
                      "type": "GemRow",
                      "params": { },
                      "components": {
                        components: [{
                          "type": "GemText",
                          "params": {
                            "value": "You can inspect, delete, and move elements using the controls. Hover over the buttons for additional info"
                          }
                        }]
                      }
                    },
                  ]
                }
              }
            ]
          }
        }
      ]
    }
    if (localStorage.getItem('gui_wizard'))
      d = JSON.parse(localStorage.getItem('gui_wizard'))
    for (let component of d.components) {
      this.wizardContainer.addInner('components', component);

      this.wizardContainer.updateComps();
    }

  }


  // Whacky to GuiDSL - with jsweet this can be done properly :)
  makeString(e, knowntype?: any) {
    let ret = '';
    if (e instanceof WizardContainer) {
      if (!e.containerType) {
        ret += "page MyPage( ) {\n";
        let sorted = this.sortTemplateContains(e.templateContainers);
        for (let cd of sorted['components']) {
          ret += this.makeStringTC(e, cd);
        }
        ret += "}\n";
        return ret;
      }
    }

    if (e.templateContainers === undefined) {
      console.trace("Missing", e)
      return "Missing templateContainers?";
    }

    ret += "@" + GET_WIZARD_DATA_BY_CLASS(e.containerType).componentName + " {\n";
    let first = true;
    // Start with simple literal-values
    for (let k in e.containerInputs) {
      if (k in e.innerNames) continue;
      if (!first) ret += ",\n ";
      first = false;
      ret += k + " = " + e.containerInputs[k];
    }
    // followed by inner components
    let sorted = this.sortTemplateContains(e.templateContainers);
    for (let key in sorted) {
      if (!first) ret += ",\n ";
      first = false;
      ret += key + " = ";
      if (sorted[key].length == 1)
        ret += this.makeStringTC(e, sorted[key][0]);
      else {
        ret += "[";
        for (let entryIndexs in (sorted[key])) {
          let entryIndex = entryIndexs as unknown as number;
          if (entryIndex > 0)
            ret += ",\n ";
          ret += this.makeStringTC(e, sorted[key][entryIndex]);
        }
        ret += "]";
      }
    }
    ret += "}\n";
    return ret;
  }

  makeObj(e, knowntype?: any): {} {
    if (e instanceof WizardContainer) {
      if (!e.containerType) {
        let ret = {components: []};
        let sorted = this.sortTemplateContains(e.templateContainers);
        for (let cd of sorted['components']) {
          ret.components.push(this.makeObjTC(e, cd));
        }
        return ret;
      }
    }

    if (e.templateContainers === undefined) {
      console.trace("Missing", e)
      return "Missing templateContainers?";
    }

    let ret = {
      type: GET_WIZARD_DATA_BY_CLASS(e.containerType).componentName,
      params: {},
      components: {}
    };
    // Start with simple literal-values
    for (let k in e.containerInputs) {
      console.log("inputs", k)
      if (k in e.innerNames) continue;
      ret.params[k] = e.containerInputs[k];
    }
    // followed by inner components
    let sorted = this.sortTemplateContains(e.templateContainers);
    for (let key in sorted) {
      ret.components[key] = [];
      for (let entryIndexs in (sorted[key])) {
        let entryIndex = entryIndexs as unknown as number;
        ret.components[key].push(this.makeObjTC(e, sorted[key][entryIndex]))
      }
    }
    return ret;
  }

  sortTemplateContains(d: { index: number, listName: string, type: any, inputs: any }[]): {
    [listName: string]: { index: number, listName: string, type: any, inputs: any }[]
  } {
    let ret = {};
    for (let e of d) {
      if (!(e.listName in ret))
        ret[e.listName] = [];
      ret[e.listName][e.index] = e;
    }
    return ret;
  }


  makeStringTC(outer: WizardContainer, e: {
    index: number,
    listName: string,
    type: any,
    inputs: any
  }) {
    if (e.type == WizardContainer) {
      // Next, convert the object of data to the Real thingy
      let index = 0, foundIndex = -1;
      for (let entry of outer.templateContainers) {
        if (entry === e) {
          foundIndex = index;
          break;
        }
        index++;
      }
      let diff = outer.ngComponentOutlet.length - outer.templateContainers.length;
      const t = e.inputs.containerType;
      // and try again
      return this.makeString(outer.ngComponentOutlet.get(foundIndex + diff)['_componentRef']['instance'], t);
    }
    let ret = '@' + GET_WIZARD_DATA_BY_CLASS(e.type).componentName;

    ret += "(";

    let first = true;
    for (let iKey in e.inputs) {
      if (!first)
        ret += ",\n";
      ret += " " + iKey;
      ret += " = \"" + e.inputs[iKey] + "\" ";
      first = false;
    }

    ret += ")";

    return ret;
  }

  makeObjTC(outer: WizardContainer, e: {
    index: number,
    listName: string,
    type: any,
    inputs: any
  }) {
    if (e.type == WizardContainer) {
      // Next, convert the object of data to the Real thingy
      let index = 0, foundIndex = -1;
      for (let entry of outer.templateContainers) {
        if (entry === e) {
          foundIndex = index;
          break;
        }
        index++;
      }
      let diff = outer.ngComponentOutlet.length - outer.templateContainers.length;
      const t = e.inputs.containerType;
      // and try again
      return this.makeObj(outer.ngComponentOutlet.get(foundIndex + diff)['_componentRef']['instance'], t);
    }
    let ret = {type: GET_WIZARD_DATA_BY_CLASS(e.type).componentName, params: {}};
    for (let iKey in e.inputs) {
      ret.params[iKey] = e.inputs[iKey];
    }
    return ret;
  }


  // Change a property in inspect view
  changeProp(prop: string): void {
    let current = this.currentEditing.inputs[prop];
    let r = prompt("What value do you want to change to?", current);
    if (r === null) return
    this.currentEditing.inputs[prop] = r;
  }


  // Handle inspect request
  inspect(e: { type: any, inputs: any }) {
    this.currentEditing = e;
    let data = GET_WIZARD_DATA_BY_CLASS(e.type);
    if (data) {
      let doShow = {}
      for (let prop in data.inspect) {
        let i = data.inspect[prop];
        doShow[prop] = {value: e.inputs[prop], format: i.format};
      }
      this.currentEditingInfo = doShow;
      return;
    }
    this.currentEditingInfo = {
      'ERROR': {
        value: 'ERROR',
        format: "ERROR - unknown component " + e.type.name
      }
    }
  }

  addElemClb(choice: string) {
    if (choice === undefined) {
      this.newElemCallback(undefined, {}); // abort
      this.newElemCallback = undefined;
      return;
    }
    if (choice in WIZARD_DATA) {
      this.newElemCallback(...WIZARD_DATA[choice].new);
    }
    this.newElemCallback = undefined;
  }
}
