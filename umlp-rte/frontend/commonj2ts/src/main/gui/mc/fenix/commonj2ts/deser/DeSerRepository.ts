/* Generated from Java with JSweet 3.2.0-UMLP-SNAPSHOT - http://www.jsweet.org */
import { JsonDeSer } from '../command/JsonDeSer';
import { IUMLPMap } from '../common/IUMLPMap';

export interface DeSerRepository {
    getDeSerMap(): Promise<IUMLPMap<string, JsonDeSer<any>>>;
}
