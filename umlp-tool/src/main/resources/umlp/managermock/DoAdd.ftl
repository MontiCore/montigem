<#-- (c) https://github.com/MontiCore/monticore -->
<#-- keyObservers: the key observers of the map roles that may hold objects of the class (GemMapKeyRole) -->
${tc.signature("name", "managerName", "assocs", "keyObservers")}
${cd4c.method("protected void _doAddWithScope${name}(${name} o, NotificationScope scope)")}

${name?uncap_first}s.put(o.getGemId(), o);
${name?uncap_first}GemList.add(o);
if (!o.containsObserver(the${name}CommandObserver)) {
  o.addObserver(the${name}CommandObserver);
}
<#list keyObservers as keyObserver>
if (!o.containsObserver(${keyObserver})) {
  o.addObserver(${keyObserver});
}
</#list>

<#list assocs as assoc>
  <#assign type = assoc.getSymbol().getType().print()>
  <#assign otherSide = assoc.getSymbol().getOtherSide().getAssocSide()>
  <#if genService.isList(assoc)>
  for (Long initialId : o._internal_get${assoc.getName()?cap_first}GemList().getIDs(0, o.size${assoc.getName()?cap_first}s())) {
    if(${managerName}Manager.isLoaded${type}(initialId)){
      ${type} other = ${managerName}Manager.get${type}Sure(initialId);
      <#if genService.isList(otherSide)>
        other._internal_add${otherSide.getName()?cap_first}(other.size${otherSide.getName()?cap_first}s(), Collections.singletonList(o.getGemId()), NotificationScope.MINIMAL);
      <#else>
        other._internal_set${otherSide.getName()?cap_first}(o, NotificationScope.MINIMAL);
      </#if>
      }
    }
  <#else>
    if(o.get${assoc.getName()?cap_first}Gem().isPresent()){
    ${type} other  = o.get${assoc.getName()?cap_first}();
    //${type} other = ${managerName}Manager.get${type}Sure(initialId);
    <#if genService.isList(otherSide)>
      other._internal_add${otherSide.getName()?cap_first}(other.size${otherSide.getName()?cap_first}s(), Collections.singletonList(o.getGemId()), NotificationScope.MINIMAL);
    <#else>
      other._internal_set${otherSide.getName()?cap_first}(o, NotificationScope.MINIMAL);
    </#if>
    }
  </#if>
</#list>

${managerName?uncap_first}Observers.forEach(obs -> obs.maybeNotify${name}Added(o, scope));
