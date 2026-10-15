# 14. Python Client Generator

The Python client generator creates a [pydantic](https://docs.pydantic.dev/)-based Python interface for a MontiGem domain model.

## 14.1 Gradle Setup

The [CDGen](https://github.com/MontiCore/cd4analysis) Gradle plugin is used with custom decorators.

```groovy
plugins {
  id 'de.rwth.se.cdgen' version "$mc_version"
}
import de.monticore.cdgen.gradleplugin.CDGenTask

dependencies {
  cdTool("de.monticore.lang.cd4analysis:cdgradle:$mc_version")
  cdTool("de.monticore.lang:cd4analysis:$mc_version")
  cdTool("umlp.experiments.python-client:backend:$umlp_version:cd2pojo")
}

// Generate the files for use with the python-generator
def generateGemForPy = tasks.register("generateGemForPy", CDGenTask) {
  input.from(project.file("models/MyDomain.umlp"))
  class2MC = true
  outputDir.set(project.layout.buildDirectory.dir("py_out_java"))
  configTemplate = 'cd2python.Gem2Py' // This config template describes the Gem2Py decorators
  getExtraClasspathElements().from(configurations.cdTool)
}
// Rename python files to .py 
tasks.register("transformGemToPy", Sync) {
  from(generateGemForPy)
  dependsOn(generateGemForPy)
  into(project.layout.buildDirectory.dir("py_out"))
  eachFile { file ->
    if (file.name.endsWith(".java")) {
      file.name = file.name.replaceAll(/\.java/, ".py")
    }
  }
}
```

Run `gradle transformGemToPy`; the client is placed in `build/py_out`.

**Known issue:** The .umlp model MUST use `classdiagram` instead of the keyword `umlp`!

## 14.2 Runtime

The generated code depends on the `montigem_runtime` package (`experiments/python-client/runtime-package`, requires Python >= 3.8, pydantic and websockets).
This reference assumes Python 3.12 or newer.

```powershell
pip install montigem_runtime --extra-index-url https://git.rwth-aachen.de/api/v4/projects/129570/packages/pypi/simple
```

## 14.3 Usage

Each class `MyClass` of a domain `MyDomain` is generated as module `MyDomain.MyClass`, which defines the class `MyDomain.MyClass.MyClass`.

Connect to the backend's command endpoint and run the command consumer alongside your own code:

```python
import asyncio
from montigem_runtime.commandmanager import CommandManager
from MyDomain.MyDomainManager import MyDomainManager
from MyDomain.MyDomainCommandHandler import MyDomainCommandHandler

command_manager = CommandManager()
my_domain_manager = MyDomainManager(command_manager)
cmd_handler = MyDomainCommandHandler()


async def main():
  async def do_stuff():
    p1 = await my_domain_manager.get(Person, 42)
    p2 = await p1.sibling
    await p2.setName("Peter")
    print(p2.name, await p1.sibling.name)
    async for parent in p1.parents:
      print("parent", parent.name)

  async with asyncio.TaskGroup() as tg:
    tg.create_task(command_manager.connect(host="ws://localhost:8081/umlp/api/command"))
    tg.create_task(command_manager.command_forwarding_consumer(cmd_handler, my_domain_manager))
    tg.create_task(do_stuff())


if __name__ == "__main__":
  asyncio.run(main())
```

```
classdiagram MyDomain { // known issue: umlp MyDomain does not work!
  class Person {
    String name;
  }
  association Person -> (sibling) Person [0..1];
  association Person -> (parents) Person [*];
}
```

**Fetching an object:**

```python
from MyDomain.Person import Person

person = await my_domain_manager.get(Person, gem_id=42)
```

**Creating a new instance:**

```python
from MyDomain.Person import Person

person2 = await my_domain_manager.create(Person(gemId=42, name='Peter'))
```

**Getting the gemId of an element:**

```python
print(person.gemId)
```

**Reading primitive attributes:**

```python
print(person.name)
```

**Setting primitive attributes:**

```python
await person.setName("Petra")
```

**Following mandatory/optional associations:**

```python
other_person = await person.sibling
```

*Hint: the association getters and setters can be chained with a single await, e.g. `await person.sibling.sibling.sibling.name`*

**Setting mandatory/optional associations:**

```python
await person.setSibling(other_person.gemId)
await person.setSibling(-1)  # setting optional ones absent
```

**Following multiple associations:**

```python
async for parent in person.parents:
  print(parent.name)
```

*(More information is available via the `GemList` interface)*

**Setting multiple associations:**

```python
await person.parents_add(len(person.parents), other_person.gemId)
await person.parents_rem(index_in_list=1)
```

**Sending custom commands:**
They currently have to be constructed manually:

````python
await CommandManager.instance.send(
  Command(commandType="mydomain.commands.MyCustomCommand",
          personGemId=person.gemId,
          person2GemId=person_2.gemId))
````

**Observers:**
Each domain class is generated with an Observer class as well.

````python
from MyDomain.PersonObserver import PersonObserver


class MyPersonObserver(PersonObserver):
  async def notifySetname(self, person: Person, _old_value: str, name: str):
    pass


person._observer.append(MyPersonObserver())
````

---
[< Previous: Testing](13_Testing.md) | [Back to Index](../MontiGem_Documentation.md)
