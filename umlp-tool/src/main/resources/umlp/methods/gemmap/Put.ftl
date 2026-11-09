<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts o under the key derived from its qualifying attribute, replacing the value stored for that key;
     keyType: the stored key type (the key, or the gemId of the key object for GemEntryMaps).
     Replacing a stale entry (its value derives another key by now) is a repair; replacing a value that derives the
     same key means the qualifying attribute is not unique, which the server (participant 0) reports with errorCode.
     otherSideName, otherSideIsList, targetManager, ownerType, ownerManager: the other side of a bidirectional
     association, see InternalPutId -->
${tc.signature("name", "targetName", "sourceName", "isEntry", "keyType", "errorCode", "otherSideName", "otherSideIsList",
    "targetManager", "ownerType", "ownerManager")}

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
// the value's other owner before this put, -1 if none
long oldOwnerID = -1;
<#if otherSideName?has_content && !otherSideIsList>
if (scope.contains(NotificationScope.LOCAL)) {
  // a single owner: the value leaves its old owner's map, locally (see _internal_put${name?cap_first}Id)
  ${tc.includeArgs("umlp.methods.gemmap.OtherSideMove", ["o.getGemId()", "this.getGemId()", name, otherSideName,
      targetName, targetManager, ownerType, ownerManager, "NotificationScope.LOCAL", "oldOwnerID"])}
}
</#if>
this._${name}._internal_putValue(o);
check${name?cap_first}();
<#if otherSideName?has_content>
if (scope.contains(NotificationScope.LOCAL)) {
  // the other side of o refers to this owner, a replaced value's no longer
  if (oldID != null && ${targetManager}.isLoaded${targetName}(oldID)) {
    ${targetName} replaced = ${targetManager}.get${targetName}Sure(oldID);
    ${tc.includeArgs("umlp.methods.gemmap.OtherSideRelease", ["replaced", "this.getGemId()", otherSideName, otherSideIsList])}
  }
  ${tc.includeArgs("umlp.methods.gemmap.OtherSideRefer", ["o", "this.getGemId()", otherSideName, otherSideIsList])}
}
</#if>
// a replaced value leaves with the put: every participant replaces it itself (see _internal_put${name?cap_first}Id),
// so its removal is not sent as a command of its own and the server sees the replacement
NotificationScope removeScope = scope.contains(NotificationScope.LOCAL) ? NotificationScope.LOCAL : scope;
for (${sourceName}Observer obs : ${sourceName?uncap_first}Observers) {
  if (oldID != null) {
    obs.maybeNotifyRemove${name?cap_first}((${sourceName}) this, oldID, -1, removeScope);
  }
  obs.maybeNotifyPut${name?cap_first}((${sourceName}) this, key, o.getGemId(), oldOwnerID, scope);
}
