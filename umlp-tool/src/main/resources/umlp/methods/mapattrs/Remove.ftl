<#-- (c) https://github.com/MontiCore/monticore -->
<#-- removes the entry of key; returns its value, null if none -->
${tc.signature("domain", "name", "keyType", "valueType")}

${cd4c.method("public ${valueType} remove${name?cap_first}(${keyType} key, NotificationScope scope)")}
if (!this._${name}.containsKey(key)) {
  return null;
}
${valueType} oldValue = this._${name}.remove(key);
check${name?cap_first}();
for (${domain}Observer obs : ${domain?uncap_first}Observers) {
  obs.maybeNotifyRemove${name?cap_first}((${domain}) this, key, oldValue, scope);
}
return oldValue;
