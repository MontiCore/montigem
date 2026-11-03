import {JsonDeSer} from './JsonDeSer';
import {JsonPrinter} from '../mcjson/JsonPrinter';
import {PojoGraphContext} from '../common/PojoGraphContext';

export interface PojoJsonDeSer<T> extends JsonDeSer<T> {
  getTypeIdentifier(): string;
  createObject(): Promise<T>;
  readJson(object: T, json: any, context: PojoGraphContext): Promise<void>;
  writeJson(object: T, printer: JsonPrinter, context: PojoGraphContext): Promise<void>;
  finishObject(object: T): Promise<boolean>;
}
