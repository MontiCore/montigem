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
<#if otherSideName?has_content && !otherSideIsList>
if (scope.contains(NotificationScope.LOCAL)) {
  // a single owner: the value leaves its old owner's map, which its other side names (see _internal_put${name?cap_first}Id)
  boolean server = CommandManager.getParticipantId() == 0;
  long oldOwnerID = o.get${otherSideName?cap_first}Gem().getGemId();
  if (oldOwnerID != -1 && oldOwnerID != this.getGemId()
      && (server || ${ownerManager}.isLoaded${ownerType}(oldOwnerID))) {
    Optional<${ownerType}> oldOwner = ${ownerManager}.${ownerType?uncap_first}Opt(oldOwnerID);
    if (oldOwner.isPresent()) {
      oldOwner.get()._internal_remove${name?cap_first}Id(o.getGemId(), server ? NotificationScope.ALL : NotificationScope.LOCAL);
    }
  }
}
</#if>
this._${name}._internal_putValue(o);
check${name?cap_first}();
<#if otherSideName?has_content>
if (scope.contains(NotificationScope.LOCAL)) {
  // the other side of o refers to this owner, a replaced value's no longer, like for a bidirectional list (MINIMAL: the
  // map's command and row carry the pair)
  if (oldID != null && ${targetManager}.isLoaded${targetName}(oldID)) {
    ${targetName} replaced = ${targetManager}.get${targetName}Sure(oldID);
  <#if otherSideIsList>
    int idx = replaced._internal_get${otherSideName?cap_first}GemList().indexOfId(this.getGemId());
    if (idx != -1) {
      replaced._internal_remove${otherSideName?cap_first}(Collections.singleton(idx), NotificationScope.MINIMAL);
    }
  <#else>
    if (replaced.get${otherSideName?cap_first}Gem().getGemId() == this.getGemId()) {
      replaced._internal_set${otherSideName?cap_first}(-1L, NotificationScope.MINIMAL);
    }
  </#if>
  }
  <#if otherSideIsList>
  if (o._internal_get${otherSideName?cap_first}GemList().indexOfId(this.getGemId()) == -1) {
    o._internal_add${otherSideName?cap_first}(o._internal_get${otherSideName?cap_first}GemList().getIDs().size(),
        Collections.singletonList(this.getGemId()), NotificationScope.MINIMAL);
  }
  <#else>
  o._internal_set${otherSideName?cap_first}(this.getGemId(), NotificationScope.MINIMAL);
  </#if>
}
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
