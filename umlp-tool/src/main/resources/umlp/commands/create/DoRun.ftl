<#-- (c) https://github.com/MontiCore/monticore -->
<#-- mirroredMaps: the roles of the object's GemMaps of bidirectional qualified associations (their values refer to
     the object, GenService#getMirroredMapRoles); singleOwnerMaps: those with a single owner, whose values leave their
     old owner's map (GenService#getSingleOwnerMapRoles) -->
${tc.signature("classname", "managerName", "packagename", "hasOCL", "mirroredMaps", "singleOwnerMaps")}
${cd4c.method("public StatusResult doAction()")}

${managerName}.addWithScope${classname}(${classname?uncap_first}, NotificationScope.LOCAL);
<#if singleOwnerMaps?has_content>
// a single owner: the server takes the values put while the object was a builder out of their old owners' maps (with
// ALL, for the database and the old owners' subscribers); the participant that built it moved what it had loaded
if (CommandManager.getParticipantId() == 0) {
  <#list singleOwnerMaps as role>
  for (long valueID : ${classname?uncap_first}._internal_get${role?cap_first}GemMap().getValueIDs()) {
    ${classname?uncap_first}._internal_moveFromOldOwner${role?cap_first}(valueID);
  }
  </#list>
}
</#if>
<#list mirroredMaps as role>
// the values loaded here refer to the new object (their mirror)
for (long valueID : ${classname?uncap_first}._internal_get${role?cap_first}GemMap().getValueIDs()) {
  ${classname?uncap_first}._internal_mirrorPut${role?cap_first}(valueID);
}
</#list>
return StatusResult.ok(getId());
