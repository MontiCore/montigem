/* (c) https://github.com/MontiCore/monticore */

import {ClientCommandCommunication} from "../../command/ClientCommandCommunication";
import {Command} from "../../command/Command";
import {CommandObjectMapper} from "../../command/CommandObjectMapper";
import {CommandReceiveObserver} from "../../command/CommandReceiveObserver";
import {CommandResult} from "../../command/CommandResult";
import {CommandResultReceiveObserver} from "../../command/CommandResultReceiveObserver";
import {SimpleResult} from "../../command/SimpleResult";
import {LoadingState} from "../../common/LoadingState";
import {webSocket, WebSocketSubject} from 'rxjs/webSocket';
import {
  first,
  firstValueFrom,
  from,
  retry,
  Subject,
  take,
  timer,
  BehaviorSubject,
  tap,
  filter,
  bufferWhen,
  catchError,
  ObservableInput, merge, of
} from "rxjs";
import {Log} from "../../logging";
import {PendingCommands} from "../../command/PendingCommands";

export class ClientCommandWebsocketCommunication implements ClientCommandCommunication {
  public static MAX_TRIES: number = 10;
  private maxWaitingTime: number = 6000; // 6 seconds
  private participantId: number = -1;
  // TODO AGe: any
  private session: WebSocketSubject<any>;
  // TODO AGe: URI
  private uri: string = 'ws://localhost:8000';

  // not necessary?
  // private headers: WebSocketHttpHeaders;

  private messages: Map<number, Subject<CommandResult<any>>> = new Map();

  private commandObserver: Array<CommandReceiveObserver> = [];
  private commandObserverChain: Promise<any> = Promise.resolve();
  private commandResultObserver: Array<CommandResultReceiveObserver> = [];

  private socket: WebSocketSubject<any>;
  private outgoingMessages = new Subject<any>();
  private connectionStatus = new BehaviorSubject<boolean>(false);
  private established: boolean = false;

  private immediateRetry: Subject<any> = new Subject();
  private retryCount: number = 0;

  /*
  executor: ExecutorService;
  private initLatch: number;
  /**/

  public constructor(uri: string) {
    this.uri = uri;
  }

  private static warnCouldNotReceive<T>(command: Command<T>): void {
    Log.warn("Could not receive response from server for command id: " + command.getId());
  }

  public async sendCommandToServerAndReceiveResult<T>(command: Command<T>): Promise<CommandResult<T>> {
    command.setParticipantId(this.participantId);
    // let result: CommandResult<T>;
    // try {
    let payload: string = await CommandObjectMapper.serializeCommand(command);
    Log.info(`Sending message to server: ${payload}`, this.constructor.name);
    let sendingSuccessful: boolean = this.sendMessage(payload);
    if (!sendingSuccessful) {
      ClientCommandWebsocketCommunication.warnCouldNotReceive(command);
      return Promise.resolve(new SimpleResult<T>().createFromLoadingState(command.getId(), LoadingState.FAILED_TO_LOAD, command.getCommandType()));
    }
    const resultSubject = new Subject<CommandResult<T>>();
    this.messages.set(command.getId(), resultSubject);
    const result = new Promise<CommandResult<T>>((resolve, reject) => {
      from(resultSubject).pipe(first(), take(1)).subscribe(v => {
        resolve(v);
        /*
        resolve(new SimpleResult<T>().SimpleResult1(
            command.getId(), v, command.getCommandType()
        ));
        /**/
      });
      // TODO AGe when reject?
    });
    result.then(() => this.messages.delete(command.getId()));

    return result;

    /*

    if (this.messages.has(command.getId())) {
      let response: Promise<CommandResult<any>> = this.messages.get(command.getId());
      response
      .then(value => {
        this.messages.delete(command.getId());
        result = <CommandResult<T>>value;
        console.info(`
          Received answer from server for command with id
          ${command.getId()}: ${CommandObjectMapper.serializeResponse(result)}
        `, this.constructor.name);
      })
      .catch(() => {
        ClientCommandWebsocketCommunication.warnCouldNotReceive(command);
        return new SimpleResult<T>().SimpleResult2(
          command.getId(), LoadingState.FAILED_TO_LOAD, command.getCommandType()
        );
      });
    }
  }
  catch (e: any) {
    ClientCommandWebsocketCommunication.warnCouldNotReceive(command);
    // e.printStackTrace();
    return new SimpleResult<T>().SimpleResult2(
      command.getId(), LoadingState.FAILED_TO_LOAD, command.getCommandType()
    );
  }
  return result;
  /**/
  }

