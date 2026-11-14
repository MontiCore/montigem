<#-- (c) https://github.com/MontiCore/monticore -->
<#-- remove(key) of the map: removes the value stored for the key, see _internal_removeValueIDAndSync -->
${tc.signature("keyType", "isEntry")}
<#if isEntry>
${cd4c.method("protected void _internal_removeAndSync(Long keyGemID)")}
Long gemID = getID(keyGemID);
<#else>
${cd4c.method("protected void _internal_removeAndSync(${keyType} key)")}
Long gemID = getID(key);
</#if>
if (gemID != null) {
  _internal_removeValueIDAndSync(gemID);
}
