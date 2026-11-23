<#-- (c) https://github.com/MontiCore/monticore -->
<#-- deletes the entry, see dao/mapattrs -->
${tc.signature("cdName", "name", "classDefiningAttrName", "className", "keyType", "valueType")}
<#-- o: a fixed name, the class name could clash with key, value, oldValue -->
<#assign attrName = "o"/>
${cd4c.method("public void notifyRemove${name?cap_first}(${classDefiningAttrName} ${attrName}, ${keyType} key, ${valueType} oldValue)")}
${cdName}DAOFactory.get${className?cap_first}DAO().remove${name?cap_first}(
    ${attrName}.getGemId(), key, ${attrName}.get${name?cap_first}Map());
