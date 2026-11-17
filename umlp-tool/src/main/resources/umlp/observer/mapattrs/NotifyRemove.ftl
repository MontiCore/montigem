<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the entry of key was removed from a map attribute with simple values; oldValue: its value -->
${tc.signature("name", "className", "keyType", "valueType")}
${cd4c.method("void notifyRemove${name?cap_first}(${className} ${className?uncap_first}, ${keyType} key, ${valueType} oldValue)")}
