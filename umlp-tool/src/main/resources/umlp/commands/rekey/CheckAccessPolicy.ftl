<#-- (c) https://github.com/MontiCore/monticore -->
<#-- only the server sends rekeying (it derives keys from the values itself), so the server (participant 0) never
     accepts it from a client: otherwise a client could move keys and the server would send that on -->
${tc.signature()}
${cd4c.method("public boolean checkAccessPolicy()")}
return CommandManager.getParticipantId() != 0;
