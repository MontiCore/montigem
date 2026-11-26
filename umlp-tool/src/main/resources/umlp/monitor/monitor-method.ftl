<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "returnType", "paramSignature", "paramCallsite", "isVoid", "paramsPresent", "actualCall", "signature")}
${cd4c.method("public ${returnType} ${name}(${paramSignature})")}

MethodMonitor monitor = MethodMonitor.reportMethodCall(this, "${signature}", "${name}" <#if paramsPresent>, ${paramCallsite}</#if>);
<#if !isVoid>${returnType} res;</#if>
try {
<#if isVoid>
  ${actualCall};
<#else>
  res = ${actualCall};
</#if>
} catch (Exception e) {
  monitor.reportError(e);
  throw e;
}
<#if isVoid>
  monitor.reportReturn();
<#else>
  monitor.reportReturnWithValue(Optional.ofNullable(res));
  return res;
</#if>

