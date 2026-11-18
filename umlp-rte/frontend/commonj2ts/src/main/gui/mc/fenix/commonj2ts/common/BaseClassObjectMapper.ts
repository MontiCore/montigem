/* (c) https://github.com/MontiCore/monticore */
import {Log} from '../logging/Log';
import {BooleanDeSer} from "../common/BooleanDeSer";
import {ShortDeSer} from "../common/ShortDeSer";
import {CharacterDeSer} from "../common/CharacterDeSer";
import {LongDeSer} from "../common/LongDeSer";
import {IntegerDeSer} from "../common/IntegerDeSer";
import {ByteDeSer} from "../common/ByteDeSer";
import {FloatDeSer} from "../common/FloatDeSer";
import {DoubleDeSer} from "../common/DoubleDeSer";
import {StringDeSer} from "../common/StringDeSer";
import {LocalDateDeSer} from "../deser";
import {LocalDateTimeDeSer} from "../deser/LocalDateTimeDeSer";
import {DeSer, JsonDeSer} from "../command";
import {IUMLPMap} from "./IUMLPMap";
import {AsyncMap} from "../javautil/AsyncMap";

/**
 * (De)serializer for base classes from java.lang
 * Since this uses the static delegate pattern, additional base classes can be added via subclassing.
 * (See FlensburgBaseClassObjectMapper)
 * @class
 */
export class BaseClassObjectMapper {
  static instance: BaseClassObjectMapper;

  protected baseDeSers: Map<string, DeSer<any>> = new Map();

  public constructor() {
    // Put this into construcot instead of async constructor
    this.baseDeSers.set("java.lang.Boolean", new BooleanDeSer());
    this.baseDeSers.set("java.lang.Short", new ShortDeSer());
    this.baseDeSers.set("java.lang.Character", new CharacterDeSer());
    this.baseDeSers.set("java.lang.Long", new LongDeSer());
    this.baseDeSers.set("java.lang.Integer", new IntegerDeSer());
    this.baseDeSers.set("java.lang.Byte", new ByteDeSer());
    this.baseDeSers.set("java.lang.Float", new FloatDeSer());
    this.baseDeSers.set("java.lang.Double", new DoubleDeSer());
    this.baseDeSers.set("java.lang.String", new StringDeSer());

    this.baseDeSers.set("java.time.LocalDate", new LocalDateDeSer());
    this.baseDeSers.set("java.time.LocalDateTime", new LocalDateTimeDeSer());
  }

  public async __BaseClassObjectMapperConstructor(): Promise<BaseClassObjectMapper> {
    return this;
  }

  /** java.util.Map in transpiled code: a live view of the base DeSers */
  public getBaseDeSers() : IUMLPMap<string, DeSer<any>> {
    return AsyncMap.__viewOf(this.baseDeSers);
  }

  /**
   * Synchronous like in Java: the DeSers come from an AsyncMap (generated DeSerRepository#getDeSerMap)
   * or a native Map (libraries transpiled before maps became IUMLPMaps)
   */
  public static addDeSers(addedDeSers : IUMLPMap<string, JsonDeSer<any>> | Map<string, JsonDeSer<any>>) : void {
    const deSers = addedDeSers instanceof Map ? addedDeSers : addedDeSers.__internalMap();
    for (const [deSerTypeName,deSer] of deSers) {
      this.getInstance().baseDeSers.set(deSerTypeName, deSer)
    }
  }

  public static instance_$LI$(): BaseClassObjectMapper {
    if (BaseClassObjectMapper.instance == null) {
      BaseClassObjectMapper.instance = new BaseClassObjectMapper();
    }
    return BaseClassObjectMapper.instance;
  }

  public static getInstance(): BaseClassObjectMapper {
    return BaseClassObjectMapper.instance_$LI$();
  }

  public static async serialize(o: any, className: string): Promise<string> {
    return await BaseClassObjectMapper.getInstance()._serialize(o, className);
  }

  public static async deserialize<T>(s: string, className: string): Promise<T> {
    return await BaseClassObjectMapper.getInstance()._deserialize<any>(s, className);
  }

  public static canDeSer(className: string): boolean {
    return BaseClassObjectMapper.getInstance()._canDeSer(className);
  }

  public async _serialize(o: any, className: string): Promise<string> {
    if (!this.baseDeSers.has(className)) {
      return null;
    }
    return await this.baseDeSers.get(className).serialize(o);
  }

  public async _deserialize<T>(s: string, className: string): Promise<T> {
    if (!this.baseDeSers.has(className)) {
      return null;
    }
    try {
      return await <T>this.baseDeSers.get(className).deserialize(s);
    } catch (ex) {
      Log.error("Wrong return type of base class deser: \'" + className + "\'");
      return null;
    }
  }

  public _canDeSer(className: string): boolean {
    return this.baseDeSers.has(className);
  }
}

BaseClassObjectMapper["__class"] = "umlp.backendrte.common.BaseClassObjectMapper";

