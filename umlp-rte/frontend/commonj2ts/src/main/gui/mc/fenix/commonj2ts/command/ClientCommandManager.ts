/* (c) https://github.com/MontiCore/monticore */

import {CommandManager} from "./CommandManager";
import {ClientCommandCommunication} from "./ClientCommandCommunication";
import {Command} from "./Command";
import {CommandResult} from "./CommandResult";
import {PendingCommands} from "./PendingCommands";

export class ClientCommandManager extends CommandManager {

  protected commandCommunication: ClientCommandCommunication;
  private initSuccessFul: boolean = false;

  public constructor(commandCommunication: ClientCommandCommunication) {
    super();
    // makes the count available to end-to-end tests before the first command is sent
    PendingCommands.publish();
    this.commandCommunication = commandCommunication.onCommandReceived(cmd => cmd.doRun());
    this.initCommunication();
  }

  public initCommunication(): Promise<void> {
    let promiseResolve: (value: unknown) => void, promiseReject: (reason?: any) => void;

    const retPromise: Promise<void> = new Promise(function (resolve, reject) {
      promiseResolve = resolve;
      promiseReject = reject;
    });

    this.commandCommunication.init().then(res => {
      let initResult: number = res;
      if (!Number.isNaN(initResult)) {
        this.participantId = initResult;
        this.initSuccessFul = true;
        promiseResolve(this.participantId);
      } else {
        promiseReject();
        // TODO MH
      }
    })
    return retPromise;
  }

  public retryInitCommunicationImmediate() {
    this.commandCommunication.retryImmediate();
  }

  public pauseRetryCommunication() {
    this.commandCommunication.pauseRetry();
  }

  public isInitSuccessFul(): boolean {
    return this.initSuccessFul;
  }

  protected override async _executeCommand<T>(command: Command<T>): Promise<CommandResult<T>> {
    command.setParticipantId(this.participantId);
    return await this.commandCommunication.sendCommandToServerAndReceiveResult(command);
  }
}
