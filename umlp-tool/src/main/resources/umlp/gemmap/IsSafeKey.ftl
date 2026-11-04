<#-- (c) https://github.com/MontiCore/monticore -->
<#-- long keys: the browser represents integers exactly only up to 2^53-1 (GemValueMap#MAX_SAFE_KEY, see
     GemValueMap#isSafeKey); a null key is reported elsewhere -->
${tc.signature("keyType")}
${cd4c.method("public boolean isSafeKey(${keyType} key)")}
return key == null || (key >= -9007199254740991L && key <= 9007199254740991L);
