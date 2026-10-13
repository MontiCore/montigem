<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "scope")}

${cd4c.method("public void remove${name?cap_first}ByIdSure(long gemId)")}
remove${name?cap_first}ByIdSure(gemId, NotificationScope.${scope});
