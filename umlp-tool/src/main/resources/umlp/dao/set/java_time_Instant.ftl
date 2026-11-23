${signature("statement","attributeName", "index", "isOptional")}
<#-- as OffsetDateTime in UTC: JDBC drivers (PostgreSQL) have no Instant -->
<#assign instant=isOptional?then("${attributeName}.orElse(null)", attributeName)>
${statement}.setObject(${index}, ${instant} == null ? null : ${instant}.atOffset(java.time.ZoneOffset.UTC));
