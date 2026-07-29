To test our JSweet adapters, there are two test methods 
1. transpile only tests including CoCo checks
2. execution including typescript compile and transpile

For both options there is a Util class that provides default Adapter setups 
such as the ones the MontiGem Gradle plugin is actually using.
Execution tests require a typescript compilation step and execution in the node js runtime.
For this you can use the RealTscCheck.

The execution tests compare the result of static Java method executions with their 
transpiled version. This requires that the result is stringified the same in js and java, which 
is why we only allow simple datatypes.

The Java sources of the execution test are in `test/java/resources/exectest`. 
Each method that shall be tested and compared is marked by the `JSweetTest` Java annotation.  
