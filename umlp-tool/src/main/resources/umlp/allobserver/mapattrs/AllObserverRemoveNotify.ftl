<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "classDefiningAttrName", "keyType", "valueType")}
<#assign attrName = classDefiningAttrName?uncap_first/>
${cd4c.method("public void notifyRemove${name?cap_first}(${classDefiningAttrName} ${attrName}, ${keyType} key, ${valueType} oldValue)")}
  this.consumer.accept(null);
