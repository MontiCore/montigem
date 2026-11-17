<#-- (c) https://github.com/MontiCore/monticore -->
<#-- chosen time keys: the association table stores them only to the microsecond, so finer keys are refused (see
     GemValueMap#isSafeKey); a null key is reported elsewhere -->
${tc.signature("keyType")}
${cd4c.method("public boolean isSafeKey(${keyType} key)")}
return key == null || key.getNano() % 1000 == 0;
