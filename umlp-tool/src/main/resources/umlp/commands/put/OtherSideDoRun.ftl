<#-- (c) https://github.com/MontiCore/monticore -->
<#-- included code (tc.includeArgs) of a put command of a bidirectional association on a participant other than the
     server: the command reaches the subscribers of the owner, the value and (single owner) the old owner, so it changes
     what is loaded here and loads nothing, like the commands of a bidirectional association (Add_DoRun): the old owner
     loses the value, the owner puts it (which updates the value's other side if loaded), otherwise the loaded value's
     other side refers to the owner.
     classname/managerName: the owner type; name: the map role; otherSide: the role of the other side;
     otherSideIsList: [*] owner side (no old owner); targettype/targetTypeManager: the value type -->
${tc.signature("classname", "managerName", "name", "otherSide", "otherSideIsList", "targettype", "targetTypeManager")}
if (CommandManager.getParticipantId() != 0) {
<#if !otherSideIsList>
  if (oldOwnerGemId != -1 && oldOwnerGemId != gemId && ${managerName}.isLoaded${classname}(oldOwnerGemId)) {
    ${managerName}.get${classname}Sure(oldOwnerGemId)._internal_remove${name?cap_first}Id(${name}GemId, NotificationScope.LOCAL);
  }
</#if>
  if (${managerName}.isLoaded${classname}(gemId)) {
    // the key comes from the server: the value need not be loaded
    ${managerName}.get${classname}Sure(gemId)._internal_put${name?cap_first}Id(key, ${name}GemId, NotificationScope.LOCAL);
  }
  else if (${targetTypeManager}.isLoaded${targettype}(${name}GemId)) {
    ${targettype} value = ${targetTypeManager}.get${targettype}Sure(${name}GemId);
    ${tc.includeArgs("umlp.methods.gemmap.OtherSideRefer", ["value", "gemId", otherSide, otherSideIsList])}
  }
  return StatusResult.ok(getId());
}
