<#-- (c) https://github.com/MontiCore/monticore -->
<#-- rekeys a value of a map role (see GemMapKeyRole) in the maps of the loaded owners; called by the key observers
     when the qualifying attribute of the value changes. keyType: the stored key type (long for GemEntryMaps) -->
${tc.signature("name", "keyType", "delegateFlag")}
<#if delegateFlag>
  ${cd4c.method("public static void rekey${name}(${keyType} oldKey, ${keyType} newKey, long valueGemID)")}

  getSingleton()._doRekey${name}(oldKey, newKey, valueGemID);
<#else>
  ${cd4c.method("protected abstract void _doRekey${name}(${keyType} oldKey, ${keyType} newKey, long valueGemID)")}
</#if>
