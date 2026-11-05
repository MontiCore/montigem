<#-- (c) https://github.com/MontiCore/monticore -->
<#-- removes the value with gemID from the map; returns false if it is not in it (no error: e.g. a participant that moved
     the value itself receives the server's removal as well). otherSideName, otherSideIsList, targetName, targetManager:
     the other side of a bidirectional association, see InternalPutId -->
${tc.signature("name", "sourceName", "otherSideName", "otherSideIsList", "targetName", "targetManager")}

${cd4c.method("public boolean _internal_remove${name?cap_first}Id(long gemID, NotificationScope scope)")}
if (!this._${name}._internal_removeValueID(gemID)) {
  return false;
}
check${name?cap_first}();
<#if otherSideName?has_content>
// the other side of the removed value no longer refers to this owner, like for a bidirectional list (MINIMAL)
if (scope.contains(NotificationScope.LOCAL) && ${targetManager}.isLoaded${targetName}(gemID)) {
  ${targetName} removed = ${targetManager}.get${targetName}Sure(gemID);
  <#if otherSideIsList>
  int idx = removed._internal_get${otherSideName?cap_first}GemList().indexOfId(this.getGemId());
  if (idx != -1) {
    removed._internal_remove${otherSideName?cap_first}(Collections.singleton(idx), NotificationScope.MINIMAL);
  }
  <#else>
  if (removed.get${otherSideName?cap_first}Gem().getGemId() == this.getGemId()) {
    removed._internal_set${otherSideName?cap_first}(-1L, NotificationScope.MINIMAL);
  }
  </#if>
}
</#if>
for (${sourceName}Observer obs : ${sourceName?uncap_first}Observers) {
  obs.maybeNotifyRemove${name?cap_first}((${sourceName}) this, gemID, -1, scope);
}
return true;
