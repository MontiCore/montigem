<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts the value with gemID under key by id, without loading the value or the key object, replacing the value stored
     for key, like java.util.Map#put; the value is stored under one key only, so an entry it has under another key
     moves to key (see _internal_putValueID; the put tells the other participants, which move it as well).
     Used for the puts sent by the server (command _put) and, like GemList's _internal_add<Role>, as the owner put of a
     type qualified association (chosen key): put<Role>(key, o) and a put through the map (PutAndSync) delegate to it.
     keyType: the stored key type (the key, or the gemId of the key object for GemEntryMaps);
     chosen: the key is chosen on put (type qualified association), so it must be present -->
${tc.signature("name", "sourceName", "keyType", "isEntry", "chosen")}

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
this._${name}._internal_putValueID(key, gemID);
check${name?cap_first}();
// a replaced value leaves with the put: every participant replaces it itself, so its removal is not sent as a command
// of its own and the server sees the replacement
NotificationScope removeScope = scope.contains(NotificationScope.LOCAL) ? NotificationScope.LOCAL : scope;
for (${sourceName}Observer obs : ${sourceName?uncap_first}Observers) {
  if (oldID != null) {
    obs.maybeNotifyRemove${name?cap_first}((${sourceName}) this, oldID, -1, removeScope);
  }
  obs.maybeNotifyPut${name?cap_first}((${sourceName}) this, key, gemID, scope);
}
