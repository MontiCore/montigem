<#-- (c) https://github.com/MontiCore/monticore -->
<#-- map: if this list is the other side of the GemMap of an attribute qualified association, the map's role (see
     InternalAdd) -->
${tc.signature("name", "otherSideName", "targetName", "sourceName", "managerName", "map")}

${cd4c.method("public void _internal_remove${name?cap_first}(Set<Integer> idxs, NotificationScope scope)")}
Map<Integer, Long> idxToId = new HashMap<>();
for(int idx : idxs){
  idxToId.put(idx, _${name}.getID(idx));
}
<#if map?has_content>
if (scope.contains(NotificationScope.LOCAL)) {
  // the other side of a map: the owners remove this value from their maps, which update this list (MINIMAL)
  for (long ownerID : idxToId.values()) {
    ${managerName}.get${targetName}Sure(ownerID)._internal_remove${map?cap_first}Id(this.getGemId(), scope);
  }
  return;
}
</#if>

this._${name}._internal_remove(idxs);
check${name?cap_first}();

for(Map.Entry<Integer, Long> e : idxToId.entrySet()) {
<#if otherSideName?has_content>
  if (scope.contains(NotificationScope.LOCAL)) {
    if(${managerName}.isLoaded${targetName}(e.getValue())){
      <#if otherSideName?starts_with("set")>
        // Clear old other side
        ${managerName}.get${targetName}Sure(e.getValue()).${otherSideName}Gem(new Gem<>(), NotificationScope.MINIMAL);
      <#else>
        ${managerName}.get${targetName}Sure(e.getValue()).${otherSideName}((${sourceName}) this, NotificationScope.MINIMAL);
      </#if>
    }
  }
</#if>
  ${sourceName?uncap_first}Observers.forEach(obs -> obs.maybeNotifyRemove${name?cap_first}((${sourceName}) this, e.getValue(), e.getKey(), scope));
}
