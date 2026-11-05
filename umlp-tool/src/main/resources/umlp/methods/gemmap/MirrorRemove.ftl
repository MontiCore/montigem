<#-- (c) https://github.com/MontiCore/monticore -->
<#-- bidirectional qualified association: the value with gemID left this owner's map, so its mirror (the value side,
     see GenService#isMapMirror) no longer refers to this owner; like MirrorPut, every participant does so itself for
     the values it loaded (MINIMAL) -->
${tc.signature("name", "mirror", "mirrorIsList", "targetName", "targetManager")}

${cd4c.method("protected void _internal_mirrorRemove${name?cap_first}(long gemID)")}
if (!${targetManager}.isLoaded${targetName}(gemID)) {
  return;
}
${targetName} value = ${targetManager}.get${targetName}Sure(gemID);
<#if mirrorIsList>
int idx = value._internal_get${mirror?cap_first}GemList().indexOfId(this.getGemId());
if (idx != -1) {
  value._internal_remove${mirror?cap_first}(Collections.singleton(idx), NotificationScope.MINIMAL);
}
<#else>
if (value.get${mirror?cap_first}Gem().getGemId() == this.getGemId()) {
  value._internal_set${mirror?cap_first}(-1L, NotificationScope.MINIMAL);
}
</#if>
