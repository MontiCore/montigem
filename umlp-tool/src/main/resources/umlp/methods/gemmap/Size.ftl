<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name")}

${cd4c.method("public int size${name?cap_first}s()")}
return this._internal_get${name?cap_first}GemMap().size();
