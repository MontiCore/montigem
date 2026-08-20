/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component, TemplateRef, ViewChild, ViewContainerRef} from '@angular/core';
import { config, GemTreeNodeComponent } from '../../../../../../build/gui2java2ts/mc/fenix/arrange/GemTreeNodeComponent';
import { MatIcon } from '@angular/material/icon';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, MatIcon],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemTreeNode extends GemTreeNodeComponent {
  @ViewChild('treeNodeTemplate', { static: true })
  treeNodeTemplate: TemplateRef<any>;

  override _isExpanded: boolean = false;

  constructor(private viewContainerRef: ViewContainerRef) {
    super();
  }

  ngOnInit() {
    // Remove component's enclosing html tag
    this.viewContainerRef.createEmbeddedView(this.treeNodeTemplate);
    this.viewContainerRef.element.nativeElement.remove();
  }

  leftClick() {
    this.onClick.emit()
  }

  expand() {
    if (this.children) {
      this.isExpanded = !this.isExpanded;
      this.onToggle.emit(this.isExpanded);
    }
  }

}
