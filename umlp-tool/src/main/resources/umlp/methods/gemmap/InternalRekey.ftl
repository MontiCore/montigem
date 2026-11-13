<#-- (c) https://github.com/MontiCore/monticore -->
<#-- moves the value with gemID from oldKey to newKey after its qualifying attribute changed and notifies the
     observers (the server's command sending observer sends _rekey on); nothing happens if the value is not (or no
     longer) stored under oldKey, e.g. as this participant already rekeyed it itself -->
${tc.signature("name", "sourceName", "keyType")}

${cd4c.method("public boolean _internal_rekey${name?cap_first}(${keyType} oldKey, ${keyType} newKey, long gemID, NotificationScope scope)")}
if (!this._${name}._internal_rekey(oldKey, newKey, gemID)) {
  return false;
}
for (${sourceName}Observer obs : ${sourceName?uncap_first}Observers) {
  obs.maybeNotifyRekey${name?cap_first}((${sourceName}) this, oldKey, newKey, gemID, scope);
}
return true;
