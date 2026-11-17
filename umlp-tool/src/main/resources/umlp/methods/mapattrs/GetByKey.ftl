<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the value of key; a missing key is an error, like for GemMaps -->
${tc.signature("name", "keyType", "valueType")}

${cd4c.method("public ${valueType} get${name?cap_first}(${keyType} key)")}
${valueType} value = this._${name}.get(key);
if (value == null) {
  Log.error("0xA7333 There is no ${name} with key " + key);
}
return value;
