<#-- (c) https://github.com/MontiCore/monticore -->
<#-- map: the owner's map after the remove -->
${tc.signature("name", "keyType", "valueType")}
${cd4c.method("public abstract void remove${name?cap_first}(long gemId, ${keyType} key, Map<${keyType}, ${valueType}> map)")}
