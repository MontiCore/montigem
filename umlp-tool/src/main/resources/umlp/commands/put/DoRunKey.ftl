<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts a value into the GemMap of a type qualified association under the key sent: the key is chosen on put, so
     there is nothing to derive. keyType: the key type, a domain class for GemEntryMaps (the command carries the gemId
     of the key object) -->
${tc.signature("classname", "managerName", "name", "targettype", "targetTypeManager", "isEntry", "keyType", "keyTypeManager")}
${cd4c.method("public StatusResult doAction()")}
Optional<${classname}> ${classname?uncap_first}Opt = ${managerName}.${classname?uncap_first}Opt(gemId);
if (${classname?uncap_first}Opt.isEmpty()) {
  GeneralErrorCollector.reportSyncError();
  return StatusResult.error(getId());
}
// participant 0 is the server: it puts through the owner (observers and persistence), its command sending observer
// sends the command on to the other participants
if (CommandManager.getParticipantId() == 0) {
  Optional<${targettype}> ${name}Opt = ${targetTypeManager}.${targettype?uncap_first}Opt(${name}GemId);
  if (${name}Opt.isEmpty()) {
    GeneralErrorCollector.reportSyncError();
    return StatusResult.error(getId());
  }
<#if isEntry>
  Optional<${keyType}> keyOpt = ${keyTypeManager}.${keyType?uncap_first}Opt(key);
  if (keyOpt.isEmpty()) {
    GeneralErrorCollector.reportSyncError();
    return StatusResult.error(getId());
  }
  ${classname?uncap_first}Opt.get().put${name?cap_first}(keyOpt.get(), ${name}Opt.get(), NotificationScope.LOCAL);
<#else>
  ${classname?uncap_first}Opt.get().put${name?cap_first}(key, ${name}Opt.get(), NotificationScope.LOCAL);
</#if>
}
else {
  // the key comes from the server: neither the value nor the key object need be loaded
  ${classname?uncap_first}Opt.get()._internal_put${name?cap_first}Id(key, ${name}GemId, NotificationScope.LOCAL);
}
return StatusResult.ok(getId());
