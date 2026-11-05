<#-- (c) https://github.com/MontiCore/monticore -->
<#-- map: if this list is the other side of the GemMap of an attribute qualified association, the map's role (see
     InternalAdd); managerName: the manager of the target type -->
${tc.signature("name", "targetName", "sourceName", "map", "managerName")}

${cd4c.method("public void _internal_set${name?cap_first}(int idx, long gemID, NotificationScope scope)")}
<#if map?has_content>
if (scope.contains(NotificationScope.LOCAL)) {
  // the other side of a map: the owner at idx removes this value from its map, the new one puts it into its map
  long oldOwnerID = this._${name}.getID(idx);
  if (oldOwnerID == gemID) {
    return;
  }
  ${managerName}.get${targetName}Sure(oldOwnerID)._internal_remove${map?cap_first}Id(this.getGemId(), scope);
  ${managerName}.get${targetName}Sure(gemID).put${map?cap_first}((${sourceName}) this, scope);
  return;
}
</#if>
if(idx < 0){
  throw new IllegalStateException("idx (=" + idx + ") must be 0 or higher");
}
if(idx > this._${name}.size()){
  throw new IllegalStateException("idx (=" + idx + ") must be at most ${name}.size (=" + this._${name}.size() + ")");
}
long oldValue;
if(this._${name}.size() > idx){
  oldValue = this._${name}.getID(idx);
} else {
  oldValue = -1;
}

if(oldValue == gemID){
  return;
}

this._${name}._internal_set(idx, gemID);
check${name?cap_first}();
${sourceName?uncap_first}Observers.forEach(obs ->
  obs.maybeNotifySet${name?cap_first}((${sourceName}) this, idx, oldValue, gemID, scope)
);
