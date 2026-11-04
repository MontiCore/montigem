<#-- (c) https://github.com/MontiCore/monticore -->
<#-- ownerIsLeft: whether the owner (the qualified side) is the left side of the association -->
${tc.signature("association", "ownerName", "keyType", "ownerIsLeft")}
${cd4c.method("public void putMany${association.getName()?cap_first}(long ${ownerName}GemId, Map<${keyType}, Long> ids)")}
  ensurePut${association.getName()?cap_first}Created();
  if(ids.isEmpty()) {
    return;
  }
  try(Connection c = DatabaseAccessManager.getConnection()){
    try(PreparedStatement st = c.prepareStatement(put${association.getName()?cap_first})) {
      c.setAutoCommit(false);
      st.clearBatch();
      for (Map.Entry<${keyType}, Long> entry : ids.entrySet()) {
        st.clearParameters();
<#if ownerIsLeft>
        st.setLong(1, ${ownerName}GemId);
        st.setLong(2, entry.getValue());
<#else>
        st.setLong(1, entry.getValue());
        st.setLong(2, ${ownerName}GemId);
</#if>
        st.setObject(3, entry.getKey());
        st.addBatch();
      }
      st.executeBatch();
      c.commit();
    } catch (SQLException e){
      try {
        c.rollback();
      } catch (SQLException ex) {
        Log.error("Error rolling back transaction", ex);
      }
      throw new RuntimeException(e);
    }
    finally{
      try {
        c.setAutoCommit(true);
      } catch (SQLException e) {
        Log.error("Error while enabling autocommit in putMany${association.getName()?cap_first}", e);
      }
    }
  } catch(SQLException e) {
    throw new RuntimeException(e);
  }
