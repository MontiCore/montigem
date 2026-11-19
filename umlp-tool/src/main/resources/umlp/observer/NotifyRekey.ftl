<#-- (c) https://github.com/MontiCore/monticore -->
<#-- a value of the GemMap of an attribute qualified association moved from oldKey to newKey,
     as its qualifying attribute changed; keys as in NotifyPut -->
${tc.signature("name","className", "keyType")}
${cd4c.method("void notifyRekey${name?cap_first}(${className} ${className?uncap_first}, ${keyType} oldKey, ${keyType} newKey, long valueGemId)")}
