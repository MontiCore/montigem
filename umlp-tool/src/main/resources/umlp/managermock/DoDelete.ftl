<#-- (c) https://github.com/MontiCore/monticore -->
<#-- mapRoles: the map roles of loaded owners that may hold the deleted object (GemMapOwnerRole);
     keyRoles: the map roles of loaded owners that may use the deleted object as a key (GemMapOwnerRole#keyedBy) -->
${tc.signature("name", "managerName", "mapRoles", "keyRoles")}
${cd4c.method("protected boolean _doDelete${name}(Long gemId, NotificationScope scope)")}
  if (!${name?uncap_first}s.containsKey(gemId)) {
    return false;
  }
<#if mapRoles?has_content>
  // the deleted object leaves the maps of the loaded owners that hold it, through the owners: with ALL, whatever the
  // scope of the deletion, every participant that loaded an owner removes the entry
</#if>
<#list mapRoles as r>
  <#if r.singleton>
  if (${r.owner?uncap_first}Instance.isPresent() && ${r.owner?uncap_first}Instance.get()._internal_get${r.role?cap_first}GemMap().containsValueID(gemId)) {
    ${r.owner?uncap_first}Instance.get().remove${r.role?cap_first}ByIdSure(gemId, NotificationScope.ALL);
  }
  <#else>
  for (${r.owner} owner : new java.util.ArrayList<>(${r.owner?uncap_first}s.values())) {
    if (owner._internal_get${r.role?cap_first}GemMap().containsValueID(gemId)) {
      owner.remove${r.role?cap_first}ByIdSure(gemId, NotificationScope.ALL);
    }
  }
  </#if>
</#list>
<#if keyRoles?has_content>
  // the entries whose key is the deleted object leave the maps of the loaded owners the same way, by their values
</#if>
<#list keyRoles as r>
  <#if r.singleton>
  if (${r.owner?uncap_first}Instance.isPresent()) {
    Long ${r.role}ValueID = ${r.owner?uncap_first}Instance.get()._internal_get${r.role?cap_first}GemMap().getID(gemId);
    if (${r.role}ValueID != null) {
      ${r.owner?uncap_first}Instance.get().remove${r.role?cap_first}ByIdSure(${r.role}ValueID, NotificationScope.ALL);
    }
  }
  <#else>
  for (${r.owner} owner : new java.util.ArrayList<>(${r.owner?uncap_first}s.values())) {
    Long valueID = owner._internal_get${r.role?cap_first}GemMap().getID(gemId);
    if (valueID != null) {
      owner.remove${r.role?cap_first}ByIdSure(valueID, NotificationScope.ALL);
    }
  }
  </#if>
</#list>
  ${name?uncap_first}GemList.remove(${name?uncap_first}s.get(gemId));
  ${name?uncap_first}s.remove(gemId);
  ${managerName?uncap_first}Observers.forEach(obs -> obs.maybeNotify${name}Deleted(gemId, scope));
  return true;
