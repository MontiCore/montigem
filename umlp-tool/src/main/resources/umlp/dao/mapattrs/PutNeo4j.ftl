<#-- (c) https://github.com/MontiCore/monticore -->
<#-- writes the whole map after the put, see WriteNeo4j -->
${tc.signature("name", "keyType", "valueType")}
${cd4c.method("public void put${name?cap_first}(long gemId, ${keyType} key, ${valueType} value, Map<${keyType}, ${valueType}> map)")}
write${name?cap_first}(gemId, map);
