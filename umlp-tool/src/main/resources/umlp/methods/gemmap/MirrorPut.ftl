<#-- (c) https://github.com/MontiCore/monticore -->
<#-- bidirectional qualified association: the value with gemID is in this owner's map now, so its mirror (the value
     side, see GenService#isMapMirror) refers to this owner. Every participant does so itself for the values it loaded
     (MINIMAL: no commands, no rows; the map's command and row carry the pair); a value loaded later reads its owner.
     mirror: the role of the mirror; mirrorIsList: the owner side is [*]; targetName/targetManager: the value type -->
${tc.signature("name", "mirror", "mirrorIsList", "targetName", "targetManager")}

${cd4c.method("public void _internal_mirrorPut${name?cap_first}(long gemID)")}
if (!${targetManager}.isLoaded${targetName}(gemID)) {
  return;
}
${targetName} value = ${targetManager}.get${targetName}Sure(gemID);
<#if mirrorIsList>
if (value._internal_get${mirror?cap_first}GemList().indexOfId(this.getGemId()) == -1) {
  value._internal_add${mirror?cap_first}(value._internal_get${mirror?cap_first}GemList().getIDs().size(),
      Collections.singletonList(this.getGemId()), NotificationScope.MINIMAL);
}
<#else>
value._internal_set${mirror?cap_first}(this.getGemId(), NotificationScope.MINIMAL);
</#if>
