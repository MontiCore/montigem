<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the key of a value is the value of its qualifying attribute (GemEntryMap: the gemId of the referenced object) -->
${tc.signature("keyType", "targetName", "keyExpression", "isEntry")}
<#if isEntry>
${cd4c.method("public long deriveKeyID(${targetName} value)")}
<#else>
${cd4c.method("public ${keyType} deriveKey(${targetName} value)")}
</#if>
return ${keyExpression};
