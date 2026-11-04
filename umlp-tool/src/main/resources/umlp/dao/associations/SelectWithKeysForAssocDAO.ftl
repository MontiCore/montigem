<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("oneSideName", "otherSide", "keyType")}
<#assign methodName = otherSide?cap_first+"IdsWithKeysFor"+oneSideName?cap_first>
${cd4c.method("public Map<${keyType}, Long> select${methodName}(long ${oneSideName}GemId)")}
ensureSelect${methodName?cap_first}Created();

try(Connection connection = DatabaseAccessManager.getConnection(); PreparedStatement st = connection.prepareStatement(select${methodName})) {
  st.clearParameters();
  st.setLong(1, ${oneSideName}GemId);
  Map<${keyType}, Long> res = new java.util.LinkedHashMap<>();
    try(ResultSet rs = st.executeQuery()) {
      while(rs.next()){
        res.put(rs.getObject(2, ${keyType}.class), rs.getLong(1));
      }
      return res;
    } catch(SQLException e) {
      throw new RuntimeException(e);
    }
} catch (SQLException e){
  throw new RuntimeException(e);
}
