<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the live map: modifications through it are synchronized via the owner, like put${name?cap_first}/remove${name?cap_first} -->
${tc.signature("name", "mapType")}

${cd4c.method("public ${mapType} get${name?cap_first}Map()")}
return this._internal_get${name?cap_first}GemMap();