  public init(): Promise<number> {
    this.onCommandResultReceived(
      (result : CommandResult<any>) => {
        result.isSuccessful().then(
          success => {
            if(!success) {
              Log.debug("Command with id "+result.getCommandId()+ " was not successful.", "ClientCommandWebsocketCommunication");
            }
          }
        )
      }
    );

    this.socket = webSocket({
      url: this.uri,
      openObserver: {
        next: () => this.connectionStatus.next(true)
      },
      closeObserver: {
        next: () => this.connectionStatus.next(false)
      }
    });
    const retrySocket = this.socket.pipe(
      retry({ // retry, but with back off of 2^n * backoffInterval (and at most 10s)
        delay: (_, retryCount:number) => {
          if (this.retryCount == -2) {
            // disabled
            return this.immediateRetry;
          }
          this.retryCount++;
          if (retryCount < this.retryCount)
            this.retryCount = retryCount;
          const backoffInterval = 400;
          return merge(this.immediateRetry, timer(Math.min(10*1000, Math.pow(2, this.retryCount - 1) * backoffInterval)));
        },
        resetOnSuccess: true,
      })
    );

    // Outgoing message queue
    // Buffer messages when WebSocket is not connected, flushed when it reconnects
    const bufferedMessages$ = this.outgoingMessages.pipe(
      filter( () => !this.connectionStatus.value), // only if not connected
      bufferWhen(() => this.connectionStatus.pipe(filter(isOpen => isOpen))) // flush them when it reconnects
    );

    // Subscription to send messages through the directly WebSocket only if it's open
    this.outgoingMessages.pipe(
      filter(() => this.connectionStatus.value), // Only if open
      tap(message => {
        this.socket.next(message);
      })
    ).subscribe();
    // Subscribe to flush buffered messages when connection is re-established
    bufferedMessages$.subscribe(messages => {
      messages.forEach(message => {
        this.socket.next(message);
      });
    });


    retrySocket.subscribe({
      next: msg => this.handleTextMessage(msg),
      error: err => Log.error(err),
      complete: () => this.afterConnectionClosed()
    });

    return firstValueFrom(retrySocket.pipe(
      catchError(err => {
        Log.error('Error on retrySocket: ' + err);
        // Return a fallback value or empty observable, so it doesn't reject
        return of(null);
      })
    ));
  }

  public onCommandReceived(observer: CommandReceiveObserver): ClientCommandWebsocketCommunication {
    this.commandObserver.push(observer);
    return this;
  }

  public onCommandResultReceived(observer: CommandResultReceiveObserver): ClientCommandWebsocketCommunication {
    this.commandResultObserver.push(observer);
    return this;
  }

  public isOpen(): boolean {
    return this.socket != null;
  }

  public afterConnectionClosed(): void {
    Log.warn(`Websocket connection closed`);
    this.socket.unsubscribe();
  }

  public handleTransportError(exception: any): void {
    Log.warn(`Websocket transportation error ${exception}`);
  }

  public sendMessage(message: string): boolean {
    let lastException: string = null;
    let tries: number = 0;

    // while (tries < ClientCommandWebsocketCommunication.MAX_TRIES) {
    // TODO AGe: what?
    let payload = JSON.parse(message);
    this.outgoingMessages.next(payload);
    // }

    return true;
    // TODO: identify unsuccessful
    /*
    console.error("0xWS001 An error occured on sending a websocket message", lastException);
    return false;
    /**/
  }

  // TODO AGe: any message
  protected async handleTextMessage(message: any): Promise<void> {
    if (this.expectInitMessage()) {
      this.established = true;
      // Fall initialisierung der Verbindung
      this.participantId = JSON.parse(message);
      if (Number.isNaN(this.participantId)) {
        this.participantId = null;
      }
    } else {
      // The client counts as busy until the message is processed (see PendingCommands)
      PendingCommands.begin();
      try {
        // Fall Antwort eines Requests, oder Erhalt eines weitergeleiteten Commands zur
        // Datensynchronisierung.
        // TODO AHe, MH: Wasteful way of distinguishing commands and results
        // TODO AGe: what?
        const payload = JSON.stringify(message);
        let commandResultOptional: CommandResult<any> = null;
        commandResultOptional = await CommandObjectMapper.deserializeResponse(payload);

        if (commandResultOptional != null) { // TODO MH: Need to check for participantId
          // Fall Antwort eines Requests
          let commandResult: CommandResult<any> = commandResultOptional;
          if (this.messages.has(commandResult.getCommandId())) {
            this.messages.get(commandResult.getCommandId()).next(commandResult);
          } else {
            // TODO AGe: received some rogue command?
          }
          // this.messages.set(commandResult.getCommandId(), commandResult);
          for (const observer of this.commandResultObserver) {
            observer(commandResult);
          }
        } else {
          let commandOptional: Command<Object> = null;
          commandOptional = await CommandObjectMapper.deserializeCommand(payload);
          if (commandOptional != null) {
            //Fall Antwort eines Erhalt eines weitergeleiteten Commands zur Datensynchronisierung
            Log.trace("Received command "+commandOptional.getId(),"ClientCommandWebsocketCommunication")
            let finalCommandOptional: Command<Object> = commandOptional;
            // Mutex
            this.commandObserverChain = this.commandObserverChain.then(() => {
              return this.commandObserver.reduce((prevPromise, consumer) => {
                return prevPromise.then(() => {
                  return consumer(finalCommandOptional);
                });
              }, Promise.resolve());
            });
            // Busy until the observers have processed the command
            PendingCommands.begin();
            const processed = () => PendingCommands.end();
            this.commandObserverChain.then(processed, processed);
          }
        }
      } finally {
        PendingCommands.end();
      }
    }
  }

  protected expectInitMessage(): boolean {
    return !this.established;
    // return this.initLatch.getCount() > 0;
  }

  public retryImmediate() {
    this.retryCount = 0;
    this.immediateRetry.next(true);
  }

  public pauseRetry() {
    this.retryCount = -2;
  }
}
