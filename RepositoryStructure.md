# Introduction #

Passerelle consists principally of a flow engine, including actor development APIs, a basic actor library and a modeling&execution workbench.

The engine integrates some underlying technologies and contains wrappers and extensions on them. [Ptolemy](http://ptolemy.berkeley.edu/ptolemyII/) is the main underlying platform for Passerelle. But their are also lots of other (smaller) 3rd-party libraries on which the Passerelle engine depends.

Actor libraries may also add their own dependencies, required to implement the specific actor functions (e.g. the [Tango](http://www.tango-controls.org/) system i.c.o. actors for synchrotron beamline control & data acquisition).

The repository also contains an eclipse RCP workbench for Passerelle, and an alternative Java-Swing-based "HMI".

All of the above are structured as one or more OSGi bundles. Each bundle corresponds to an eclipse PDE project. All these projects are available in the source repository on this site.

# Repository structure #

The structure is as follows :
  * ~~com.isencia.passerelle.gettingstarted~~ : contains the PDE target platform and some docs.
    * This "old-style" target contains all binary jars etc. This is no longer the preferred approach for BuildingFromSource.
    * The new approach uses Maven & Tycho based on POMsand compatible target definitions.
  * com.isencia.passerelle.target-platform : this contains the tycho-compatible target definition, no longer the full set of binary jars.
  * com.isencia.passerelle.parent : this contains the parent POM from where a build can be started
  * passerelle-core : the core projects for the engine and a basic actor library.
  * passerelle-eclipse-workbench : the projects for the RCP, including product definition.
  * passerelle-swing-hmi : the Swing-based tool.
  * ptolemy : a subset of Ptolemy v7.0.1, repackaged as OSGi bundles, and with a limited number of small code changes needed for Passerelle.
  * contributions-actors : this is where other participants may maintain their actor libraries, that are not part of the "basic" Passerelle distribution.
  * contributions-tools : the place for contributions of miscellaneous extras.

In the contributions sections, Soleil has already provided their custom actor library with dependencies, and a custom GUI to create and run batches of Passerelle models (Bossanova).

Soleil's dependencies are currently collected in one big collection of jars in a single bundle. Work is planned to move to a cleaner approach of bundle-izing each dependency separately.