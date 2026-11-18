${signature("statement","columnName")}
<#-- stored as OffsetDateTime in UTC: JDBC drivers (PostgreSQL) have no Instant -->
(${statement}.getObject(${columnName}, java.time.OffsetDateTime.class) == null ? null : ${statement}.getObject(${columnName}, java.time.OffsetDateTime.class).toInstant())
