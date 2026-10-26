import {BaseClassObjectMapper} from './BaseClassObjectMapper';
import {DeSer} from '../command/DeSer';
import {PojoJsonDeSer} from '../command/PojoJsonDeSer';
import {JsonPrinter} from '../mcjson/JsonPrinter';

/** Per-traversal identity table; mirrors the Java POJO graph wire format. */
export class PojoGraphContext {
  private written = new Map<object, string>();
  private read = new Map<string, any>();
  private readTypes = new Map<string, string>();
  private finishers: Array<() => Promise<void>> = [];
  private writeDepth = 0;
  private readDepth = 0;

  public async __PojoGraphContextConstructor(): Promise<PojoGraphContext> {
    return this;
  }

  public async serialize(object: any, className: string): Promise<string> {
    const deser = BaseClassObjectMapper.getInstance().getBaseDeSers().get(className);
    if (this.isPojoDeSer(deser)) {
      return this.serializeObject(object, deser as PojoJsonDeSer<any>);
    }
    return BaseClassObjectMapper.serialize(object, className);
  }

  public async deserialize<T>(json: string, className: string): Promise<T> {
    const deser = BaseClassObjectMapper.getInstance().getBaseDeSers().get(className);
    if (this.isPojoDeSer(deser)) {
      return this.deserializeObject(JSON.parse(json), deser as PojoJsonDeSer<T>);
    }
    return BaseClassObjectMapper.deserialize<T>(json, className);
  }

  public async deserializeValue<T>(value: any, className: string): Promise<T> {
    const deser = BaseClassObjectMapper.getInstance().getBaseDeSers().get(className);
    if (this.isPojoDeSer(deser)) {
      return this.deserializeObject(value, deser as PojoJsonDeSer<T>);
    }
    return BaseClassObjectMapper.deserialize<T>(JSON.stringify(value), className);
  }

  /** Every DeSer states itself whether it handles POJO graphs, so no member is probed here. */
  public isPojoDeSer(deser: DeSer<any>): boolean {
    return deser != null && deser.isPojoDeSer();
  }

  public async serializeObject<T>(object: T, deser: PojoJsonDeSer<T>): Promise<string> {
    const printer = new JsonPrinter(true);
    printer.beginObject();
    await this.writeObject(object, printer, deser);
    printer.endObject();
    return printer.toString();
  }

  public async writeObject<T>(object: T, printer: JsonPrinter, deser: PojoJsonDeSer<T>): Promise<void> {
    if (object == null) {
      throw new Error('Cannot serialize a null POJO');
    }
    this.writeDepth++;
    try {
      const key = object as object;
      if (this.written.has(key)) {
        printer.member('$ref', this.written.get(key));
      } else {
        const id = String(this.written.size);
        this.written.set(key, id);
        printer.member('$id', id);
        await deser.writeJson(object, printer, this);
      }
    } finally {
      if (--this.writeDepth === 0) this.written.clear();
    }
  }

  public async deserializeObject<T>(json: any, deser: PojoJsonDeSer<T>): Promise<T> {
    if (typeof json === 'string') {
      json = JSON.parse(json);
    }
    this.readDepth++;
    try {
      if (Object.prototype.hasOwnProperty.call(json, '$ref')) {
        const id = json.$ref;
        if (Object.prototype.hasOwnProperty.call(json, '$id') || !this.read.has(id)
            || deser.getTypeIdentifier() !== this.readTypes.get(id)) {
          throw new Error('Invalid POJO reference: ' + id);
        }
        return this.read.get(id);
      }
      const object = await deser.createObject();
      if (Object.prototype.hasOwnProperty.call(json, '$id')) {
        const id = json.$id;
        if (typeof id !== 'string' || this.read.has(id)) {
          throw new Error('Invalid or duplicate POJO identity: ' + id);
        }
        this.read.set(id, object);
        this.readTypes.set(id, deser.getTypeIdentifier());
      }
      this.finishers.push(async () => {
        if (!await deser.finishObject(object)) {
          throw new Error('Invalid POJO: ' + deser.getTypeIdentifier());
        }
      });
      await deser.readJson(object, json, this);
      if (this.readDepth === 1) {
        for (const finish of this.finishers) await finish();
      }
      return object;
    } finally {
      if (--this.readDepth === 0) {
        this.read.clear();
        this.readTypes.clear();
        this.finishers = [];
      }
    }
  }
}

PojoGraphContext['__class'] = 'umlp.backendrte.common.PojoGraphContext';
