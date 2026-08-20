import { Component,ChangeDetectionStrategy } from '@angular/core';
import { LoadingTestsManagerClientImpl } from 'loadingtests/LoadingTestsManagerClientImpl';
import { ClientCommandManager, ClientCommandWebsocketCommunication, CommandManager } from '@umlp/commonj2ts';
import { LoadingTestsComponentObserverManager } from 'LoadingTestsComponentObserverManager'
import { LoadingTestsManager } from 'loadingtests/LoadingTestsManager';
import { RouterOutlet } from '@angular/router';

declare global {
  interface Window {
    LoadingTestsManager: typeof import('loadingtests/LoadingTestsManager').LoadingTestsManager
  }
}

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  selector: 'app-root',
  templateUrl: '../gui2java2ts/AppComponent.html',
  styleUrls: ['../gui2java2ts/AppComponent.scss'],
  imports: [
    RouterOutlet
  ]
})
export class AppComponent {
  constructor() {
    LoadingTestsManagerClientImpl.init()
    const ws_protocol = document.location.protocol == "https:" ? "wss" : "ws";
    const cmdManager = new ClientCommandManager(new ClientCommandWebsocketCommunication(
        ws_protocol + '://' + document.location.host + '/umlp/api/command'
    ));
    CommandManager.initMe(cmdManager);
    (<any>window).LoadingTestsManager = LoadingTestsManager;
    (<any>window).websocket_connected = true;
    LoadingTestsComponentObserverManager.init();
  }
}
