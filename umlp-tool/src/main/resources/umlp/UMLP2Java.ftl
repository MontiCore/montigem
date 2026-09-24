<#-- (c) https://github.com/MontiCore/monticore -->
<#--
  This config template does the default UMLP2Java generation.

  Call it using the CLI: .. -ct umlp.UMLP2Java

-->
${tc.signature("decoratedAST", "glex", "hwPath", "cdGenerator", "schemaGenerator", "oclCompilationUnits")}

<#-- Initialize all modules, adding their templates to the other hookpoints, e.g., BeforeTransform -->
${includeArgs("umlp.generator.hook.InitModules", glex)}

<#-- Call all decorators -->
<#assign beDecorator = tc.instantiate("umlp.generator.domain.decorator.BEDecorator", [glex, hwPath, oclCompilationUnits])>
<#assign commandDecorator = tc.instantiate("umlp.generator.domain.decorator.commands.CommandDecorator", [glex])>
<#assign requestDecorator = tc.instantiate("umlp.generator.domain.decorator.commands.GetFreeGemIdRequestDecorator", [glex])>
<#assign topDecorator = tc.instantiate("de.monticore.cd.codegen.TopDecorator", [hwPath])>

${includeArgs("umlp.generator.hook.BeforeTransform", decoratedAST, glex, hwPath, cdGenerator, schemaGenerator, beDecorator, commandDecorator, requestDecorator, topDecorator)}

<#-- After BeforeTransform (may add SQL type mappings), before the decorators (DAOs need the SQL names) -->
${schemaGenerator.initIdentifiers(ast)}
${beDecorator.transform(ast, decoratedAST)}
${commandDecorator.transform(ast, decoratedAST)}
${requestDecorator.transform(ast, decoratedAST)}

${includeArgs("umlp.generator.hook.BeforeTOP", decoratedAST, glex, hwPath, cdGenerator, schemaGenerator, beDecorator, commandDecorator, requestDecorator, topDecorator)}
${topDecorator.decorate(decoratedAST)}

${includeArgs("umlp.generator.hook.AfterTransform", decoratedAST, glex, hwPath, cdGenerator, schemaGenerator, beDecorator, commandDecorator, requestDecorator, topDecorator)}

<#-- Generate Java-Code -->
${cdGenerator.generate(decoratedAST)}

<#-- Generate SQL-Schema -->
${schemaGenerator.generate(ast)}
