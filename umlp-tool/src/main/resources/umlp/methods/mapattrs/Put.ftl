<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts value under key; returns the old value, null if none -->
${tc.signature("domain", "name", "keyType", "valueType", "isLongKey")}

${cd4c.method("public ${valueType} put${name?cap_first}(${keyType} key, ${valueType} value, NotificationScope scope)")}
if (key == null || value == null) {
  Log.error("0xA7331 The ${name} of the object with id " + getGemId() + " cannot hold the key " + key
      + " with the value " + value + ": null keys and values are not allowed");
  return null;
}
<#if isLongKey>
if (key > GemValueMap.MAX_SAFE_KEY || key < -GemValueMap.MAX_SAFE_KEY) {
  Log.error("0xA7332 The ${name} of the object with id " + getGemId() + " cannot hold the key " + key
      + ", which a browser cannot represent exactly");
  return null;
}
</#if>
${valueType} oldValue = this._${name}.put(key, value);
if (Objects.equals(value, oldValue)) {
  return oldValue;
}
set${name?cap_first}Present(true);
check${name?cap_first}();
for (${domain}Observer obs : ${domain?uncap_first}Observers) {
  obs.maybeNotifyPut${name?cap_first}((${domain}) this, key, value, oldValue, scope);
}
return oldValue;
