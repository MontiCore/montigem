${signature("statement","attributeName", "index", "isOptional")}
<#-- as nanoseconds, like in SQL -->
<#assign duration=isOptional?then("${attributeName}.orElse(null)", attributeName)>
"${index}", ${duration} == null ? null : ${duration}.toNanos()
