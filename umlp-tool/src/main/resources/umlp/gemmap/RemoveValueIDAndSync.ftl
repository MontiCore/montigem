<#-- (c) https://github.com/MontiCore/monticore -->
<#-- removes through the owner once built: observers, synchronization and persistence -->
${tc.signature("name")}
${cd4c.method("protected void _internal_removeValueIDAndSync(long gemID)")}
if (buildFinished) {
  getOwner().remove${name?cap_first}ByIdSure(gemID);
}
else {
  _internal_removeValueID(gemID);
}
