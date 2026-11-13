<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "keyType")}

${cd4c.method("public boolean containsKey${name?cap_first}(${keyType} key)")}
return this._internal_get${name?cap_first}GemMap().containsKey(key);
