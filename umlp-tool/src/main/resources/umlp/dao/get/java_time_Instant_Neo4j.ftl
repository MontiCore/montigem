${signature("statement","columnName")}
(${statement}.get(${columnName}).isNull()? null :${statement}.get(${columnName}).asOffsetDateTime().toInstant())
