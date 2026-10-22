<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "mapType")}

${cd4c.method("public ${mapType} _internal_get${name?cap_first}GemMap()")}
return this._${name};
