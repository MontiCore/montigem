<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "keyType", "targetName")}

${cd4c.method("public Map<${keyType}, ${targetName}> get${name?cap_first}Map()")}
// read only: modifications have to go through put${name?cap_first} and remove${name?cap_first}
return Collections.unmodifiableMap(this._internal_get${name?cap_first}GemMap());
