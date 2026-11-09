<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts a value into the GemMap of a type qualified association under the key sent: the key is chosen on put, so
     there is nothing to derive, and every participant puts by id (_internal_put<Role>Id, which checks the key). The
     server has checked that the value and the key object exist (CheckContract), the other participants load neither.
     keyType: the key type, a domain class for GemEntryMaps (the command carries the gemId of the key object);
     otherSide: the role of the other side of a bidirectional association (empty otherwise), see OtherSideDoRun -->
${tc.signature("classname", "managerName", "name", "targettype", "targetTypeManager", "isEntry", "keyType", "keyTypeManager",
    "otherSide", "otherSideIsList")}
${cd4c.method("public StatusResult doAction()")}
<#if otherSide?has_content>
${tc.includeArgs("umlp.commands.put.OtherSideDoRun", [classname, managerName, name, otherSide, otherSideIsList, targettype,
    targetTypeManager])}
  <#if !otherSideIsList>
${tc.includeArgs("umlp.commands.put.OldOwner", [name, otherSide, targettype, targetTypeManager])}
  </#if>
</#if>
Optional<${classname}> ${classname?uncap_first}Opt = ${managerName}.${classname?uncap_first}Opt(gemId);
if (${classname?uncap_first}Opt.isEmpty()) {
  GeneralErrorCollector.reportSyncError();
  return StatusResult.error(getId());
}
${classname?uncap_first}Opt.get()._internal_put${name?cap_first}Id(key, ${name}GemId, NotificationScope.LOCAL);
return StatusResult.ok(getId());
