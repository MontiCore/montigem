<#-- (c) https://github.com/MontiCore/monticore -->
<#-- mapRoles: the map roles of loaded owners that may hold the deleted object (GemMapOwnerRole);
     keyRoles: the map roles of loaded owners that may use the deleted object as a key (GemMapOwnerRole#keyedBy);
     bidirectionalMaps: the roles of the deleted object's own GemMaps of bidirectional qualified associations
     (GenService#getBidirectionalMaps) -->
${tc.signature("cdName","name", "managerName", "mapRoles", "keyRoles", "bidirectionalMaps")}
${cd4c.method("protected boolean _doDelete${name}(Long gemId, NotificationScope scope)")}
  if(!(known${name}s.containsKey(gemId) || ${cdName}DAOFactory.get${name?cap_first}DAO().select${name}(gemId).isPresent())) {
    return false;
  }
${tc.includeArgs("umlp.managerimpl.RemoveBidirectionalMapEntries", [name, "_do" + name + "Opt(gemId)", bidirectionalMaps])}
<#if mapRoles?has_content>
  // the deleted object leaves the maps of the loaded owners that hold it, through the owners: with ALL, whatever the
  // scope of the deletion, the database and every participant that loaded an owner remove the entry
</#if>
<#list mapRoles as r>
  <#if r.singleton>
  if (${r.owner?uncap_first}Instance != null && ${r.owner?uncap_first}Instance._internal_get${r.role?cap_first}GemMap().containsValueID(gemId)) {
    ${r.owner?uncap_first}Instance.remove${r.role?cap_first}ByIdSure(gemId, NotificationScope.ALL);
  }
  <#else>
  // a cache entry the garbage collector has cleared is null
  for (${r.owner} owner : new java.util.ArrayList<>(known${r.owner}s.values())) {
    if (owner != null && owner._internal_get${r.role?cap_first}GemMap().containsValueID(gemId)) {
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
  if (${r.owner?uncap_first}Instance != null) {
    Long ${r.role}ValueID = ${r.owner?uncap_first}Instance._internal_get${r.role?cap_first}GemMap().getID(gemId);
    if (${r.role}ValueID != null) {
      ${r.owner?uncap_first}Instance.remove${r.role?cap_first}ByIdSure(${r.role}ValueID, NotificationScope.ALL);
    }
  }
  <#else>
  for (${r.owner} owner : new java.util.ArrayList<>(known${r.owner}s.values())) {
    Long valueID = owner == null ? null : owner._internal_get${r.role?cap_first}GemMap().getID(gemId);
    if (valueID != null) {
      owner.remove${r.role?cap_first}ByIdSure(valueID, NotificationScope.ALL);
    }
  }
  </#if>
</#list>
  known${name}s.remove(gemId);
  ${managerName?uncap_first}Observers.forEach(obs -> obs.maybeNotify${name}Deleted(gemId, scope));
  return true;
