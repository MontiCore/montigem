<#-- (c) https://github.com/MontiCore/monticore -->
<#-- moves a value of the GemMap of an attribute qualified association from oldKey to newKey; sent by the server
     to the participants that loaded the owner, also those that did not load the value -->
${tc.signature("classname", "managerName", "name")}
${cd4c.method("public StatusResult doAction()")}
Optional<${classname}> ${classname?uncap_first}Opt = ${managerName}.${classname?uncap_first}Opt(gemId);
if (${classname?uncap_first}Opt.isEmpty()) {
  GeneralErrorCollector.reportSyncError();
  return StatusResult.error(getId());
}
// a participant that observes the value has already rekeyed it locally: then nothing changes
${classname?uncap_first}Opt.get()._internal_rekey${name?cap_first}(oldKey, newKey, ${name}GemId, NotificationScope.LOCAL);
return StatusResult.ok(getId());
