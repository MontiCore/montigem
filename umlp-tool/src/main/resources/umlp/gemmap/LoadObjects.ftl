<#-- (c) https://github.com/MontiCore/monticore -->
<#-- methodName: loadValueObjects, or loadKeyObjects of a GemEntryMap -->
${tc.signature("methodName", "type", "manager")}
${cd4c.method("protected List<LoadResult<${type}>> ${methodName}(List<Long> gemIDs)")}
// batch-load the elements with gemID from input list using manager and return the result as list
List<${type}> loaded = ${manager}.loadAll${type}WithIDs(gemIDs);
java.util.Map<Long, ${type}> loadedByID = new java.util.HashMap<>();
for(${type} item : loaded) {
  loadedByID.put(item.getGemId(), item);
}
List<LoadResult<${type}>> loadResults = new ArrayList<>();
for(long gemID : gemIDs) {
  // todo load in a way that keeps the information, why it could not be loaded
  ${type} item = loadedByID.get(gemID);
  if(item == null) {
    loadResults.add(new LoadResult(LoadingState.FAILED_TO_LOAD));
  }
  else {
    loadResults.add(new LoadResult(item));
  }
}
return loadResults;
