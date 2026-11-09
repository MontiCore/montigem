<#-- (c) https://github.com/MontiCore/monticore -->
<#-- included code (tc.includeArgs): a bidirectional qualified association with a single owner ([1]/[0..1] owner side)
     keeps a value in one owner's map, so putting it into another owner's map takes it out of its old owner's map,
     which the value's other side names. The server (participant 0) loads the value and the old owner if needed, the
     other participants move only what they loaded.
     valueId: an expression for the value's gemId; ownerId: an expression for the new owner's gemId; map: the role of
     the map; otherSide: the role of the other side; valueType/valueManager, ownerType/ownerManager: the types and their
     managers; removeScope: an expression for the scope of the removal from the old owner; oldOwnerVar: a long variable
     of the including code that gets the old owner's gemId if the value had another owner (left alone otherwise), or
     empty -->
${tc.signature("valueId", "ownerId", "map", "otherSide", "valueType", "valueManager", "ownerType", "ownerManager",
    "removeScope", "oldOwnerVar")}
{
  boolean server = CommandManager.getParticipantId() == 0;
  if (server || ${valueManager}.isLoaded${valueType}(${valueId})) {
    Optional<${valueType}> moved = ${valueManager}.${valueType?uncap_first}Opt(${valueId});
    long movedFrom = moved.isPresent() ? moved.get().get${otherSide?cap_first}Gem().getGemId() : -1;
    if (movedFrom != -1 && movedFrom != ${ownerId}) {
      <#if oldOwnerVar?has_content>
      ${oldOwnerVar} = movedFrom;
      </#if>
      if (server || ${ownerManager}.isLoaded${ownerType}(movedFrom)) {
        Optional<${ownerType}> oldOwner = ${ownerManager}.${ownerType?uncap_first}Opt(movedFrom);
        if (oldOwner.isPresent()) {
          oldOwner.get()._internal_remove${map?cap_first}Id(${valueId}, ${removeScope});
        }
      }
    }
  }
}
