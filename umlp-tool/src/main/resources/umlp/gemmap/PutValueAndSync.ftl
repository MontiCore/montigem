<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts through the owner once built: observers, synchronization and persistence -->
${tc.signature("targetName", "name")}
${cd4c.method("protected void _internal_putValueAndSync(${targetName} value)")}
if (buildFinished) {
  getOwner().put${name?cap_first}(value);
}
else {
  _internal_putValue(value);
}
