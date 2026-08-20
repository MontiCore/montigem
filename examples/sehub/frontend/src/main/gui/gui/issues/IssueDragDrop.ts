/* (c) https://github.com/MontiCore/monticore */

import {CdkDragDrop, moveItemInArray, transferArrayItem} from '@angular/cdk/drag-drop';
import {Component, EventEmitter, Output,ChangeDetectionStrategy} from '@angular/core';
import {IssueDragDropComponent,config} from 'gui/issues/IssueDragDropComponent';
import {Issue} from 'sehub/Issue';
import { DragDropModule } from "@angular/cdk/drag-drop";
import { IssueEntry } from "gui/issues/IssueEntry";
import { IUMLPList,AsyncList,GemIdentifiable,GemListUtil } from "@umlp/commonj2ts";
@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports:[...config.imports,DragDropModule,IssueEntry],
  selector: config.selector,
  templateUrl: "IssueDragDrop.html",
  styles: config.styles,
  styleUrls: config.styleUrls
})
export class IssueDragDrop extends IssueDragDropComponent {

  // TODO: fix generator error
  @Output()
  public override drop: EventEmitter<any> = new EventEmitter<any>();

  onDrop(event: CdkDragDrop<IUMLPList<Issue>>) {
    this.drop.emit(event?.previousContainer?.data?.[event.previousIndex]);
    if (event.previousContainer === event.container) {
      GemListUtil.toArraySync(event.container.data).then(data =>
          moveItemInArray(data, event.previousIndex, event.currentIndex)
      )

    } else {
      GemListUtil.toArraySync(event.container.data).then(data =>
          transferArrayItem(
              data,
              data,
              event.previousIndex,
              event.currentIndex,
          )
      )
    }
  }

  protected dropTopWrapper : Array<string> = []

  public override setCanDropTo(list : AsyncList<string>) {
    GemListUtil.toArraySync(list).then(l => this.dropTopWrapper = l)
  }

  private getIt2IterableForAngular__CachedResult: Array<Issue> = undefined;
  public getIt2IterableForAngular__ChangeDetection(): Array<Issue> {
    if (!this.allVarsInited) {
      return undefined;
    }
    if (this.getIt2IterableForAngular__CachedResult === undefined) {
      this.getIt2IterableForAngular__CachedResult = null;
      this.getIt2IterableForAngular__update();
    }
    return this.getIt2IterableForAngular__CachedResult;
  }

  public getIt2IterableForAngular__update(): void {

    Promise.resolve(this.getIt2IterableForAngular(
        this.createDomainObserverRegisterFunction('getIt2IterableForAngular__update' + this.computeHash([]), () => this.getIt2IterableForAngular__update()),
        this.createStateVarObserverRegisterFunction('getIt2IterableForAngular__update' + this.computeHash([]), () => this.getIt2IterableForAngular__update())
    )).then(__v => this.getIt2IterableForAngular__CachedResult = __v === undefined ? null : __v);
  }

  /*private*/ async getIt2IterableForAngular(
      __regObsToDomainObj: <T extends GemIdentifiable> (__domainObj: T) => T = __domainObj => __domainObj,
      __regObsToStateVar: <T> (__stateVar: T, varName: string) => T = (__stateVar, _) => __stateVar): Promise<Array<Issue>> {
    return GemListUtil.toArraySync<any>((await this.getIssues(__regObsToDomainObj, __regObsToStateVar)));
  }

}
