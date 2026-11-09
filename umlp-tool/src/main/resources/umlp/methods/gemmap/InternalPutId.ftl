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
// the value's other owner before this put, -1 if none (or unknown here: the value is not loaded)
long oldOwnerID = -1;
<#if otherSideName?has_content && !otherSideIsList>
if (scope.contains(NotificationScope.LOCAL)) {
  // a single owner: the value leaves its old owner's map. Locally: the put notification names the old owner, so the
  // put command reaches its subscribers, which take the value out themselves (like the replaced value below)
  ${tc.includeArgs("umlp.methods.gemmap.OtherSideMove", ["gemID", "this.getGemId()", name, otherSideName, targetName,
      targetManager, ownerType, ownerManager, "NotificationScope.LOCAL", "oldOwnerID"])}
}
</#if>
this._${name}._internal_putValueID(key, gemID);
check${name?cap_first}();
<#if otherSideName?has_content>
if (scope.contains(NotificationScope.LOCAL)) {
  // the other side of the values loaded here refers to this owner, a replaced value's no longer
  if (oldID != null && ${targetManager}.isLoaded${targetName}(oldID)) {
    ${targetName} replaced = ${targetManager}.get${targetName}Sure(oldID);
    ${tc.includeArgs("umlp.methods.gemmap.OtherSideRelease", ["replaced", "this.getGemId()", otherSideName, otherSideIsList])}
  }
  if (${targetManager}.isLoaded${targetName}(gemID)) {
    ${targetName} value = ${targetManager}.get${targetName}Sure(gemID);
    ${tc.includeArgs("umlp.methods.gemmap.OtherSideRefer", ["value", "this.getGemId()", otherSideName, otherSideIsList])}
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
  obs.maybeNotifyPut${name?cap_first}((${sourceName}) this, key, gemID, oldID != null ? oldID : -1, oldOwnerID, scope);
}
