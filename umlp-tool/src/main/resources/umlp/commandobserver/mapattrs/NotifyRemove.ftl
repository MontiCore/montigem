<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "keyType", "valueType", "definingType")}
${cd4c.method("public void notifyRemove${name?cap_first}(${definingType} o, ${keyType} key, ${valueType} oldValue)")}
CommandManager.executeCommand(new ${definingType}_remove${name?cap_first}(o.getGemId(), key));
