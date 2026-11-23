${signature("statement","columnName")}
(${statement}.get(${columnName}).isNull() ? null : java.time.Duration.ofNanos(${statement}.get(${columnName}).asLong()))
