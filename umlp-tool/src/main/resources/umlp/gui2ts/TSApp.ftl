/* (c) https://github.com/MontiCore/monticore */

${tc.signature("cdName", "htmlPath", "scssPaths")}

import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ClientCommandManager, ClientCommandWebsocketCommunication, CommandManager, BaseClassObjectMapper } from '@umlp/commonj2ts';
import {config} from 'AppGemConfig'

import { ${cdName}ComponentObserverManager } from '${cdName}ComponentObserverManager'
import { ${cdName}ManagerClientImpl } from '${cdName?lower_case}/${cdName}ManagerClientImpl';

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  selector: 'app-root',
  templateUrl: '${htmlPath}',
  styleUrls: [<#list scssPaths as scssPath>'${scssPath}'<sep>, </#list>],
  imports: [
    RouterOutlet,
    ...config.imports
  ]
})
export class AppComponent {
  constructor() {
    ${cdName}ManagerClientImpl.init();
    const ws_protocol = document.location.protocol == "https:" ? "wss" : "ws";
    const cmdManager = new ClientCommandManager(new ClientCommandWebsocketCommunication(
      ws_protocol + '://' + document.location.host + '/umlp/api/command'
    ));
    CommandManager.initMe(cmdManager);
    (<any>window).websocket_connected = true;
    ${cdName}ComponentObserverManager.init();
  }
}
