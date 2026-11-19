<#--CD2OwnDecorator.ftl-->
<#-- @ftlvariable name="glex" type="de.monticore.generating.templateengine.GlobalExtensionManagement" -->
<#-- @ftlvariable name="decConfig" type="de.monticore.cd.codegen.DecoratorConfig" -->
<#-- @ftlvariable name="tc" type="de.monticore.generating.templateengine.TemplateController" -->
<#-- @ftlvariable name="genSetup" type="de.monticore.generating.GeneratorSetup" -->

${genSetup.setTracing(false)}    <#--must be false for python-->
${genSetup.setCommentStart("#")}
${genSetup.setCommentEnd("\n")}
${genSetup.setDefaultFileExtension(".py")} <#--output .py files-->

<#--Apply the default creator: Copy the original CD and use it as the base-->
${decConfig.withCopyCreator().defaultApply()}

<#--It is possible to add your own decorators via this config template too: -->
<#-- We just have to add the class to the classpath of the CDTool -->
<#--${decConfig.withDecorator("mc.MyOwnDecorator").defaultApply()}-->
<#--TODO: Make configurable/eImports -->
<#--${decConfig.withGetters().ignoreOnName("noGetter").defaultApply()}-->

${decConfig.withDecorator("umlp.py.GemBaseDecorator").applyOnName("pygem")}
${decConfig.withDecorator("umlp.py.GemManagerDecorator").applyOnName("pygem")}
${decConfig.withDecorator("umlp.py.GemSetterDecorator").applyOnName("pygem")}
${decConfig.withDecorator("umlp.py.GemObserverDecorator").applyOnName("pygem")}
${decConfig.withDecorator("umlp.py.GemCommandObserverDecorator").applyOnName("pygem")}
${decConfig.withDecorator("umlp.py.GemCommandHandlerDecorator").applyOnName("pygem")}
${decConfig.withDecorator("umlp.py.GemBiAssocObserverDecorator").applyOnName("pygem")}
<#-- workaround for HWC -->
${decConfig.withDecorator("umlp.py.GemHWDecorator").applyOnName("pygem")}

${decConfig.withDecorator("umlp.py.GemMethodDecorator").applyOnName("pygem")}

${glex.replaceTemplate("cd2java.Class", glex.templateHP("cd2python.Class"))}
${glex.replaceTemplate("cd2java.Enum", glex.templateHP("cd2python.Enum"))}
