/* (c) https://github.com/MontiCore/monticore */
import {Log} from '../logging/Log';
import {LogUtil} from '../util/LogUtil';
import {CommandObjectMapper} from './CommandObjectMapper';
import {Command} from './Command';
import {CommandResult} from './CommandResult';
import {Participant} from './Participant';
import {DomainTypeFilter} from "./DomainTypeFilter";
import {PendingCommands} from "./PendingCommands";

export class CommandManager {
  protected static instance: CommandManager = null;
  // maximum number of gemIDs per LoadAll<X>WithIDs command
  public static loadBatchSize: number = 5000;
  protected participantId: number = null;
  protected particpants: any;
  protected runningClientId: number;
  protected freeCommandId: number = 0;

  constructor() {
    this.participantId = 0;
    this.particpants = <any>({});
    this.runningClientId = 1;
    if (this.freeCommandId === undefined) {
      this.freeCommandId = 0;
    }
  }

  public static async register(participant: Participant): Promise<number> {
    return await CommandManager.getInstance()._register(participant);
  }

  public static async deregister(participantId: number) {
    await CommandManager.getInstance()._deregister(participantId);
  }

  public static getParticipantId(): number {
    return CommandManager.getInstance()._getParticipantId();
  }

  public static init() {
    CommandManager.instance = new CommandManager();
  }

  public static initMe(commandManager: CommandManager) {
    CommandManager.instance = commandManager;
  }

  public static getInstance(): CommandManager {
    if (CommandManager.instance == null) {
      Log.warn("0x10000 Command manager was not initialized");
      CommandManager.init();
    }
    return CommandManager.instance;
  }

  public static async executeCommand<T>(command: Command<T>): Promise<CommandResult<T>> {
    return PendingCommands.track(async () => {
      Log.trace("Exec command " + command.getId() + " " + command.getCommandType() + " from client " + command.getParticipantId(), LogUtil.getLoggerName());
      Log.trace((await CommandObjectMapper.serializeCommand(command)), LogUtil.getLoggerName());
      return await CommandManager.getInstance()._executeCommand<any>(command);
    });
  }

  public static sendCommandToSubscribers<T>(command: Command<T>) {
    CommandManager.getInstance()._sendCommandToSubscribers<any>(command);
  }

  public static async shouldSendCommand<T>(command: Command<T>, participant: Participant): Promise<boolean> {
    return this.getInstance()._shouldSendCommand(command, participant);
  }

  public static subscribeToType(participantId: number, domainTypeFilter: DomainTypeFilter) {
    this.getInstance()._subscribeToType(participantId, domainTypeFilter);
  }

  public static getFreeCommandId(): number {
    return CommandManager.getInstance()._getFreeCommandId();
  }

  public async _register(participant: Participant): Promise<number> {
    const participantId: number = this.runningClientId++;
    /* put */
    ((m, k, v) => {
      if (m.entries == null) m.entries = [];
      for (let i = 0; i < m.entries.length; i++) if (m.entries[i].key == null && k == null || m.entries[i].key.equals != null && m.entries[i].key.equals(k) || m.entries[i].key === k) {
        m.entries[i].value = v;
        return;
      }
      m.entries.push({
        key: k, value: v, getKey: function () {
          return this.key
        }, getValue: function () {
          return this.value
        }
      });
    })(<any>this.particpants, participantId, participant);
    return participantId;
  }

  async _deregister(participantId: number) {
    /* remove */
    ((m, k) => {
      if (m.entries == null) m.entries = [];
      for (let i = 0; i < m.entries.length; i++) if (m.entries[i].key == null && k == null || m.entries[i].key.equals != null && m.entries[i].key.equals(k) || m.entries[i].key === k) {
        return m.entries.splice(i, 1)[0];
      }
    })(<any>this.particpants, participantId);
  }

  async _sendCommandToSubscribers<T>(command: Command<T>) {
    {
      let array = /* keySet */((m) => {
        let r = [];
        if (m.entries == null) m.entries = [];
        for (let i = 0; i < m.entries.length; i++) r.push(m.entries[i].key);
        return r;
      })(<any>this.particpants);
      for (let index = 0; index < array.length; index++) {
        let participantId = array[index];
        {
          const participant: Participant = /* get */((m, k) => {
            if (m.entries == null) m.entries = [];
            for (let i = 0; i < m.entries.length; i++) if (m.entries[i].key == null && k == null || m.entries[i].key.equals != null && m.entries[i].key.equals(k) || m.entries[i].key === k) {
              return m.entries[i].value;
            }
            return null;
          })(<any>this.particpants, participantId);
          if (!(participantId === command.getParticipantId())) {
            let match: boolean = false;
            {
              for await (let gemId of await command.getEffectedGemIds()) {
                if (participant.isSubscribedTo(gemId)) {
                  match = true;
                  break;
                }
              }
            }
            if (match) {
              Log.trace("Sending command " + command.getId() + " " + command.getCommandType() + " to " + participantId, LogUtil.getLoggerName());
              participant.sendCommand<any>(command);
            } else {
              Log.trace("Skip sending command " + command.getCommandType() + " to " + participantId, LogUtil.getLoggerName());
            }
          }
        }
      }
    }
  }

  public getParticipant(id: number): Participant {
    return this.particpants.get(id);
  }

  protected _getParticipantId(): number {
    return this.participantId;
  }

  protected async _executeCommand<T>(command: Command<T>): Promise<CommandResult<T>> {
    const res: CommandResult<T> = await command.doRun();
    if (/* containsKey */((m, k) => {
      if (m.entries == null) m.entries = [];
      for (let i = 0; i < m.entries.length; i++) if (m.entries[i].key == null && k == null || m.entries[i].key.equals != null && m.entries[i].key.equals(k) || m.entries[i].key === k) {
        return true;
      }
      return false;
    })(<any>this.particpants, command.getParticipantId())) {
      /* get */
      ((m, k) => {
        if (m.entries == null) m.entries = [];
        for (let i = 0; i < m.entries.length; i++) if (m.entries[i].key == null && k == null || m.entries[i].key.equals != null && m.entries[i].key.equals(k) || m.entries[i].key === k) {
          return m.entries[i].value;
        }
        return null;
      })(<any>this.particpants, command.getParticipantId()).subscribe(command.getNewlySubscribedGemIds());
    } else {
    }
    if (!command.isRequest()) {
      this._sendCommandToSubscribers<any>(command);
    }
    return res;
  }

  protected async _shouldSendCommand<T>(command: Command<T>, participant: Participant): Promise<boolean> {
    for await (let gemId of await command.getEffectedGemIds()) {
      if (participant.isSubscribedTo(gemId)) {
        return true;
      }
    }

    return participant.isSubscribedToType(command.getCommandType());
  }

  protected _subscribeToType(participantId: number, domainTypeFilter: DomainTypeFilter) {
    let p = this.particpants.get(participantId);
    if (this.particpants.hasOwnProperty(participantId)) {
      p.subscribeType(domainTypeFilter);
    }
  }

  protected _getFreeCommandId(): number {
    return this.freeCommandId++;
  }
}

CommandManager["__class"] = "umlp.backendrte.command.CommandManager";

