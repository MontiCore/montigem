<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "className", "keyType", "valueType")}

${cd4c.method("void maybeNotifyRemove${name?cap_first}(${className} o, ${keyType} key, ${valueType} oldValue, NotificationScope scope)")}

if(scope.contains(this.getScope())) {
    this.notifyRemove${name?cap_first}(o, key, oldValue);
}
