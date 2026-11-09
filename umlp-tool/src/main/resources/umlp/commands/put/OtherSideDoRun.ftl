<#-- (c) https://github.com/MontiCore/monticore -->
<#-- included code (tc.includeArgs) of a put command of a bidirectional association on a participant other than the
     server: the command reaches the subscribers of the owner, the value, the replaced value and (single owner) the old
     owner, so it changes what is loaded here and loads nothing, like the commands of a bidirectional association
     (Add_DoRun): the old owner loses the value, the owner puts it (which updates the other side of the loaded values),
     otherwise the loaded replaced value's other side releases the owner and the loaded value's refers to it.
     classname/managerName: the owner type; name: the map role; otherSide: the role of the other side;
     otherSideIsList: [*] owner side (no old owner); targettype/targetTypeManager: the value type.
     TODO: the replaced value (oldValueGemId) is the one the sender's map held under the key; the server does not
     correct it. It differs from the value the server replaces if the sender's map was stale: concurrent puts under
     the same key, or an [[attr]] key the server corrected (0xA7306/0xA7316). Then the other side of the wrongly named
     value is released and the replaced one stays stale on the participants that loaded only the values. Fix with the
     handling of concurrent commands (known bugs #15/#16), e.g. the server sets oldValueGemId from its map before it
     puts, like the old owner (OldOwner.ftl). -->
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
  else {
    if (oldValueGemId != -1 && ${targetTypeManager}.isLoaded${targettype}(oldValueGemId)) {
      ${targettype} replaced = ${targetTypeManager}.get${targettype}Sure(oldValueGemId);
      ${tc.includeArgs("umlp.methods.gemmap.OtherSideRelease", ["replaced", "gemId", otherSide, otherSideIsList])}
    }
    if (${targetTypeManager}.isLoaded${targettype}(${name}GemId)) {
      ${targettype} value = ${targetTypeManager}.get${targettype}Sure(${name}GemId);
      ${tc.includeArgs("umlp.methods.gemmap.OtherSideRefer", ["value", "gemId", otherSide, otherSideIsList])}
    }
  }
  return StatusResult.ok(getId());
}
