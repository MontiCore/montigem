<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("targetName")}
${cd4c.method("protected void onValueKeyed(${targetName} value)")}
// rekey the value when its qualifying attribute changes
if (!value.containsObserver(keyObserver)) {
  value.addObserver(keyObserver);
}
