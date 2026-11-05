<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts the value with gemID under key by id, without loading the value or the key object, replacing the value stored
     for key, like java.util.Map#put; the value is stored under one key only, so an entry it has under another key
     moves to key (see _internal_putValueID; the put tells the other participants, which move it as well).
     Used for the puts sent by the server (command _put) and, like GemList's _internal_add<Role>, as the owner put of a
     type qualified association (chosen key): put<Role>(key, o) and a put through the map (PutAndSync) delegate to it.
     keyType: the stored key type (the key, or the gemId of the key object for GemEntryMaps);
     chosen: the key is chosen on put (type qualified association), so it must be present;
     otherSideName: for a bidirectional association the role of the other side (value side), like for a bidirectional
     list, empty otherwise; otherSideIsList: the owner side is [*] (otherwise a single owner, a put moves the value);
     targetName/targetManager: the value type; ownerType/ownerManager: the owner type the other side refers to -->
${tc.signature("name", "sourceName", "keyType", "isEntry", "chosen", "otherSideName", "otherSideIsList", "targetName",
    "targetManager", "ownerType", "ownerManager")}

${cd4c.method("public void _internal_put${name?cap_first}Id(${keyType} key, long gemID, NotificationScope scope)")}
<#if chosen>
if (key == null) {
  Log.error("0xA7323 The object with id " + gemID + " cannot be put under the key null");
  return;
}
</#if>
<#if !isEntry>
if (!this._${name}.isSafeKey(key)) {
  Log.error("0xA7326 The object with id " + gemID + " cannot be put under the key " + key
      + ", which a browser cannot represent exactly");
  return;
}
</#if>
Long oldID = this._${name}.getID(key);
if (oldID != null && oldID == gemID) {
  return;
}
<#if otherSideName?has_content && !otherSideIsList>
if (scope.contains(NotificationScope.LOCAL)) {
  // a single owner: the value leaves its old owner's map, which its other side names. The server loads the value and
  // the old owner if needed and removes with ALL: the database deletes the old pair and the old owner's subscribers
  // learn it (they do not receive this put); the other participants move what they loaded, locally
  boolean server = CommandManager.getParticipantId() == 0;
  if (server || ${targetManager}.isLoaded${targetName}(gemID)) {
    Optional<${targetName}> moved = ${targetManager}.${targetName?uncap_first}Opt(gemID);
    long oldOwnerID = moved.isPresent() ? moved.get().get${otherSideName?cap_first}Gem().getGemId() : -1;
    if (oldOwnerID != -1 && oldOwnerID != this.getGemId()
        && (server || ${ownerManager}.isLoaded${ownerType}(oldOwnerID))) {
      Optional<${ownerType}> oldOwner = ${ownerManager}.${ownerType?uncap_first}Opt(oldOwnerID);
      if (oldOwner.isPresent()) {
        oldOwner.get()._internal_remove${name?cap_first}Id(gemID, server ? NotificationScope.ALL : NotificationScope.LOCAL);
      }
    }
  }
}
</#if>
this._${name}._internal_putValueID(key, gemID);
check${name?cap_first}();
<#if otherSideName?has_content>
if (scope.contains(NotificationScope.LOCAL)) {
  // the other side of the values loaded here refers to this owner, a replaced value's no longer, like for a
  // bidirectional list (MINIMAL: the map's command and row carry the pair)
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
  if (${targetManager}.isLoaded${targetName}(gemID)) {
    ${targetName} value = ${targetManager}.get${targetName}Sure(gemID);
  <#if otherSideIsList>
    if (value._internal_get${otherSideName?cap_first}GemList().indexOfId(this.getGemId()) == -1) {
      value._internal_add${otherSideName?cap_first}(value._internal_get${otherSideName?cap_first}GemList().getIDs().size(),
          Collections.singletonList(this.getGemId()), NotificationScope.MINIMAL);
    }
  <#else>
    value._internal_set${otherSideName?cap_first}(this.getGemId(), NotificationScope.MINIMAL);
  </#if>
  }
}
</#if>
// a replaced value leaves with the put: every participant replaces it itself, so its removal is not sent as a command
// of its own and the server sees the replacement
NotificationScope removeScope = scope.contains(NotificationScope.LOCAL) ? NotificationScope.LOCAL : scope;
for (${sourceName}Observer obs : ${sourceName?uncap_first}Observers) {
  if (oldID != null) {
    obs.maybeNotifyRemove${name?cap_first}((${sourceName}) this, oldID, -1, removeScope);
  }
  obs.maybeNotifyPut${name?cap_first}((${sourceName}) this, key, gemID, scope);
}
