<#-- (c) https://github.com/MontiCore/monticore -->
<#-- isMap: removes from a GemMap; the entry may be gone already (a participant that moved the value of a single owner
     itself receives the server's removal from the old owner as well), so its absence is no error -->
${tc.signature( "attributename","classname","managerName", "isGem", "targetname", "hasOCL", "isMap")}
${cd4c.method("public StatusResult doAction()")}

Optional<${classname}> ${classname?uncap_first}Opt = ${managerName}.${classname?uncap_first}Opt(gemId);
if(${classname?uncap_first}Opt.isEmpty()){
  // TODO AHe: add GemId to reportSyncError or replace with <Class>ErrorCollector.reportPersonNotPresent(gemId)...
  GeneralErrorCollector.reportSyncError();
  return StatusResult.error(getId());
}
<#if isMap>
  ${classname?uncap_first}Opt.get()._internal_remove${targetname?cap_first}Id(${targetname}GemId, NotificationScope.LOCAL);
<#elseif isGem>
  ${classname?uncap_first}Opt.get().remove${targetname?cap_first}ByIdSure(${targetname}GemId, NotificationScope.LOCAL);
<#else>
  ${classname?uncap_first}Opt.get().remove${attributename?cap_first}(indexInList, NotificationScope.LOCAL);
</#if>
return StatusResult.ok(getId());
