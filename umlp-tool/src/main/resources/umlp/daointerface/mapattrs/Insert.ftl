<#-- (c) https://github.com/MontiCore/monticore -->
<#-- stores all entries of a new object, in insertion order -->
${tc.signature("name", "keyType", "valueType")}
${cd4c.method("public abstract void insert${name?cap_first}(long gemId, Map<${keyType}, ${valueType}> map)")}
