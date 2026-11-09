<#-- (c) https://github.com/MontiCore/monticore -->
<#-- included code (tc.includeArgs) of a put command of a bidirectional association with a single owner: the server
     (participant 0) sets the owner the value leaves from the value's other side before it puts, like the sender did
     (which names none if it did not load the value), so the command reaches that owner's subscribers as well
     (getEffectedGemIds is read after doAction). name: the map role; otherSide: the role of the other side;
     targettype/targetTypeManager: the value type -->
${tc.signature("name", "otherSide", "targettype", "targetTypeManager")}
if (CommandManager.getParticipantId() == 0) {
  Optional<${targettype}> moved = ${targetTypeManager}.${targettype?uncap_first}Opt(${name}GemId);
  long ownerOfValue = moved.isPresent() ? moved.get().get${otherSide?cap_first}Gem().getGemId() : -1;
  oldOwnerGemId = ownerOfValue != gemId ? ownerOfValue : -1;
}
