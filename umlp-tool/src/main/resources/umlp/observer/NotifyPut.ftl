<#-- (c) https://github.com/MontiCore/monticore -->
<#-- a value was put into the GemMap of a qualified association;
     key: the key (GemValueMap) or the gemId of the key object (GemEntryMap);
     oldOwnerGemId: for a bidirectional association with a single owner the owner the value left with this put (like
     the old value of a set), -1 if it had none or the participant did not know it (the value was not loaded) -->
${tc.signature("name","className", "keyType")}
${cd4c.method("void notifyPut${name?cap_first}(${className} ${className?uncap_first}, ${keyType} key, long valueGemId, long oldOwnerGemId)")}
