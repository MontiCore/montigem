<#-- (c) https://github.com/MontiCore/monticore -->
<#-- rekeys a value in the maps of the loaded owners of a map role (GemMapKeyRole), like _doDelete removes it:
     through the owners with ALL, so that the server tells every participant that loaded an owner. It is a no-op for
     an owner that does not hold the value under oldKey. An owner that is not loaded derives the key when its map is
     loaded. -->
${tc.signature("name", "role", "keyType", "owners")}
${cd4c.method("protected void _doRekey${name}(${keyType} oldKey, ${keyType} newKey, long valueGemID)")}
<#list owners as r>
  <#if r.singleton>
if (${r.owner?uncap_first}Instance != null) {
  ${r.owner?uncap_first}Instance._internal_rekey${role?cap_first}(oldKey, newKey, valueGemID, NotificationScope.ALL);
}
  <#else>
// a cache entry the garbage collector has cleared is null
for (${r.owner} owner : new java.util.ArrayList<>(known${r.owner}s.values())) {
  if (owner != null) {
    owner._internal_rekey${role?cap_first}(oldKey, newKey, valueGemID, NotificationScope.ALL);
  }
}
  </#if>
</#list>
