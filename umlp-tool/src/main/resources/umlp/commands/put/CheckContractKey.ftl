<#-- (c) https://github.com/MontiCore/monticore -->
<#-- type qualified association: the key is chosen on put, so it must be present; a command that fails its contract is
     neither executed nor sent on by the server (the owner rejects a null key before sending, see
     methods/gemmap/PutKey) -->
${tc.signature()}
${cd4c.method("public boolean checkContract()")}
if (key == null) {
  Log.warn("0xA7324 The put of the object with id " + getGemId() + " under the key null is rejected");
  return false;
}
return true;
