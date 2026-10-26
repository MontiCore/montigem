<#-- (c) https://github.com/MontiCore/monticore -->
<#-- rekeys through the owner once built: its observers are notified (the server's command sending observer sends
     _rekey to the participants that loaded the owner); keyType as in the RTE: the key, or the key's gemId (long) -->
${tc.signature("name", "keyType")}
${cd4c.method("public void _internal_rekeyAndSync(${keyType} oldKey, ${keyType} newKey, long valueGemID)")}
if (buildFinished) {
  getOwner()._internal_rekey${name?cap_first}(oldKey, newKey, valueGemID, NotificationScope.ALL);
}
else {
  _internal_rekey(oldKey, newKey, valueGemID);
}
