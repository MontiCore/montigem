/* (c) https://github.com/MontiCore/monticore */
package hardy.flensburg;

// HWC-P

import umlp.backendrte.common.SerializableDeSer;

// adds java.util.Date and java.net.InetAddress to the BaseClassObjectMapper
public class FlensburgBaseClassObjectMapper extends FlensburgBaseClassObjectMapperTOP {
  FlensburgBaseClassObjectMapper(){
    baseDeSers.put("java.util.Date", new DateDeSer());
    baseDeSers.put("java.net.InetAddress", new SerializableDeSer());
  }
}
