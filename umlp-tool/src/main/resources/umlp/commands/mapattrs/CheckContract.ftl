<#-- (c) https://github.com/MontiCore/monticore -->
<#-- rejects null keys and values and unsafe long keys -->
${tc.signature("hasValue", "longKey")}
${cd4c.method("public boolean checkContract()")}
if (key == null<#if hasValue> || value == null</#if>) {
  Log.warn("0xA7334 The command for the object with id " + getGemId() + " is rejected: null keys and values are "
      + "not allowed");
  return false;
}
<#if longKey>
if (key < -9007199254740991L || key > 9007199254740991L) {
  Log.warn("0xA7335 The put of the key " + key + " into the object with id " + getGemId()
      + " is rejected: a browser cannot represent the key exactly");
  return false;
}
</#if>
return true;
