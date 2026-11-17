<#-- (c) https://github.com/MontiCore/monticore -->
<#-- map: the owner's map after the put -->
${tc.signature("name", "keyType", "valueType")}
${cd4c.method("public abstract void put${name?cap_first}(long gemId, ${keyType} key, ${valueType} value, Map<${keyType}, ${valueType}> map)")}
