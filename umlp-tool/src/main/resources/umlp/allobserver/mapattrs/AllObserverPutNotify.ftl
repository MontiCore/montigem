<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "classDefiningAttrName", "keyType", "valueType")}
<#-- o: a fixed name, the class name could clash with key, value, oldValue -->
<#assign attrName = "o"/>
${cd4c.method("public void notifyPut${name?cap_first}(${classDefiningAttrName} ${attrName}, ${keyType} key, ${valueType} value, ${valueType} oldValue)")}
  this.consumer.accept(null);
