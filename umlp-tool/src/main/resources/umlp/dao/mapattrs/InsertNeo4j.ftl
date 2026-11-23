<#-- (c) https://github.com/MontiCore/monticore -->
<#-- writes all entries of a new object, see WriteNeo4j -->
${tc.signature("name", "keyType", "valueType")}
${cd4c.method("public void insert${name?cap_first}(long gemId, Map<${keyType}, ${valueType}> map)")}
write${name?cap_first}(gemId, map);
