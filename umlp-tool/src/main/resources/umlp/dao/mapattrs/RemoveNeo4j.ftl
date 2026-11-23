<#-- (c) https://github.com/MontiCore/monticore -->
<#-- writes the whole map after the remove, see WriteNeo4j -->
${tc.signature("name", "keyType", "valueType")}
${cd4c.method("public void remove${name?cap_first}(long gemId, ${keyType} key, Map<${keyType}, ${valueType}> map)")}
write${name?cap_first}(gemId, map);
