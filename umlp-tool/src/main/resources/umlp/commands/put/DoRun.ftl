<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts a value into the GemMap of an attribute qualified association under its key -->
${tc.signature("classname", "managerName", "name", "targettype", "targetTypeManager", "isEntry", "keyType", "warnCode")}
${cd4c.method("public StatusResult doAction()")}
Optional<${classname}> ${classname?uncap_first}Opt = ${managerName}.${classname?uncap_first}Opt(gemId);
if (${classname?uncap_first}Opt.isEmpty()) {
  GeneralErrorCollector.reportSyncError();
  return StatusResult.error(getId());
}
// participant 0 is the server: it derives the key from the value. A key that does not match (the client's copy
// of the value was stale) is replaced before the server sends the command on to the other participants.
if (CommandManager.getParticipantId() == 0) {
  Optional<${targettype}> ${name}Opt = ${targetTypeManager}.${targettype?uncap_first}Opt(${name}GemId);
  if (${name}Opt.isEmpty()) {
    GeneralErrorCollector.reportSyncError();
    return StatusResult.error(getId());
  }
  ${keyType} derived = ${classname?uncap_first}Opt.get()._internal_get${name?cap_first}GemMap().<#if isEntry>deriveKeyID<#else>deriveKey</#if>(${name}Opt.get());
  if (key == null ? derived != null : !key.equals(derived)) {
    Log.warn("${warnCode} The key " + key + " sent with the put of the object with id " + ${name}GemId
        + " does not match its derived key " + derived + "; the derived key is used");
    key = derived;
  }
  ${classname?uncap_first}Opt.get().put${name?cap_first}(${name}Opt.get(), NotificationScope.LOCAL);
}
else {
  // the key comes from the server: the value need not be loaded
  ${classname?uncap_first}Opt.get()._internal_put${name?cap_first}Id(key, ${name}GemId, NotificationScope.LOCAL);
}
return StatusResult.ok(getId());
