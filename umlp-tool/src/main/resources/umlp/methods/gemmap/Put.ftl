<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts o under the key derived from its qualifying attribute, replacing the value stored for that key -->
${tc.signature("name", "targetName", "sourceName", "isEntry")}

${cd4c.method("public void put${name?cap_first}(${targetName} o, NotificationScope scope)")}
Long oldID = this._${name}.getID(this._${name}.<#if isEntry>deriveKeyID<#else>deriveKey</#if>(o));
if (oldID != null && oldID == o.getGemId()) {
  return;
}
this._${name}._internal_putValue(o);
check${name?cap_first}();
for (${sourceName}Observer obs : ${sourceName?uncap_first}Observers) {
  if (oldID != null) {
    obs.maybeNotifyRemove${name?cap_first}((${sourceName}) this, oldID, -1, scope);
  }
  obs.maybeNotifyAdd${name?cap_first}((${sourceName}) this, o.getGemId(), -1, scope);
}
