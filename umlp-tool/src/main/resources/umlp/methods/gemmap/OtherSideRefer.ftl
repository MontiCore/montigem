<#-- (c) https://github.com/MontiCore/monticore -->
<#-- included code (tc.includeArgs): the other side of a loaded value refers to an owner (bidirectional qualified
     association, see GenService#isOtherSideOfMap), like a bidirectional list sets its other side (MINIMAL: the map's
     command and row carry the pair). value: an expression for the value object; ownerId: an expression for the owner's
     gemId; otherSide: the role of the other side; isList: the owner side is [*] -->
${tc.signature("value", "ownerId", "otherSide", "isList")}
<#if isList>
if (${value}._internal_get${otherSide?cap_first}GemList().indexOfId(${ownerId}) == -1) {
  ${value}._internal_add${otherSide?cap_first}(${value}._internal_get${otherSide?cap_first}GemList().getIDs().size(),
      Collections.singletonList(${ownerId}), NotificationScope.MINIMAL);
}
<#else>
${value}._internal_set${otherSide?cap_first}(${ownerId}, NotificationScope.MINIMAL);
</#if>
