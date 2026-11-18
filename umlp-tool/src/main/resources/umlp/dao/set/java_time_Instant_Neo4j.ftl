${signature("statement","attributeName", "index", "isOptional")}
<#-- as OffsetDateTime in UTC: the Neo4j driver has no Instant -->
<#assign instant=isOptional?then("${attributeName}.orElse(null)", attributeName)>
"${index}", ${instant} == null ? null : ${instant}.atOffset(java.time.ZoneOffset.UTC)
