${signature("statement","attributeName", "index", "isOptional")}
<#-- as OffsetDateTime in UTC: the Neo4j driver has no Instant -->
"${index}", <#if isOptional>${attributeName}<#else>java.util.Optional.ofNullable(${attributeName})</#if>.map(i -> i.atOffset(java.time.ZoneOffset.UTC)).orElse(null)
