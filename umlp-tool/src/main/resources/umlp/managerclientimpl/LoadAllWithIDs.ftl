<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("classname", "domainPackage", "isSingleton", "isAbstract")}
${cd4c.method("public List<${classname}> _doLoadAll${classname}WithIDs(List<Long> gemIDs)")}
<#if isSingleton>
  List<${classname}> result = new java.util.ArrayList<>();
  for (Long gemID : gemIDs) {
    java.util.Optional<${classname}> opt = this._do${classname}Opt(gemID);
    if (opt.isPresent()) {
      result.add(opt.get());
    }
  }
  return result;
<#else>
  java.util.Map<Long, ${classname}> loadedByID = new java.util.HashMap<>();
  List<Long> newGemIDs = new ArrayList<>();
  for (Long gemID : gemIDs) {
    if (!_isLoaded${classname}(gemID)) {
      newGemIDs.add(gemID);
    }
  }

  // load in batches of at most CommandManager.loadBatchSize gemIDs
  for (int start = 0; start < newGemIDs.size(); start += CommandManager.loadBatchSize) {
    int end = Math.min(start + CommandManager.loadBatchSize, newGemIDs.size());
    List<Long> batch = new ArrayList<>();
    for (int i = start; i < end; i++) {
      batch.add(newGemIDs.get(i));
    }
    CommandResult<${classname}> commandResult = CommandManager.executeCommand(new LoadAll${classname}WithIDs(batch));

    if (commandResult.isListResult()) {
      List<${classname}> items = commandResult.asListResult().get();
      for (${classname} item : items) {
        // register with the manager owning the concrete type (monticore#5062)
        loadedByID.put(item.getGemId(), item._registerMe());
      }
    } else {
      Log.error("0xCL001 Could not load ${classname} batch. Expected ListResult.");
    }
  }

  List<${classname}> result = new ArrayList<>();
  for (Long gemID : gemIDs) {
    if (loadedByID.containsKey(gemID)) {
      result.add(loadedByID.get(gemID));
    } else if (_isLoaded${classname}(gemID)) {
      // already cached before this call; _isLoaded guards against firing a single load command
      java.util.Optional<${classname}> opt = this._do${classname}Opt(gemID);
      if (opt.isPresent()) {
        result.add(opt.get());
      }
    }
  }

  return result;
</#if>
