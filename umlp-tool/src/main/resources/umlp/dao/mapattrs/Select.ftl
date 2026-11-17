<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the entries in insertion order -->
${tc.signature("tableName", "name", "keyType", "valueType", "keySymType", "valueSymType")}
<#assign attrName=name?cap_first>
${cd4c.method("public Map<${keyType}, ${valueType}> select${attrName}(long gemId)")}
if (select${attrName}Statement == null) {
  select${attrName}Statement = "SELECT _key, _value FROM ${tableName} WHERE id = ? ORDER BY index ASC";
}
try (Connection connection = DatabaseAccessManager.getConnection();
     PreparedStatement st = connection.prepareStatement(select${attrName}Statement)) {
  st.clearParameters();
  st.setLong(1, gemId);
  Map<${keyType}, ${valueType}> res = new LinkedHashMap<>();
  try (ResultSet rs = st.executeQuery()) {
    while (rs.next()) {
      res.put(${includeArgs("umlp.dao.mapattrs.GetValue", "rs", keySymType, 1, "")},
          ${includeArgs("umlp.dao.mapattrs.GetValue", "rs", valueSymType, 2, "")});
    }
  }
  return res;
} catch (SQLException e) {
  throw new RuntimeException(e);
}
