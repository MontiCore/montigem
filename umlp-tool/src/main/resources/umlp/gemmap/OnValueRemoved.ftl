<#-- (c) https://github.com/MontiCore/monticore -->
<#-- stops observing a value that left the map; observedTypes as in OnValueKeyed -->
${tc.signature("targetName", "manager", "observedTypes")}
${cd4c.method("protected void onValueRemoved(long valueGemID, ${targetName} value)")}
if (value == null) {
  if (!${manager}.isLoaded${targetName}(valueGemID)) {
    // not loaded: the observed instance is gone
    return;
  }
  value = ${manager}.get${targetName}Sure(valueGemID);
}
<#list observedTypes as type>
<#if type?is_first>if<#else>} else if</#if> (value instanceof ${type}) {
  ((${type}) value).removeObserver(keyObserver${type});
</#list>
}
