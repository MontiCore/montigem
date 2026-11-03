<#-- (c) https://github.com/MontiCore/monticore -->
<#-- rekeys a value locally in the maps of the loaded owners of a map role (GemMapKeyRole); clients do not send
     rekeying, the server tells them (a participant that already rekeyed the value ignores it), see managerimpl/DoRekey -->
${tc.signature("name", "role", "keyType", "owners")}
${cd4c.method("protected void _doRekey${name}(${keyType} oldKey, ${keyType} newKey, long valueGemID)")}
<#list owners as r>
  <#if r.singleton>
if (${r.owner?uncap_first}Instance.isPresent()) {
  ${r.owner?uncap_first}Instance.get()._internal_rekey${role?cap_first}(oldKey, newKey, valueGemID, NotificationScope.ALL);
}
  <#else>
for (${r.owner} owner : known${r.owner}s.values()) {
  owner._internal_rekey${role?cap_first}(oldKey, newKey, valueGemID, NotificationScope.ALL);
}
  </#if>
</#list>
