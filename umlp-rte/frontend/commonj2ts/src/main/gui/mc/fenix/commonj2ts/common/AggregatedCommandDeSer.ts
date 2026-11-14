import {JsonPrinter} from '../mcjson/JsonPrinter';
import {Log} from '../logging/Log';
import {Command} from '../command/Command';
import {CommandDeSer} from "../command";
import {IUMLPMap} from "./IUMLPMap";
import {AsyncMap} from "../javautil/AsyncMap";

export abstract class AggregatedCommandDeSer {
  /** java.util.Map in transpiled code: filled by the generated subclass, read here via __internalMap() */
  deSerMap: IUMLPMap<string, CommandDeSer<any>> = AsyncMap.__viewOf(new Map());

  constructor() {
  }

  public async __AggregatedCommandDeSerConstructor(): Promise<void> {
    this.deSerMap = AsyncMap.__viewOf(new Map());
  }

  public buildJson(command: Command<any>, printer: JsonPrinter) {
    const commandType: string = command.getCommandType();
    if (this.deSerMap.__internalMap().has(commandType)) {
      this.deSerMap.__internalMap().get(commandType).buildJson(command, printer);
    } else {
      Log.error("Can not find deSer for commandType " + commandType);
    }
  }

  public async serialize(command: Command<any>): Promise<string | null> {
    const commandType: string = command.getCommandType();
    if (!this.deSerMap.__internalMap().has(commandType)) {
      return Promise.resolve(null);
    }

    const printer: JsonPrinter = new JsonPrinter(true);
    printer.beginObject();
    await this.deSerMap.__internalMap().get(commandType).buildJson(command, printer);
    printer.endObject();

    return Promise.resolve(printer.toString());
  }

  public async deserialize<T>(cmdStr: string): Promise<Command<any>> {
    return await this.fromJson<any>(JSON.parse(cmdStr));
  }

  public async fromJson<T>(jsonObject: any): Promise<Command<any>> {
    if (typeof jsonObject["commandType"] !== 'string') {
      return null;
    }
    const commandType: string = jsonObject["commandType"];
    if (!this.deSerMap.__internalMap().has(commandType)) {
      // Not an error: aggregated DeSers are chained by the generated
      // CommandObjectMapper (app DeSer first, imported-domain DeSers as
      // fallback). A miss here is expected for every broadcast of an imported
      // domain and used to flood the console with errors on live updates.
      // The mapper logs when ALL DeSers in the chain fail.
      Log.debug("No deSer for commandType " + commandType + ", trying next in chain", "AggregatedCommandDeSer");
      return null;
    }
    return await this.deSerMap.__internalMap().get(commandType).fromJson(jsonObject);
  }
}

AggregatedCommandDeSer["__class"] = "umlp.backendrte.common.AggregatedCommandDeSer";



