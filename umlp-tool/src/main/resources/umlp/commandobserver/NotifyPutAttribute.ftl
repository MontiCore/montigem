<#-- (c) https://github.com/MontiCore/monticore -->
<#-- a value was put into the GemMap of an attribute qualified association (always unidirectional) -->
${tc.signature("name", "keyType", "otherType")}
${cd4c.method("public void notifyPut${name?cap_first}(${otherType} o, ${keyType} key, long valueGemId)")}
  CommandManager.executeCommand(new ${otherType}_put${name?cap_first}(o.getGemId(), valueGemId, key));
