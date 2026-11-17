<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the entries of a map attribute with simple values, in insertion order -->
${tc.signature("name", "keyType", "valueType")}
${cd4c.method("public abstract Map<${keyType}, ${valueType}> select${name?cap_first}(long gemId)")}
