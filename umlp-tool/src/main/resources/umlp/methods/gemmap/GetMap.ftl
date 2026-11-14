<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the live map, as a plain java.util.Map like get<Role>List() for lists:
     modifications through it are synchronized via the owner, like put${name?cap_first}/remove${name?cap_first} -->
${tc.signature("name", "keyType", "targetName")}

${cd4c.method("public Map<${keyType}, ${targetName}> get${name?cap_first}Map()")}
return this._internal_get${name?cap_first}GemMap();
