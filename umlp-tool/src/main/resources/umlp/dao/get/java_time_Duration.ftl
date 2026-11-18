${signature("statement","columnName")}
<#-- stored as nanoseconds, see set/java_time_Duration -->
(${statement}.getObject(${columnName}) == null ? null : java.time.Duration.ofNanos(${statement}.getLong(${columnName})))
