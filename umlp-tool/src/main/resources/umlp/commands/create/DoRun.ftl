<#-- (c) https://github.com/MontiCore/monticore -->
<#-- bidirectionalMaps: the sides of the object's GemMaps of bidirectional qualified associations
     (GenService#getBidirectionalMaps). The object arrives deserialized, so build() left their other side alone: the
     other side of the values loaded here refers to the new object now, like for a bidirectional list; with a single
     owner the server takes the values out of their old owners' maps (with ALL, for the database and the old owners'
     subscribers, see _internal_put<Role>Id); the participant that built the object did both itself -->
${tc.signature("classname", "managerName", "packagename", "hasOCL", "bidirectionalMaps")}
${cd4c.method("public StatusResult doAction()")}

${managerName}.addWithScope${classname}(${classname?uncap_first}, NotificationScope.LOCAL);
<#if bidirectionalMaps?has_content>
boolean server = CommandManager.getParticipantId() == 0;
</#if>
<#list bidirectionalMaps as assoc>
  <#assign otherSide=genService.getOtherSideOfMap(assoc).get()>
  <#assign valueType=assoc.getSymbol().getType().print()>
  <#assign valueManager=genService.getManagerOfType(assoc)>
  <#assign ownerType=otherSide.getSymbol().getType().print()>
  <#assign ownerManager=genService.getManagerOfType(otherSide)>
for (long valueID : ${classname?uncap_first}._internal_get${assoc.getName()?cap_first}GemMap().getValueIDs()) {
  if (!server && !${valueManager}.isLoaded${valueType}(valueID)) {
    continue;
  }
  Optional<${valueType}> value = ${valueManager}.${valueType?uncap_first}Opt(valueID);
  if (value.isEmpty()) {
    continue;
  }
  <#if genService.isList(otherSide)>
  if (value.get()._internal_get${otherSide.getName()?cap_first}GemList().indexOfId(${classname?uncap_first}.getGemId()) == -1) {
    value.get()._internal_add${otherSide.getName()?cap_first}(value.get()._internal_get${otherSide.getName()?cap_first}GemList().getIDs().size(),
        Collections.singletonList(${classname?uncap_first}.getGemId()), NotificationScope.MINIMAL);
  }
  <#else>
  long oldOwnerID = value.get().get${otherSide.getName()?cap_first}Gem().getGemId();
  if (server && oldOwnerID != -1 && oldOwnerID != ${classname?uncap_first}.getGemId()) {
    Optional<${ownerType}> oldOwner = ${ownerManager}.${ownerType?uncap_first}Opt(oldOwnerID);
    if (oldOwner.isPresent()) {
      oldOwner.get()._internal_remove${assoc.getName()?cap_first}Id(valueID, NotificationScope.ALL);
    }
  }
  value.get()._internal_set${otherSide.getName()?cap_first}(${classname?uncap_first}.getGemId(), NotificationScope.MINIMAL);
  </#if>
}
</#list>
return StatusResult.ok(getId());
