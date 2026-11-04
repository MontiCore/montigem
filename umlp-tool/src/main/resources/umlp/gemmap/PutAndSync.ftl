<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts a value under a chosen key (type qualified association) through the owner once built: observers,
     synchronization and persistence -->
${tc.signature("keyType", "targetName", "name")}
${cd4c.method("protected void _internal_putAndSync(${keyType} key, ${targetName} value)")}
if (buildFinished) {
  getOwner().put${name?cap_first}(key, value);
}
else {
  _internal_putKeyValue(key, value);
}
