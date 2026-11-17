<#-- (c) https://github.com/MontiCore/monticore -->
<#-- writes the entry, see dao/mapattrs -->
${tc.signature("cdName", "name", "classDefiningAttrName", "className", "keyType", "valueType")}
<#assign attrName = classDefiningAttrName?uncap_first/>
${cd4c.method("public void notifyPut${name?cap_first}(${classDefiningAttrName} ${attrName}, ${keyType} key, ${valueType} value, ${valueType} oldValue)")}
${cdName}DAOFactory.get${className?cap_first}DAO().put${name?cap_first}(
    ${attrName}.getGemId(), key, value, ${attrName}.get${name?cap_first}Map());
