/* (c) https://github.com/MontiCore/monticore */

import {GemNetworkData, GemNetworkEdge, GemNetworkNode} from '@umlp/charts';
import {ProjectPageComponent,config} from 'gui/ProjectPageComponent';
import {Artifact} from 'sehub/Artifact';
import {Directory} from 'sehub/Directory';
import { SehubSecurityManager } from "sehub/SehubSecurityManager";
import {Component,ChangeDetectionStrategy} from '@angular/core';
import {AsyncList} from '@umlp/commonj2ts';

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports:[...config.imports],
  selector: config.selector,
  templateUrl: config.templateUrl,
  styles: config.styles,
  styleUrls: config.styleUrls
})
export class ProjectPage extends ProjectPageComponent {

  networkDataCheck = { value: null, hash: null, calculating: false };
  treeViewDataCheck = { value: null, hash: null, calculating: false };

  ngOnInit(){
    if (!SehubSecurityManager.isSignedIn()) {
      this._router.navigate(['gui/login/LoginPage']);
    }
    super.ngOnInit();
  }

  public getGemNetworkGraph1Data(): GemNetworkData {
    if (!(this.project)) {
      return null;
    }
    const networkData = new GemNetworkData();

    const artifacts: Artifact[] = this.getAllArtifactsFromDir(this.project, []);

    const nodes : AsyncList<GemNetworkNode> = new AsyncList();
    nodes._data = this.getNodesFromArtifacts(artifacts);
    networkData.setNodesList(nodes);
    const edges : AsyncList<GemNetworkEdge> = new AsyncList();
    edges._data = this.getEdgesFromArtifacts(artifacts);
    networkData.setEdgesList(edges);
    const newHash = JSON.stringify(networkData);
    if (this.networkDataCheck.hash !== newHash) {
      this.networkDataCheck.hash = newHash;
      this.networkDataCheck.value = networkData;
    }
    return this.networkDataCheck.value;
  }


  private getEdgesFromArtifacts(artifacts: Artifact[]): GemNetworkEdge[] {
    let edges: GemNetworkEdge[] = [];
    artifacts.filter(a => a instanceof Directory).forEach((dir: Directory) => {
      dir.getContainsList().forEach(a => edges.push(this.createEdge(dir.getFullName(), a.getFullName())))
    })
    return edges;
  }

  private createEdge(start: string, end: string): GemNetworkEdge {
    const edge = new GemNetworkEdge();
    edge.setFrom(start);
    edge.setTo(end);
    edge.setLength(1.0);
    return edge;
  }

  private getNodesFromArtifacts(artifacts: Artifact[]): GemNetworkNode[] {
    return artifacts.map(a => this.getNodeFromArtifact(a));
  }

  private getAllArtifactsFromDir(art: Artifact, artifacts: Artifact[]): Artifact[] {
    if (art instanceof Directory) {
      (art as Directory).getContainsList().forEach(a => this.getAllArtifactsFromDir(a, artifacts))
    }
    artifacts.push(art)
    return artifacts;
  }

  private getNodeFromArtifact(art: Artifact): GemNetworkNode {
    let node: GemNetworkNode = new GemNetworkNode();
    node.setColor(art.getFullName());
    node.setColor((art instanceof Directory) ? "#FFFF00" : "#FF00BF");
    node.setMass(1.0);

    return node;
  }

}


