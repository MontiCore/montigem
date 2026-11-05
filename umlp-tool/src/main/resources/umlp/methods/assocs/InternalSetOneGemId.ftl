<#-- (c) https://github.com/MontiCore/monticore -->
<#-- map: if this is the other side of the GemMap of an attribute qualified association, the map's role (see
     InternalSetOne) -->
${tc.signature("name", "otherSideName", "targetName", "sourceName", "managerName", "map")}

${cd4c.method("public void _internal_set${name?cap_first}(long gemID, NotificationScope scope)")}
<#if map?has_content>
if (scope.contains(NotificationScope.LOCAL)) {
  _internal_set${name?cap_first}(gemID == -1 ? null : ${managerName}.get${targetName}Sure(gemID), scope);
  return;
}
</#if>

long oldValue = this._${name}.isPresent()?this._${name}.getGemId():-1;
if(oldValue == gemID){
  return;
}

this._${name} = ${managerName}.create${targetName}Gem(gemID);

<#if otherSideName?has_content>
  if (scope.contains(NotificationScope.LOCAL)) {
    if(${managerName}.isLoaded${targetName}(gemID)){
      ${managerName}.get${targetName}Sure(gemID).${otherSideName}((${sourceName}) this, NotificationScope.MINIMAL);
    }
    if(${managerName}.isLoaded${targetName}(oldValue)){
      <#if otherSideName?starts_with("set")>
        // Clear old other side
        ${managerName}.get${targetName}Sure(oldValue).${otherSideName}Gem(new Gem<>(), NotificationScope.MINIMAL);
      <#else>
        <#assign removeOtherSide=otherSideName?replace("add", "remove")>
        ${managerName}.get${targetName}Sure(oldValue).${removeOtherSide}((${sourceName}) this, NotificationScope.MINIMAL);
      </#if>
    }
  }
</#if>

for(${sourceName}Observer obs : ${sourceName?uncap_first}Observers) {
  obs.maybeNotifySet${name?cap_first}((${sourceName}) this, oldValue, gemID,  scope);
}


