${signature("statement","attributeName", "index", "isOptional")}
<#-- as nanoseconds (BIGINT, up to about 292 years) -->
<#assign duration=isOptional?then("${attributeName}.orElse(null)", attributeName)>
${statement}.setObject(${index}, ${duration} == null ? null : ${duration}.toNanos());
