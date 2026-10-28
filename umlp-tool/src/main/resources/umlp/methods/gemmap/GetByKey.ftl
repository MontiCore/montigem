<#-- (c) https://github.com/MontiCore/monticore -->
<#-- isOne: target cardinality [1], every key yields exactly one value -->
${tc.signature("name", "keyType", "targetName", "isOne")}

<#if isOne>
${cd4c.method("public ${targetName} get${name?cap_first}(${keyType} key)")}
${targetName} value = this._internal_get${name?cap_first}GemMap().get(key);
if (value == null) {
  Log.error("0xA7322 There is no ${name} with key " + key);
}
return value;
<#else>
${cd4c.method("public Optional<${targetName}> get${name?cap_first}(${keyType} key)")}
return Optional.ofNullable(this._internal_get${name?cap_first}GemMap().get(key));
</#if>
