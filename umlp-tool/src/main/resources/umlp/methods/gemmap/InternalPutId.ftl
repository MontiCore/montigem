<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts the value with gemID under key without loading it, e.g. as sent by the server (command _put);
     keyType: the stored key type (the key, or the gemId of the key object for GemEntryMaps) -->
${tc.signature("name", "sourceName", "keyType")}

${cd4c.method("public void _internal_put${name?cap_first}Id(${keyType} key, long gemID, NotificationScope scope)")}
Long oldID = this._${name}.getID(key);
if (oldID != null && oldID == gemID) {
  return;
}
this._${name}._internal_put(key, gemID);
check${name?cap_first}();
for (${sourceName}Observer obs : ${sourceName?uncap_first}Observers) {
  if (oldID != null) {
    obs.maybeNotifyRemove${name?cap_first}((${sourceName}) this, oldID, -1, scope);
  }
  obs.maybeNotifyPut${name?cap_first}((${sourceName}) this, key, gemID, scope);
}
