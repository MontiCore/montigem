/* (c) https://github.com/MontiCore/monticore */
import {JsonPrinter} from '../mcjson/JsonPrinter';
import {Log} from '../logging/Log';
import {CommandResult} from "./CommandResult";
import {StatusResult} from "./StatusResult";
import {BaseClassObjectMapper} from "../common";
import {JsonDeSer} from "./JsonDeSer";

export class CommandResultDeSer implements JsonDeSer<CommandResult<any>> {

  public isPojoDeSer(): boolean {
    return false;
  }
  constructor() {
  }

  public async __CommandResultDeSerConstructor() : Promise<CommandResultDeSer> {
    return this;
  }

  /* Default method injected from umlp.backendrte.command.JsonDeSer */
  serialize$java_lang_Object(obj: any): Promise<string> {
    const printer: JsonPrinter = new JsonPrinter(true);
    this.buildJson(obj, printer);
    return Promise.resolve(printer.getContent());
  }

  public async buildJson$umlp_backendrte_command_CommandResult$de_monticore_symboltable_serialization_JsonPrinter(result: CommandResult<any>, printer: JsonPrinter) {
    printer.member("commandId", result.getCommandId());
    const tp: string = result.getTypeParameter();
    printer.member("typeParameter", tp);
    printer.member("statusResult", result.isStatusResult());
    printer.member("simpleResult", result.isSimpleResult());
    printer.member("listResult", result.isListResult());
    if (result.isStatusResult()) {
      printer.member("successful", result.asStatusResult().wasSuccessful());
    } else {
      printer.member("successful", result.isSuccessful());
      if (BaseClassObjectMapper.canDeSer(tp)) {
        const v: string = await BaseClassObjectMapper.serialize(result.asSimpleResult().get(), tp);
        if (/* isPresent */(v != null)) {
          printer.memberJson("value", /* get */(v => {
            if (v == null) throw new Error('value is null');
            return v;
          })(v));
        } else {
          Log.warn("Error while serializing type " + tp);
        }
      }
    }

    // else if (result.isSimpleResult()) {
    //     printer.member("successful", result.asSimpleResult().isSuccessful());
    //     if (result.asSimpleResult().isSimpleResult() && BaseClassObjectMapper.canDeSer(tp)) {
    //       const v: string = BaseClassObjectMapper.serialize(result.asSimpleResult().get(), tp);
    //       if (/* isPresent */(v != null)) {
    //         printer.memberJson("value", /* get */(v => {
    //           if (v == null) throw new Error('value is null');
    //           return v;
    //         })(v));
    //       } else {
    //         Log.warn("Error while serializing type " + tp);
    //       }
    //     }
    //   } else if (result.isListResult()) {
    //     printer.member("successful", lr.isSuccessful());
    //     if (BaseClassObjectMapper.canDeSer(tp)) {
    //       let lr = result.asListResult();
    //       if (lr.isSuccessful()) {
    //         let objects: Array<string> = [];
    //         for await (const o of lr.get()) {
    //           let v: string = BaseClassObjectMapper.serialize(o, tp);
    //           if (v != null) {
    //             objects.push(v);
    //           } else {
    //             console.warn("Error while serializing type " + tp);
    //           }
    //         }
    //         let res: string = "[" + objects.join(",") + "]";
    //         printer["list"] = res;
    //       } else {
    //         printer["list"] = [];
    //       }
    //     }
    //   }
  }

  public buildJson(result?: any, printer?: any) {
    if (((result != null && result instanceof <any>CommandResult) || result === null) && ((printer != null && printer instanceof <any>JsonPrinter) || printer === null)) {
      return <any>this.buildJson$umlp_backendrte_command_CommandResult$de_monticore_symboltable_serialization_JsonPrinter(result, printer);
    } else throw new Error('invalid overload');
  }

  public serialize$umlp_backendrte_command_CommandResult(command: CommandResult<any>): Promise<string> {
    const printer: JsonPrinter = new JsonPrinter(true);
    printer.beginObject();
    this.buildJson$umlp_backendrte_command_CommandResult$de_monticore_symboltable_serialization_JsonPrinter(command, printer);
    printer.endObject();
    return Promise.resolve(printer.getContent());
  }

  /**
   *
   * @param {CommandResult} command
   * @return {string}
   */
  public serialize(command?: any): Promise<string> {
    return Promise.resolve(<any>this.serialize$umlp_backendrte_command_CommandResult(command));
  }

  public optSerialize(command : CommandResult<any>) : string {
    if(!BaseClassObjectMapper.canDeSer(command.getTypeParameter())) {
      return null;
    }
    const printer : JsonPrinter = new JsonPrinter(true);
    printer.beginObject();
    this.buildJson(command, printer);
    printer.endObject();
    return printer.getContent();
}

  public async fromJson(jsonObject: any): Promise<CommandResult<any>> {
    if (/* orElse */((v, v2) => v == null ? v2 : v)(jsonObject["statusResult"], false)) {
      return Promise.resolve((v => {
        if (v == null) throw new Error('value is null');
        return v;
      })(await new StatusResult().__StatusResultConstrcutor(jsonObject["commandId"], jsonObject["successful"])));
    }

    return Promise.resolve(null);
  }

  public deserialize(json: string): Promise<CommandResult<any>> {
    return Promise.resolve(this.fromJson(JSON.parse(json)));
  }

  protected canDeser(className : string) : boolean {
    return BaseClassObjectMapper.canDeSer(className);
  }
}

CommandResultDeSer["__class"] = "umlp.backendrte.command.CommandResultDeSer";
CommandResultDeSer["__interfaces"] = ["umlp.backendrte.command.JsonDeSer", "umlp.backendrte.command.DeSer"];

