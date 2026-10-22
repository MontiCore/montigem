<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature( "attributename","classname","managerName", "isGem", "targetname", "targettype", "targetTypeManager", "hasOCL", "isMap")}
${cd4c.method("public StatusResult doAction()")}

Optional<${classname}> ${classname?uncap_first}Opt = ${managerName}.${classname?uncap_first}Opt(gemId);
if(${classname?uncap_first}Opt.isEmpty()){
  // TODO AHe: add GemId to reportSyncError or replace with <Class>ErrorCollector.reportPersonNotPresent(gemId)...
  GeneralErrorCollector.reportSyncError();
  return StatusResult.error(getId());
}
<#if isGem>
  Optional<${targettype}> ${targetname}Opt = ${targetTypeManager}.${targettype?uncap_first}Opt(${targetname}GemId);
  if (${targetname}Opt.isEmpty()) {
    // TODO AHe: add GemId to reportSyncError or replace with <Class>ErrorCollector.reportPersonNotPresent(gemId)...
    GeneralErrorCollector.reportSyncError();
    return StatusResult.error(getId());
  }
  <#if isMap>
  // the key is derived from the target, there is no index
  ${classname?uncap_first}Opt.get().put${targetname?cap_first}(${targetname}Opt.get(), NotificationScope.LOCAL);
  <#else>
  ${classname?uncap_first}Opt.get().add${targetname?cap_first}(indexInList, ${targetname}Opt.get(), NotificationScope.LOCAL);
  </#if>
<#else>
  ${classname?uncap_first}Opt.get().add${attributename?cap_first}(indexInList, ${attributename}, NotificationScope.LOCAL);
</#if>
return StatusResult.ok(getId());
