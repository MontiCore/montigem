<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the value stored for key: the value side of a qualified association is [1], each key stands for one value
     (CoCo 0xCDC6C); a key without a value is an error, callers that are unsure ask containsKey first -->
${tc.signature("name", "keyType", "targetName")}

${cd4c.method("public ${targetName} get${name?cap_first}(${keyType} key)")}
${targetName} value = this._internal_get${name?cap_first}GemMap().get(key);
if (value == null) {
  Log.error("0xA7322 There is no ${name} with key " + key);
}
return value;
