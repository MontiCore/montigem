<#-- (c) https://github.com/MontiCore/monticore -->
<#-- a value of the GemMap moved to a new key, as its qualifying attribute changed: tells the participants that
     loaded the owner, also those that did not load the value and thus do not receive its setter commands -->
${tc.signature("name", "keyType", "otherType")}
${cd4c.method("public void notifyRekey${name?cap_first}(${otherType} o, ${keyType} oldKey, ${keyType} newKey, long valueGemId)")}
  CommandManager.sendCommandToSubscribers(new ${otherType}_rekey${name?cap_first}(o.getGemId(), valueGemId, oldKey, newKey));
