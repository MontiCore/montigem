<#-- (c) https://github.com/MontiCore/monticore -->
<#-- Generates ComponentObserverManager for the current domain model -->
${tc.signature("AST", "cdName", "cDNames", "domainClassNamesCDMap", "cdTSLibraryMap")}

import { ComponentObserverManager, GemIdentifiable } from "@umlp/commonj2ts";
<#list cDNames as specificCDName>
  import { ${specificCDName}Manager } from "<#if cdTSLibraryMap[specificCDName]??>${cdTSLibraryMap[specificCDName]}<#else>${specificCDName?lower_case}/${specificCDName}Manager</#if>";
</#list>

<#list domainClassNamesCDMap as domainClassName, package>
import { ${domainClassName}AllObserver } from "<#if cdTSLibraryMap[package]??>${cdTSLibraryMap[package]}<#else>${package?lower_case}/${domainClassName}AllObserver</#if>";
</#list>

export class ${cdName}ComponentObserverManager extends ComponentObserverManager {

  public static init(): void {
    ComponentObserverManager.initMe(new ${cdName}ComponentObserverManager());
  }

  // Domain-specific mapping from a type name to a function that create an AllObserver for that type
  private observerFactory: Map<string, ((update: () => void) => any)> = new Map;

  // Domain-specific mapping from a type name to a function that gets an object of that type
  private objectGetterFactory: Map<string, ((gemId: number) => Promise<GemIdentifiable>)> = new Map();

  constructor() {
    super();
    // AllObserver factory
<#list domainClassNamesCDMap as domainClassName, package>
    this.observerFactory.set("${domainClassName}", (update) => {
      // Avoid Async Constructor here
      const obs : ${domainClassName}AllObserver = new ${domainClassName}AllObserver();
      obs.consumer = update;
      return obs;
    });
</#list>
    // domain object getter factory
<#list domainClassNamesCDMap as domainClassName, package>
    this.objectGetterFactory.set("${domainClassName}", (gemId) => Promise.resolve(${package}Manager.${domainClassName?uncap_first}Opt(gemId)));
</#list>
  }

  protected getObserverFactory(): Map<string, ((update: () => void) => any)> {
    return this.observerFactory;
  }

  protected getObjectGetterFactory(): Map<string, ((gemId: number) => Promise<GemIdentifiable>)> {
    return this.objectGetterFactory;
  }
}
