${signature("statement","attributeName", "index", "isOptional")}
<#-- as OffsetDateTime in UTC: JDBC drivers (PostgreSQL) have no Instant -->
${statement}.setObject(${index}, <#if isOptional>${attributeName}<#else>java.util.Optional.ofNullable(${attributeName})</#if>.map(i -> i.atOffset(java.time.ZoneOffset.UTC)).orElse(null));
