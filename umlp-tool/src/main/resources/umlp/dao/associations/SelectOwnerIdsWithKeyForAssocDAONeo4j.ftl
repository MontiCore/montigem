<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the gemIds of the owners that hold an entry with the key object keyGemId, see SelectOwnerIdsWithKeyForAssocDAO -->
${tc.signature("association", "ownerType", "valueRole", "keyProperty")}
${cd4c.method("public java.util.List<Long> selectOwnerIdsWithKey${association.getName()?cap_first}(long keyGemId)")}
  try(Session session = DatabaseAccessManager4Neo4j.getSession()) {
    Result rs = session.run("MATCH (o:${ownerType})-[rel:${valueRole}]->() WHERE rel.${keyProperty} = $p1 RETURN o.gemId",
        Values.parameters("p1", keyGemId));
    java.util.List<Long> res = new java.util.ArrayList<>();
    while(rs.hasNext()){
      res.add(rs.next().get(0).asLong());
    }
    return res;
  }
