<#-- (c) https://github.com/MontiCore/monticore -->
<#-- bidirectional qualified association with a single owner ([1]/[0..1] owner side): a value is in one owner's map
     only, so a put into this owner's map moves it out of its old owner's map, which its mirror names.
     The server (participant 0) loads the value and the old owner if needed and removes with ALL: the database deletes
     the old pair and the old owner's subscribers learn the removal (they do not receive this owner's put). The other
     participants move only what they loaded, locally; the server's removal then finds nothing to do.
     Unlike a bidirectional list (an add leaves the element in the old owner's list), both sides stay consistent.
     mirror: the role of the mirror; targetName/targetManager: the value type; owner/ownerManager: the owner type -->
${tc.signature("name", "mirror", "targetName", "targetManager", "owner", "ownerManager")}

${cd4c.method("public void _internal_moveFromOldOwner${name?cap_first}(long gemID)")}
boolean server = CommandManager.getParticipantId() == 0;
if (!server && !${targetManager}.isLoaded${targetName}(gemID)) {
  return;
}
Optional<${targetName}> value = ${targetManager}.${targetName?uncap_first}Opt(gemID);
if (value.isEmpty()) {
  return;
}
long oldOwnerID = value.get().get${mirror?cap_first}Gem().getGemId();
if (oldOwnerID == -1 || oldOwnerID == this.getGemId()) {
  return;
}
if (!server && !${ownerManager}.isLoaded${owner}(oldOwnerID)) {
  return;
}
Optional<${owner}> oldOwner = ${ownerManager}.${owner?uncap_first}Opt(oldOwnerID);
if (oldOwner.isPresent()) {
  oldOwner.get()._internal_remove${name?cap_first}Id(gemID, server ? NotificationScope.ALL : NotificationScope.LOCAL);
}
