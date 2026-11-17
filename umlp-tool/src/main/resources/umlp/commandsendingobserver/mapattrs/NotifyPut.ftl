<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "keyType", "valueType", "definingType")}
${cd4c.method("public void notifyPut${name?cap_first}(${definingType} o, ${keyType} key, ${valueType} value, ${valueType} oldValue)")}
CommandManager.sendCommandToSubscribers(new ${definingType}_put${name?cap_first}(o.getGemId(), key, value));
