# Developer guides #

RepositoryStructure : a short overview of the project structure in the Passerelle repository.

PasserelleActorDevelopmentGuide : This guide presents an overview of the technological basis for the Passerelle actor model.
Then it describes the actor lifecycle, actor development APIs and types of actors. For each major actor type, a complete example is developed.
Finally, some advanced topics are discussed.

PasserelleActorPackagingGuide : This guide describes the concrete setup of an actor development project and how to package and deploy your new actors in the RCP workbench.

PasserelleUnitTestingGuide : Introducing the usage of Passerelle's test support tools, for unit-testing actors and simple flows.

# User guides #

# Links & References #

Some of the stuff used in Passerelle :

[Ptolemy II](http://ptolemy.berkeley.edu/ptolemyII/) : the platform for actor-based solutions for hybrid modeling in Java.

[OSGi](http://www.osgi.org) : besides Ptolemy, the second most important element of Passerelle's software architecture

[eclipse](http://www.eclipse.org) : our IDE and RCP platform

[eclipse's equinox](http://eclipse.org/equinox/) : the OSGi implementation on which Passerelle is based

[Apache Commons](http://commons.apache.org/) : Passerelle uses many libraries from Apache Commons, such as Collections, IO, HTTP Client, Lang, Net etc.

[JDOM](http://www.jdom.org/) : our preferred XML API

[SLF4J](http://www.slf4j.org/) : our preferred logging API