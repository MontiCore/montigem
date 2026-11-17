<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts or removes the entry locally -->
${tc.signature("classname", "managerName", "name", "hasValue")}
${cd4c.method("public StatusResult doAction()")}
Optional<${classname}> ${classname?uncap_first}Opt = ${managerName}.${classname?uncap_first}Opt(gemId);
if (${classname?uncap_first}Opt.isEmpty()) {
  GeneralErrorCollector.reportSyncError();
  return StatusResult.error(getId());
}
<#if hasValue>
${classname?uncap_first}Opt.get().put${name?cap_first}(key, value, NotificationScope.LOCAL);
<#else>
${classname?uncap_first}Opt.get().remove${name?cap_first}(key, NotificationScope.LOCAL);
</#if>
return StatusResult.ok(getId());
