<#-- (c) https://github.com/MontiCore/monticore -->
<#-- isMap: removes from a GemMap; the entry may be gone already (e.g. a participant that moved the value of a single
     owner itself), so its absence is no error.
     otherSideName, otherSideIsList, valueType, valueManager: the other side of a bidirectional association (empty
     otherwise): the command reaches the subscribers of the value as well, a participant that did not load the owner
     only releases the loaded value's other side and loads nothing (like Remove_DoRun of a bidirectional association) -->
${tc.signature( "attributename","classname","managerName", "isGem", "targetname", "hasOCL", "isMap", "otherSideName",
    "otherSideIsList", "valueType", "valueManager")}
${cd4c.method("public StatusResult doAction()")}

<#if otherSideName?has_content>
if (CommandManager.getParticipantId() != 0 && !${managerName}.isLoaded${classname}(gemId)) {
  if (${valueManager}.isLoaded${valueType}(${targetname}GemId)) {
    ${valueType} removed = ${valueManager}.get${valueType}Sure(${targetname}GemId);
    ${tc.includeArgs("umlp.methods.gemmap.OtherSideRelease", ["removed", "gemId", otherSideName, otherSideIsList])}
  }
  return StatusResult.ok(getId());
}
</#if>
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
