<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts o under the key derived from its qualifying attribute, replacing the value stored for that key;
     keyType: the stored key type (the key, or the gemId of the key object for GemEntryMaps).
     Replacing a stale entry (its value derives another key by now) is a repair; replacing a value that derives the
     same key means the qualifying attribute is not unique, which the server (participant 0) reports with errorCode -->
${tc.signature("name", "targetName", "sourceName", "isEntry", "keyType", "errorCode", "hasMirror", "moves")}

${cd4c.method("public void put${name?cap_first}(${targetName} o, NotificationScope scope)")}
${keyType} key = this._${name}.<#if isEntry>deriveKeyID<#else>deriveKey</#if>(o);
<#if !isEntry>
if (!this._${name}.isSafeKey(key)) {
  Log.error("0xA7326 The object with id " + o.getGemId() + " cannot be put under the key " + key
      + ", which a browser cannot represent exactly");
  return;
}
</#if>
Long oldID = this._${name}.getID(key);
if (oldID != null && oldID == o.getGemId()) {
  return;
}
if (oldID != null && CommandManager.getParticipantId() == 0
    && this._${name}.<#if isEntry>derivesKeyID<#else>derivesKey</#if>(oldID, key)) {
  Log.error("${errorCode} The object with id " + o.getGemId() + " replaces the object with id " + oldID
      + " under the key " + key + ", which both objects derive: the qualifying attribute is not unique");
}
<#if moves>
// a single owner: the value leaves its old owner's map
_internal_moveFromOldOwner${name?cap_first}(o.getGemId());
</#if>
this._${name}._internal_putValue(o);
check${name?cap_first}();
<#if hasMirror>
if (oldID != null) {
  _internal_mirrorRemove${name?cap_first}(oldID);
}
_internal_mirrorPut${name?cap_first}(o.getGemId());
</#if>
// a replaced value leaves with the put: every participant replaces it itself (see _internal_put${name?cap_first}Id),
// so its removal is not sent as a command of its own and the server sees the replacement
NotificationScope removeScope = scope.contains(NotificationScope.LOCAL) ? NotificationScope.LOCAL : scope;
for (${sourceName}Observer obs : ${sourceName?uncap_first}Observers) {
  if (oldID != null) {
    obs.maybeNotifyRemove${name?cap_first}((${sourceName}) this, oldID, -1, removeScope);
  }
  obs.maybeNotifyPut${name?cap_first}((${sourceName}) this, key, o.getGemId(), scope);
}
