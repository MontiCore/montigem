<#-- (c) https://github.com/MontiCore/monticore -->
<#-- included code (tc.includeArgs): the other side of a loaded value no longer refers to an owner (bidirectional
     qualified association, see GenService#isOtherSideOfMap), like a bidirectional list clears its other side (MINIMAL).
     value: an expression for the value object; ownerId: an expression for the owner's gemId; otherSide: the role of the
     other side; isList: the owner side is [*] -->
${tc.signature("value", "ownerId", "otherSide", "isList")}
<#if isList>
{
  int idx = ${value}._internal_get${otherSide?cap_first}GemList().indexOfId(${ownerId});
  if (idx != -1) {
    ${value}._internal_remove${otherSide?cap_first}(Collections.singleton(idx), NotificationScope.MINIMAL);
  }
}
<#else>
if (${value}.get${otherSide?cap_first}Gem().getGemId() == ${ownerId}) {
  ${value}._internal_set${otherSide?cap_first}(-1L, NotificationScope.MINIMAL);
}
</#if>
