<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "className", "keyType", "valueType")}

${cd4c.method("void maybeNotifyPut${name?cap_first}(${className} o, ${keyType} key, ${valueType} value, ${valueType} oldValue, NotificationScope scope)")}

if(scope.contains(this.getScope())) {
    this.notifyPut${name?cap_first}(o, key, value, oldValue);
}
