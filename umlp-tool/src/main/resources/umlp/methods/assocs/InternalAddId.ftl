<#-- (c) https://github.com/MontiCore/monticore -->
<#-- map: if this list is the other side of the GemMap of an attribute qualified association, the map's role (see
     InternalAdd); managerName: the manager of the target type -->
${tc.signature("name", "targetName", "sourceName", "map", "managerName")}

${cd4c.method("public void INTERNAL_add${name?cap_first}Id(int idx, long gemID, NotificationScope scope)")}
<#if map?has_content>
if (scope.contains(NotificationScope.LOCAL)) {
  ${managerName}.get${targetName}Sure(gemID).put${map?cap_first}((${sourceName}) this, scope);
  return;
}
</#if>
_${name}._internal_add(idx, Arrays.asList(gemID));
${sourceName?uncap_first}Observers.forEach(obs -> obs.maybeNotifyAdd${name?cap_first}((${sourceName}) this, gemID, idx, scope));
