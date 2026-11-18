<#-- (c) https://github.com/MontiCore/monticore -->
<#-- bidirectionalMaps: the sides of the object's GemMaps of bidirectional qualified associations
     (GenService#getBidirectionalMaps). The object arrives deserialized, so build() left their other side alone: the
     other side of the values loaded here refers to the new object now, like for a bidirectional list; with a single
     owner the server takes the values out of their old owners' maps (with ALL, for the database and the old owners'
     subscribers: unlike a put command, the create command does not reach them); the participant that built the object
     did both itself -->
${tc.signature("classname", "managerName", "packagename", "hasOCL", "bidirectionalMaps")}
${cd4c.method("public StatusResult doAction()")}

${managerName}.addWithScope${classname}(${classname?uncap_first}, NotificationScope.LOCAL);
<#list bidirectionalMaps as assoc>
  <#assign otherSide=genService.getOtherSideOfMap(assoc).get()>
  <#assign valueType=assoc.getSymbol().getType().print()>
  <#assign valueManager=genService.getManagerOfType(assoc)>
  <#assign ownerType=otherSide.getSymbol().getType().print()>
  <#assign ownerManager=genService.getManagerOfType(otherSide)>
for (long valueID : ${classname?uncap_first}._internal_get${assoc.getName()?cap_first}GemMap().getValueIDs()) {
  <#if !genService.isList(otherSide)>
  if (CommandManager.getParticipantId() == 0) {
    ${tc.includeArgs("umlp.methods.gemmap.OtherSideMove", ["valueID", classname?uncap_first + ".getGemId()",
        assoc.getName(), otherSide.getName(), valueType, valueManager, ownerType, ownerManager, "NotificationScope.ALL",
        ""])}
  }
  </#if>
  if (${valueManager}.isLoaded${valueType}(valueID)) {
    ${valueType} value = ${valueManager}.get${valueType}Sure(valueID);
    ${tc.includeArgs("umlp.methods.gemmap.OtherSideRefer", ["value", classname?uncap_first + ".getGemId()",
        otherSide.getName(), genService.isList(otherSide)])}
  }
}
</#list>
return StatusResult.ok(getId());
