<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "sourceName", "hasMirror")}

${cd4c.method("public boolean _internal_remove${name?cap_first}Id(long gemID, NotificationScope scope)")}
if (!this._${name}._internal_removeValueID(gemID)) {
  return false;
}
check${name?cap_first}();
<#if hasMirror>
_internal_mirrorRemove${name?cap_first}(gemID);
</#if>
for (${sourceName}Observer obs : ${sourceName?uncap_first}Observers) {
  obs.maybeNotifyRemove${name?cap_first}((${sourceName}) this, gemID, -1, scope);
}
return true;
