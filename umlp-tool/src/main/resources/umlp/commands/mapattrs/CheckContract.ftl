<#-- (c) https://github.com/MontiCore/monticore -->
<#-- rejects null keys and values, unsafe long keys and time keys finer than microseconds -->
${tc.signature("hasValue", "longKey", "nanoKey")}
${cd4c.method("public boolean checkContract()")}
if (key == null<#if hasValue> || value == null</#if>) {
  Log.warn("0xA7334 The command for the object with id " + getGemId() + " is rejected: null keys and values are "
      + "not allowed");
  return false;
}
<#if longKey>
if (key < -GemValueMap.MAX_SAFE_KEY || key > GemValueMap.MAX_SAFE_KEY) {
  Log.warn("0xA7335 The put of the key " + key + " into the object with id " + getGemId()
      + " is rejected: a browser cannot represent the key exactly");
  return false;
}
</#if>
<#if nanoKey>
if (key.getNano() % 1000 != 0) {
  Log.warn("0xA7337 The put of the key " + key + " into the object with id " + getGemId()
      + " is rejected: a database stores the key only to the microsecond");
  return false;
}
</#if>
return true;
