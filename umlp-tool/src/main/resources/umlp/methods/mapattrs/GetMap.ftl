<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the map itself, like get<Attr>List(); changes through it are not synchronized (known bug) -->
${tc.signature("name", "keyType", "valueType")}

${cd4c.method("public Map<${keyType}, ${valueType}> get${name?cap_first}Map()")}
return this._${name};
