${signature("statement","columnName")}
<#-- stored as OffsetDateTime in UTC: JDBC drivers (PostgreSQL) have no Instant -->
java.util.Optional.ofNullable(${statement}.getObject(${columnName}, java.time.OffsetDateTime.class)).map(java.time.OffsetDateTime::toInstant).orElse(null)
