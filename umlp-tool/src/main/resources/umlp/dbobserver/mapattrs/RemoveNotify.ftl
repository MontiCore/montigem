<#-- (c) https://github.com/MontiCore/monticore -->
<#-- deletes the entry, see dao/mapattrs -->
${tc.signature("cdName", "name", "classDefiningAttrName", "className", "keyType", "valueType")}
<#assign attrName = classDefiningAttrName?uncap_first/>
${cd4c.method("public void notifyRemove${name?cap_first}(${classDefiningAttrName} ${attrName}, ${keyType} key, ${valueType} oldValue)")}
${cdName}DAOFactory.get${className?cap_first}DAO().remove${name?cap_first}(
    ${attrName}.getGemId(), key, ${attrName}.get${name?cap_first}Map());
