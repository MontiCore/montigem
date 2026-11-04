<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the preconditions of the key of a put; a command that fails its contract is neither executed nor sent on by the
     server (the owner rejects such a key before sending, see methods/gemmap/PutKey and Put).
     chosen: type qualified association, the key is chosen on put, so it must be present;
     longKey: a long key must be within the integers a browser represents exactly (GemValueMap#isSafeKey) -->
${tc.signature("chosen", "longKey")}
${cd4c.method("public boolean checkContract()")}
<#if chosen>
if (key == null) {
  Log.warn("0xA7324 The put of the object with id " + getGemId() + " under the key null is rejected");
  return false;
}
</#if>
<#if longKey>
if (key != null && (key < -9007199254740991L || key > 9007199254740991L)) {
  Log.warn("0xA7327 The put of the object with id " + getGemId() + " under the key " + key
      + " is rejected: a browser cannot represent the key exactly");
  return false;
}
</#if>
return true;
