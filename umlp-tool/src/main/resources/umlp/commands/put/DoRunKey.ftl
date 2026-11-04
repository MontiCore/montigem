<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts a value into the GemMap of a type qualified association under the key sent: the key is chosen on put, so
     there is nothing to derive, and every participant puts by id (_internal_put<Role>Id, which checks the key).
     keyType: the key type, a domain class for GemEntryMaps (the command carries the gemId of the key object) -->
${tc.signature("classname", "managerName", "name", "targettype", "targetTypeManager", "isEntry", "keyType", "keyTypeManager")}
${cd4c.method("public StatusResult doAction()")}
Optional<${classname}> ${classname?uncap_first}Opt = ${managerName}.${classname?uncap_first}Opt(gemId);
if (${classname?uncap_first}Opt.isEmpty()) {
  GeneralErrorCollector.reportSyncError();
  return StatusResult.error(getId());
}
// participant 0 is the server: it checks that the value and the key object exist, like the add of a GemList checks
// its element; the other participants trust the server, they load neither
if (CommandManager.getParticipantId() == 0) {
  if (${targetTypeManager}.${targettype?uncap_first}Opt(${name}GemId).isEmpty()) {
    GeneralErrorCollector.reportSyncError();
    return StatusResult.error(getId());
  }
<#if isEntry>
  if (${keyTypeManager}.${keyType?uncap_first}Opt(key).isEmpty()) {
    GeneralErrorCollector.reportSyncError();
    return StatusResult.error(getId());
  }
</#if>
}
${classname?uncap_first}Opt.get()._internal_put${name?cap_first}Id(key, ${name}GemId, NotificationScope.LOCAL);
return StatusResult.ok(getId());
