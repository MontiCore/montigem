/* (c) https://github.com/MontiCore/monticore */

export interface DeSer<T> {
  serialize(obj: T): Promise<string>;

  deserialize(str: string): Promise<T>;

  isPojoDeSer(): boolean;
}
