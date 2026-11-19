Example application using the [dashboard module](../../modules/dashboard).
It provides an example domain (organizations, teams, people, ...) and dashboard configurations visualizing it.

Prerequisites: Java 21 installed 
To build the single jar for sharing use `gradle :examples:dashboard:frontend:fullAppJar`
The jar will be built to `frontend/build/libs`

To run the fullApp jar use the following commands:
````bash
java -Djarmode=tools -jar .\frontend-7.9.1-SNAPSHOT-fullAppJar.jar extract
cd .\frontend-7.9.1-SNAPSHOT-fullAppJar
java -jar .\frontend-7.9.1-SNAPSHOT-fullAppJar.jar
````
Then open localhost:4200 in your browser. 
