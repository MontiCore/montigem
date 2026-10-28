<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name")}

${cd4c.method("public void remove${name?cap_first}ByIdSure(long gemId, NotificationScope scope)")}
if (!_internal_remove${name?cap_first}Id(gemId, scope)) {
  Log.error("Can not find gemId " + gemId + " in _${name}");
}
