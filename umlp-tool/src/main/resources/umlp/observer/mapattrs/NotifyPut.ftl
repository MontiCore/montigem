<#-- (c) https://github.com/MontiCore/monticore -->
<#-- oldValue: the replaced value, null if none -->
${tc.signature("name", "className", "keyType", "valueType")}
${cd4c.method("void notifyPut${name?cap_first}(${className} o, ${keyType} key, ${valueType} value, ${valueType} oldValue)")}
