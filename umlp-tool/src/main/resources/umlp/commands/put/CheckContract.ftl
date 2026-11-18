<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the preconditions of a put into a GemMap; a command that fails its contract is neither executed nor sent on by the
     server (CommandManager sends a command on even if its doAction fails, and the other participants put by id
     without loading anything, so they would store an entry the server rejected).
     chosen: type qualified association, the key is chosen on put, so it must be present (the owner rejects a null key
     before sending, see methods/gemmap/InternalPutId);
     longKey: a long key must be within the integers a browser represents exactly (GemValueMap#isSafeKey);
     the server (participant 0) checks that the value exists and, for a chosen key object (keyTypeManager not empty),
     that the key object exists; the other participants trust the server -->
${tc.signature("chosen", "longKey", "name", "targettype", "targetTypeManager", "keyType", "keyTypeManager")}
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
if (CommandManager.getParticipantId() == 0) {
  if (${targetTypeManager}.${targettype?uncap_first}Opt(${name}GemId).isEmpty()) {
    Log.warn("0xA7328 The put of the object with id " + ${name}GemId + " is rejected: the object does not exist");
    GeneralErrorCollector.reportSyncError();
    return false;
  }
<#if keyTypeManager?has_content>
  if (${keyTypeManager}.${keyType?uncap_first}Opt(key).isEmpty()) {
    Log.warn("0xA7329 The put under the key object with id " + key + " is rejected: the key object does not exist");
    GeneralErrorCollector.reportSyncError();
    return false;
  }
</#if>
}
return true;
