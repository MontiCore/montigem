<#-- (c) https://github.com/MontiCore/monticore -->
<#-- writes the entry, see dao/mapattrs -->
${tc.signature("cdName", "name", "classDefiningAttrName", "className", "keyType", "valueType")}
<#-- o: a fixed name, the class name could clash with key, value, oldValue -->
<#assign attrName = "o"/>
${cd4c.method("public void notifyPut${name?cap_first}(${classDefiningAttrName} ${attrName}, ${keyType} key, ${valueType} value, ${valueType} oldValue)")}
${cdName}DAOFactory.get${className?cap_first}DAO().put${name?cap_first}(
    ${attrName}.getGemId(), key, value, ${attrName}.get${name?cap_first}Map());
