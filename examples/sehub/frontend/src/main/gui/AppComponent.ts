/* (c) https://github.com/MontiCore/monticore */
import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ClientCommandManager, ClientCommandWebsocketCommunication, CommandManager, BaseClassObjectMapper } from '@umlp/commonj2ts';
import {config} from 'AppGemConfig'

import { SehubComponentObserverManager } from 'SehubComponentObserverManager'
import { SehubManagerClientImpl } from 'sehub/SehubManagerClientImpl';

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  selector: 'app-root',
  templateUrl: '../hwc/AppComponent.html',
  styleUrls: ['../hwc/AppComponent.scss'],
  imports: [
    RouterOutlet,
    ...config.imports
  ]
})
export class AppComponent {
  constructor() {
    SehubManagerClientImpl.init();
    const ws_protocol = document.location.protocol == "https:" ? "wss" : "ws";
    const cmdManager = new ClientCommandManager(new ClientCommandWebsocketCommunication(
      ws_protocol + '://' + document.location.host + '/umlp/api/command'
    ));
    CommandManager.initMe(cmdManager);
    (<any>window).websocket_connected = true;
    SehubComponentObserverManager.init();
  }
}
