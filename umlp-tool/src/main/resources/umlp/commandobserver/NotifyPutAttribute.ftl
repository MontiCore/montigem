<#-- (c) https://github.com/MontiCore/monticore -->
<#-- a value was put into the GemMap of a qualified association; hasOldValue: the association is bidirectional, the
     command names the value the put replaced (as the client's map knows it); hasOldOwner: with a single owner, it names
     the owner the value left as well (the server corrects it, see commands/put/OldOwner) -->
${tc.signature("name", "keyType", "otherType", "hasOldValue", "hasOldOwner")}
${cd4c.method("public void notifyPut${name?cap_first}(${otherType} o, ${keyType} key, long valueGemId, long oldValueGemId, long oldOwnerGemId)")}
  CommandManager.executeCommand(new ${otherType}_put${name?cap_first}(o.getGemId(), valueGemId, key<#if hasOldValue>, oldValueGemId</#if><#if hasOldOwner>, oldOwnerGemId</#if>));
