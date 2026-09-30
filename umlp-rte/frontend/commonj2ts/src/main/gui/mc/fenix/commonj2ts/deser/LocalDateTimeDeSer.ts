/* (c) https://github.com/MontiCore/monticore */

import { DeSer } from "../command/DeSer";

// TODO AGe: moment
export class LocalDateTimeDeSer implements DeSer<Date> {

  public isPojoDeSer(): boolean {
    return false;
  }

  public async serialize(date: Date): Promise<string> {
    const year = String(date.getFullYear()).padStart(4, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hour = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2,'0');
    const seconds = String(date.getSeconds()).padStart(2,'0');

    return `${year}-${month}-${day}T${hour}:${minutes}:${seconds}`;
  }

  public async deserialize(str: string): Promise<Date> {
    // The pattern should be extended to "/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d{1,9})?)?$/"
    // const pattern = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/;
    // if (!pattern.test(str)) {
    //   console.warn("LocalDateTime must be deserialized from json string");
    //   return null;
    // }
    //
    // const dateTime = str.split('T')
    // const parts = dateTime[0].split('-');
    // const [year, month, day] = [+parts[0], +parts[1], +parts[2]];
    //
    // const timeParts = dateTime[1].split(':');
    // const [hours, minutes, seconds] = [+timeParts[0], +timeParts[1], +timeParts[2]];
    // return new Date(year, month - 1, day, hours, minutes, seconds);
    // For now, let JavaScript handle this
    let date1 = new Date(str);
    let date2 = new Date(str.substring(1, str.length - 1));
    if (isNaN(date1.getDate())) {
      return date2;
    } else {
      return date1;
    }
  }
}
