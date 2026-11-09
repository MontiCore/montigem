<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the gemIds of the owners that hold an entry with the key object keyGemId (type qualified association): the server
     loads them before it deletes the key object, so that their entries leave through the owners (bidirectional
     associations: the values' other side follows) -->
${tc.signature("association", "sqlTable", "ownerColumn", "keyColumn")}
${cd4c.method("public java.util.List<Long> selectOwnerIdsWithKey${association.getName()?cap_first}(long keyGemId)")}
  String selectOwnerIdsWithKey = "SELECT ${ownerColumn}_id FROM ${sqlTable} WHERE ${keyColumn} = ?";
  try(Connection connection = DatabaseAccessManager.getConnection(); PreparedStatement st = connection.prepareStatement(selectOwnerIdsWithKey)){
    st.clearParameters();
    st.setLong(1, keyGemId);
    java.util.List<Long> res = new java.util.ArrayList<>();
    try(ResultSet rs = st.executeQuery()) {
      while(rs.next()){
        res.add(rs.getLong(1));
      }
    }
    return res;
  }catch (SQLException e){
    throw new RuntimeException(e);
  }
