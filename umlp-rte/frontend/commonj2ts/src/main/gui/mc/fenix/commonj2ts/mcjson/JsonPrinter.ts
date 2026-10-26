import {Log} from "../logging";

export class JsonPrinter {
  res = null;

  // Is a queue
  currElements = [];

  constructor(b?: boolean) {}

  async __JsonPrinterConstructor(b: boolean) {
    return this;
  }

  beginObject(kind?: string): void {
    let obj = {};
    if (!!kind) {
      if (this.currElements.length > 0) {
        (this.getCurrentElement())[kind] = obj;
      } else {
        Log.error("0xA0613 JsonPrinter detected an invalid nesting of Json. "
          + "Cannot add a member as the first element of a Json String!");
      }
    } else if (this.currElements.length > 0) {
      if (this.getCurrentElement() instanceof Array) {
        // an object as an element of an array, like the Java JsonPrinter
        this.getCurrentElement().push(obj);
      } else {
        Log.error("0xA0600 JsonPrinter detected an invalid nesting of Json. "
          + "A JSON object within a JSON object must be a member!");
      }
    } else if (this.res == null) {
      this.res = obj;
    }
    // pushed in any case, so that endObject() pops this object
    this.currElements.push(obj);
  }

  beginArray(kind?: string): void {
    let obj = [];
    if (!!kind) {
      if (this.currElements.length > 0) {
        (this.getCurrentElement())[kind] = obj;
      } else {
        Log.error("0xA0613 JsonPrinter detected an invalid nesting of Json. "
          + "Cannot add a member as the first element of a Json String!");
      }
    }
    this.currElements.push(obj);
  }

  endObject(): void {
    this.currElements.pop();
  }

  endArray(): void {
    this.currElements.pop();
  }

  member(key: string, value: any): void {
    (this.getCurrentElement())[key] = value;
  }

  getContent(): any {
    return this.res;
  }

  memberJson(kind: string, value: string) {
    let parseElement: any = null;
    // TODO: Fallunterscheidung eleganter lösen
    try {
      parseElement = JSON.parse(value);
    } catch (err) {
      try {
        parseElement = JSON.parse("{\"" + kind + "\": \"" + value + "\"}")[kind];
      } catch (err) {
        Log.error("0xA0619 JsonPrinter cannot parse the provided JSON value: " + value);
      }
    }
    (this.getCurrentElement())[kind] = parseElement;
  }

  value(value: any) {
    // TODO
    if(typeof value === "number") {
      this.internalNumberValue(value)
    }
  }

  valueJson(value : string) : void {
    this.internalStringValue(value);
  }

  internalNumberValue(value : number) : void{
    if (!(this.currElements.length === 0) && this.getCurrentElement() instanceof Array) {
      const parent = this.getCurrentElement()
      parent.push(value);
    }
    else if (!(this.currElements.length === 0) && this.getCurrentElement().isJsonObject()) {
      Log.error("0xA0604 JsonPrinter detected an invalid nesting of Json. "
        + "Cannot add a numeric value `" + value + "` to the JSON object: `"
        + toString() + "`");
    }
    else {
      this.currElements.push(value);
    }
  }

  internalStringValue(value : string) : void {
    if (!(this.currElements.length === 0) && this.getCurrentElement() instanceof Array) {
      const parent = this.getCurrentElement();
      parent.push(JSON.stringify(value))
    }
    else if (!(this.currElements.length === 0) && this.getCurrentElement().isJsonObject()) {
      Log.error("0xA0603 JsonPrinter detected an invalid nesting of Json. "
        + "Cannot add a String value `" + value + "` to the JSON object: `"
        + toString() + "`");
    }
    else {
      this.currElements.push(JSON.stringify(value));
    }
  }

  toString(): string {
    return JSON.stringify(this.res);
  }

  private getCurrentElement() {
    return this.currElements[this.currElements.length - 1];
  }
}
