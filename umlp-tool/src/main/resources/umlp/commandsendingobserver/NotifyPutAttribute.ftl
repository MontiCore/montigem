<#-- (c) https://github.com/MontiCore/monticore -->
<#-- a value was put into the GemMap of a qualified association; hasOldOwner: the association is bidirectional with a
     single owner, the command names the owner the value left, so it reaches that owner's subscribers as well -->
${tc.signature("name", "keyType", "otherType", "hasOldOwner")}
${cd4c.method("public void notifyPut${name?cap_first}(${otherType} o, ${keyType} key, long valueGemId, long oldOwnerGemId)")}
  CommandManager.sendCommandToSubscribers(new ${otherType}_put${name?cap_first}(o.getGemId(), valueGemId, key<#if hasOldOwner>, oldOwnerGemId</#if>));
