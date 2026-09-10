import { Component } from '@angular/core';
import { MontiArcVizExperimentManagerClientImpl } from 'montiarcvizexperiment/MontiArcVizExperimentManagerClientImpl';
import { ClientCommandManager, ClientCommandWebsocketCommunication, CommandManager } from '@umlp/commonj2ts';
import { MontiArcVizExperimentComponentObserverManager } from 'MontiArcVizExperimentComponentObserverManager';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  constructor() {
    MontiArcVizExperimentManagerClientImpl.init();
    const ws_protocol = document.location.protocol == "https:" ? "wss" : "ws";
    const cmdManager = new ClientCommandManager(new ClientCommandWebsocketCommunication(
      ws_protocol + '://' + document.location.host + '/umlp/api/command'
    ));
    CommandManager.initMe(cmdManager);
    (<any>window).websocket_connected = true;
    MontiArcVizExperimentComponentObserverManager.init();
  }
}
