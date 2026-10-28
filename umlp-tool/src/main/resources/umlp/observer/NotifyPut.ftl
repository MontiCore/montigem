<#-- (c) https://github.com/MontiCore/monticore -->
<#-- a value was put into the GemMap of an attribute qualified association;
     key: the key (GemValueMap) or the gemId of the key object (GemEntryMap) -->
${tc.signature("name","className", "keyType")}
${cd4c.method("void notifyPut${name?cap_first}(${className} ${className?uncap_first}, ${keyType} key, long valueGemId)")}
