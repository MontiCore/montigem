

## MontiGem: Model-Driven Development of Data-Centric Web 
Applications 

[MontiGem](https://www.se-rwth.de/research/MontiGem/) is a 
model-driven generator framework for developing data-centric web 
applications and domain-specific information systems. Instead of 
implementing recurring application elements manually, developers 
describe the relevant domain, user interfaces, constraints, and 
access-control rules using the 
[Unified Modelling Language](https://www.se-rwth.de/research/Unified-Modeling-Language/). 
MontiGem transforms these models into substantial parts of an 
executable application that is immediately executable. 

MontiGem can be used for generation of substantial parts of the final 
software product as well as for quick prototyping in a Low-Code 
style. 

The central input is a domain model expressed as a [UML class 
diagram](https://www.se-rwth.de/research/Unified-Modeling-Language/). 

[The *class diagrams* used for this application can be viewed here.](${TODO: Link auf entsprechende Seite}) 

Additional models can describe application-specific views and 
graphical interfaces. 

 * *GUI models* describe specific views (click around to see some).
   These fuses can be composed from atomic components and smaller 
   reusable GUI structures (like this one). 
 * *Object Constraint Language (OCL)* models specify data constraints, 
   validation rules, and simple business logic, 
 * *tagging models* attach 
   *roles and access rights* to domain elements. 

From these models, MontiGem generates: 

 * database structure (relational database).
 * persistence mechanisms (load and store data), 
 * domain-specific backend components (Java), 
 * domain-specific frontendend components (Javascript), 
 * client-server communication (data transfer objects), 
 * data validation for frontand and backend (so it is consistent and secure), 
 * access-control mechanisms, 
 * a core graphical user interfaces (single object, list, search, ...), 
 * an infrastructure for application testing based on JUnit, and 
 * many more technical components (builder, vistors, ...),

The generated code provides lots of *extension points* so that handwritten code can be 
easily added. For a systematic adaptation of the code MontiGem  uses 
*extensible templates* during the generation process. 

MontiGem is built using the [MontiCore language 
workbench](https://github.com/MontiCore/tutorial/), which supports 
the development, composition, and processing of various 
[domain-specific 
languages](https://www.se-rwth.de/research/Domain-Specific-Languages/). 

## Development Method

A typical development process begins with modeling the domain 
structure. From here on developers may permanently (re-)generate the 
application, and refine its data structure, constraints, and GUI 
pages, through additional models, and implement exceptional or highly 
specialized functionality with handwritten code.  

Among others, a valuable advantage is that recurring architectural 
and design patterns are encoded in the generator and reused 
consistently. Changes to the domain model are consistently propagated 
across several application layers, avoiding inconsistencies between 
database, backend, communication, and frontend code. Development 
efficiency is increased and the results are fully deterministic and 
repeatable. 

MontiGem is particularly suitable for applications dominated by 
structured data, forms, tables, validation, user roles, and 
conventional create-read-update-delete operations. Extension 
packages, e.g. for [digital twin 
cockpits](https://www.se-rwth.de/research/Digital-Twins/), and AI 
application/chatbot integration exist. 
 
The framework has been applied in several research and industrial 
projects, including financial management, IoT device tracking, 
chatbot development, energy management, privacy-policy analysis, and 
model-based engineering tools. 

## Related Topics
- [MontiGem](https://www.se-rwth.de/research/MontiGem/)
- [Agile Model-Based Software Engineering](https://www.se-rwth.de/research/Agile-MBSE)
- [Generative Software Engineering](https://www.se-rwth.de/research/Generative-SE)
- [MontiCore - Language Workbench](https://www.se-rwth.de/research/MontiCore)
- [UML/P](https://www.se-rwth.de/research/UML-P)
- [Unified Modeling Language (UML)](https://www.se-rwth.de/research/Unified-Modeling-Language)
- [Model-Based Assistive Systems](https://www.se-rwth.de/research/Model-Based-Assistive-Systems)
- [Models in Enterprise Information System Development](https://www.se-rwth.de/research/Enterprise-Information-Systems)
- [Digital Twins and Digital Shadows in Engineering, Operation and Production](https://www.se-rwth.de/research/Digital-Twins)

#### Further links:

- [Publications](https://www.se-rwth.de/publications)
- [Topic sorted list of most interesting publications](https://www.se-rwth.de/research)
- [Books](https://www.se-rwth.de/books)
- [Aachener Informatik-Berichte, Software Engineering / Ph.D. Theses](https://www.se-rwth.de/phdtheses)
- [Website of the SE](https://www.se-rwth.de)

