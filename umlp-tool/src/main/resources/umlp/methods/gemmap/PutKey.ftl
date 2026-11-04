<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts o under the chosen key (type qualified association), replacing the value stored for that key, like
     java.util.Map#put; o is stored under one key only, so an entry it has under another key moves to key (see
     _internal_putValueID; the put tells the other participants, which move it as well).
     keyType: the key type (a domain class for GemEntryMaps); storedKeyType: the type the key is stored as (the key,
     or the gemId of the key object for GemEntryMaps) -->
${tc.signature("name", "keyType", "targetName", "sourceName", "isEntry", "storedKeyType")}

${cd4c.method("public void put${name?cap_first}(${keyType} key, ${targetName} o, NotificationScope scope)")}
if (key == null) {
  Log.error("0xA7323 The object with id " + o.getGemId() + " cannot be put under the key null");
  return;
}
<#if !isEntry>
if (!this._${name}.isSafeKey(key)) {
  Log.error("0xA7326 The object with id " + o.getGemId() + " cannot be put under the key " + key
      + ", which a browser cannot represent exactly");
  return;
}
</#if>
${storedKeyType} storedKey = <#if isEntry>key.getGemId()<#else>key</#if>;
Long oldID = this._${name}.getID(storedKey);
if (oldID != null && oldID == o.getGemId()) {
  return;
}
this._${name}._internal_putKeyValue(key, o);
check${name?cap_first}();
// a replaced value leaves with the put: every participant replaces it itself (see _internal_put${name?cap_first}Id),
// so its removal is not sent as a command of its own and the server sees the replacement
NotificationScope removeScope = scope.contains(NotificationScope.LOCAL) ? NotificationScope.LOCAL : scope;
for (${sourceName}Observer obs : ${sourceName?uncap_first}Observers) {
  if (oldID != null) {
    obs.maybeNotifyRemove${name?cap_first}((${sourceName}) this, oldID, -1, removeScope);
  }
  obs.maybeNotifyPut${name?cap_first}((${sourceName}) this, storedKey, o.getGemId(), scope);
}
