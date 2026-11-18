<#-- (c) https://github.com/MontiCore/monticore -->
<#-- map: if this is the other side of the GemMap of an attribute qualified association, the map's role -->
${tc.signature("name", "otherSideName", "targetName", "sourceName", "managerName", "map")}

${cd4c.method("public void _internal_set${name?cap_first}(${targetName} o, NotificationScope scope)")}
<#if map?has_content>
if (scope.contains(NotificationScope.LOCAL)) {
  // the other side of a map: the owner's map is the source of truth, o puts this value under the key derived from it
  // (taking it out of its old owner's map), null removes it from its owner's map; the map updates this side (MINIMAL),
  // like a bidirectional list updates its other side
  if (o != null) {
    o.put${map?cap_first}((${sourceName}) this, scope);
  }
  else if (this._${name}.getGemId() != -1) {
    Optional<${targetName}> owner = ${managerName}.${targetName?uncap_first}Opt(this._${name}.getGemId());
    if (owner.isPresent()) {
      owner.get()._internal_remove${map?cap_first}Id(this.getGemId(), scope);
    }
  }
  return;
}
</#if>

long oldValue = this._${name}.isPresent()?this._${name}.getGemId():-1;
if(oldValue == o.getGemId()){
  return;
}

this._${name} = new Gem<${targetName}>(o);

<#if otherSideName?has_content>
  if (scope.contains(NotificationScope.LOCAL)) {
    o.${otherSideName}((${sourceName}) this, NotificationScope.MINIMAL);
    if(o.getGemId() != oldValue && ${managerName}.isLoaded${targetName}(oldValue)){
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
  obs.maybeNotifySet${name?cap_first}((${sourceName}) this, oldValue, o.getGemId(),  scope);
}


