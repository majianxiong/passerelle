# Moving the Passerelle editor to Graphiti #

## Drivers ##
### As is ###
  * The look-n-feel of the current editor is not optimal
  * Connection routing is primitive
  * Actor configuration not possible via double-click and config dialog.
  * Ptolemy/Passerelle models, stored in MOML files, don't have explicit support for advanced layout, decorations etc.
  * GEF-based implementation leaves lots of technical issues and decisions to the editor development.
  * Code base has evolved through several developers and learning curves.
  * As a consequence of these two last facts, the implementation still had some annoying issues, and code was not ideal to fix/maintain/extend.

### To be ###
  * Graphiti offers a higher level of abstraction, based on proven requirements for feature-sets of graphical model editors. It combines this with a nice approach, APIs and patterns to implement custom editors.
  * It is built on top of GEF and EMF, so the underlying platform is very similar to the current editor. No weird new dependencies or frameworks are needed.
  * It allows to separate advanced graphical modeling features from the underlying "business objects", a Passerelle flow with actors, director etc, in our case.
  * Many nice-to-have features are supported out-of-the-box or through simple customizations.
  * Active Graphiti community
  * Even though it's still not at a 1.x release level (current version for Eclipse Kepler is 0.10), it has been proven :
    * at SAP, the original source of graphiti development
    * in nice use cases like the eclipse BPMN2 modeler, JBPM5 modeler by JBoss and others

Further info related to Graphiti can be found on/from [the Graphiti project site](http://www.eclipse.org/graphiti/).
Their tutorial example (also documented in their on-line help available when you install graphiti in your eclipse) is a great intro.

## Approach ##
### High-level choices ###
  * Do a pilot for a basic model editor on Graphiti
  * Passerelle workbench will package the existing and the new editor together during a migration period
  * Existing Actor Palette tree-view and Actor Attributes view should work with both editors
  * We will use Graphiti with a non-EMF business model.
    * I.e. the Ptolemy/Passerelle flow, actors, director, relations etc are directly used as business model, as in the existing GEF-based editor.
    * But Graphiti adds a graphical model (EMF-based) with support for advanced layout mechanisms, decorations etc.
  * Some impact on existing code is acceptable (and should fix & refactor where appropriate).
  * The Graphiti-related new code will be in one or more separate bundles.

### Some details ###
  * No actors in the default Graphiti/GEF palette. Actors are dragged-n-dropped from the custom Actor palette tree-view.
  * A model will be stored in two files, a MOML and an XMI file with the graphical model. So the new editor will also generate&update the MOML file when saving a model.
  * Modeling elements supported in MOML should correctly reflect what has been defined in the graphical model. E.g. actor locations should be consistent.
  * As the workbench will initially contain both editors, there is a risk that someone directly edits the moml file using the old editor.
    * This will lead to inconsistencies with the graphical model.
    * So it would be good to be able to reconcile/regenerate/update the moml file from the graphical model file and vice-versa.
  * Graphiti uses _Diagrams_ containing hierarchical _Shapes_ and _GraphicsAlgorithms_ to build graphical representations of model elements. This encapsulates the GEF/draw2d _Canvas_, _Figures_ etc.
  * Typical editor actions like drag-n-drop, moving, copy/paste, delete etc are implemented via Graphiti _Features_ . These can be linked to standard key strokes, context menus etc.
  * Actor shapes should be overridable/configurable, within a standardized actor shape container. The container defines selection, layout boundaries, port locations etc.
  * By default Graphiti uses an on-demand update feature to apply "business model" changes to the graphical model. I.e. it marks model elements as "changed" and the user must trigger an update manually.
  * Graphiti itself is based on EMF, including its command framework.
    * This implies that the existing editor commands, being GEF-based, can not be reused on the Graphiti/EMF command stack.
    * The existing commands will be used indirectly from inside Graphiti _Features_. This guarantees undo/redo support as well.

## The pilot ##

### Scope ###
  * Learn graphiti
  * Tests for compatibility of new generated MOMLs with old editor and runtimes
  * Collect feedback from DAWN about
    * breaking changes/impact
    * inspiration for extra features
  * Focus on features and on overall look-n-feel; not on graphical details of individual actor shapes
  * Continuous refactoring during learning to ensure pilot code is the right basis for future extensions. I.e. no throw-away code at the end of the pilot!
  * Demonstrate a couple of extras that are possible via Graphiti and would be much harder in the old editor
    * Draw connections via direct dragging from an output port to an input port i.o. needing to select the connection tool from the GEF palette
    * Support for connection bendpoints
    * Direct editing of the actor name by double-clicking in the Actor name's _Text_ field.
    * Collapse/Expand actors :
      * (mandatory) For plain actors : show configurable parameters in expanded state / hide them in collapsed state.
      * (optional, if time available) For submodels : show submodel contents in expanded state / hide it in collapsed state.
    * Decorators :
      * (mandatory) mark actors that have model validation errors (cfr version validation in ModelValidationService)
      * (optional, if time available) mark actors that did some work during a model execution
  * As Passerelle's target platform for RCP dvp is still Juno SR2 (and not yet Kepler), we'll be using Graphiti 0.9.2 (and not yet 0.10).
    * This may change later on, as there is some info about 0.10 compatibility with Juno.

### Design ###
Overall, the pilot implementation will follow the Graphiti architecture. This implies the following implementation elements :
  * A whole bunch of _org.eclipse.graphiti.features.IFeature_ implementations, for adding/updating/moving/deleting/...
    * It is important to read the Graphiti docs to understand their naming. E.g. difference between create and add; between remove and delete etc.
    * Passerelle's features will be in package _com.isencia.passerelle.workbench.model.editor.graphiti.feature_
  * _com.isencia.passerelle.workbench.model.editor.graphiti.PasserelleDiagramFeatureProvider_ is the main place where our features will be plugged into Graphiti.
  * A binding between the graphical model and our _Flow_ with _Actors_ etc. This is implemented via :
    * _com.isencia.passerelle.workbench.model.editor.graphiti.model.PasserelleIndependenceSolver_ :
      * Strange graphiti naming ;-) , but an _IndependenceSolver_ maintains a (Key <=> BusinessObject) mapping for editors that have a non-EMF business model.
      * At initial creation time, graphical elements get linked with their corresponding business object(s) and store the linked key.
      * In this way, related business objects can be retrieved at any time based on that key.
      * We need unique immutable keys that can be registered and persisted in both the graphical model and in the Passerelle model.
      * Models generated with the Graphiti editor will contain UUID attributes for all main model elements. These will also be saved inside our MOMLs!
      * When opening an existing model, the (Key <=> BusinessObject) map must be reconstructed from the model files, before any editing work.
    * _com.isencia.passerelle.workbench.model.editor.graphiti.model.PasserellePersistencyBehavior_ :
      * This contains the actual saving and loading of the business model.
      * It is invoked at the right time by Graphiti, next to its own saving/loading of its graphical model.
    * _com.isencia.passerelle.workbench.model.editor.graphiti.model.DiagramFlowRepository_ :
      * The central place where all opened Flows&Diagrams are maintained and mapped.
      * In a situation with multiple open models, each editor's _Diagram_ is linked to its toplevel _Flow_ in this repository.
      * In this way each action done in the selected editor can target the right underlying Passerelle model.
  * _com.isencia.passerelle.workbench.model.editor.graphiti.PasserelleDiagramTypeProvider_ is the most import "bootstrap" element for Graphiti.
    * It is registered as an extension in our plugin.xml on _org.eclipse.graphiti.ui.diagramTypeProviders_
    * It registers the _IndependenceSolver_ and _Featureprovider_, initialises the _Flow_ for a new _Diagram_ etc.
  * _com.isencia.passerelle.workbench.model.editor.graphiti.PasserelleDiagramEditor_ to integrate the Actor palette and Attributes view
    * Graphiti's _DiagramEditor_ must be extended for our needs.
    * The editor is registered in our plugin as an extension on _org.eclipse.ui.editors_
    * Some elements may change into "behaviours", when upgrading to Graphiti 0.10, where extending the _DiagramEditor_ has become more modular.
    * This integration will require code modifications around the palette and attributes view, wherever they are currently tightly coupled to the old _PasserelleModel(MultiPage)Editor_.
    * The editor is also the driver for saving models, i.e. where our _PasserellePersistencyBehavior_ must be plugged in.
  * _com.isencia.passerelle.workbench.model.editor.graphiti.PasserelleToolBehaviorProvider_
    * It drives custom features in context menu and in context buttons
    * Double-click behaviour is also implemented in here
It is important to know that for each open diagram/flow, separate instances of most of the above classes are created (except for the _DiagramFlowRepository_ which is all static).

Finally, a new wizard is added : _com.isencia.passerelle.workbench.model.editor.graphiti.wizard.CreateDiagramWizard_
  * The wizard is registered in our plugin as an extension on _org.eclipse.ui.newWizards_

The pilot is developed in one new bundle _com.isencia.passerelle.workbench.model.editor.graphiti_.

### Status ###
What's the current status of the pilot?

#### August 22, 2013 ####
  * Running the model from inside the Graphiti editor : OK
  * Graphical/tree outline view : OK
  * Direct editing of actor name : OK
  * Misc things :
    * Copy/paste : OK
    * Delete : IN PROGRESS
      * This requires an adaptation of complex lookup logic from the Swing-based (or GEF-based) editor, to find all related objects that must also be deleted automatically.
      * E.g. when deleting an actor, all its incoming/outgoing connections must also be deleted
      * A first simple implementation is working.
    * FlowChangeListener to automatically update the diagram for flow changes done from elsewhere : OK
      * This removes the need for manual/on-demand update synchronization in the diagram as was described previously.
    * Automatically reopen the same model when restarting the workbench : OK
  * New/discovered things :
    * Undo/redo support : IN PROGRESS
      * Undo/redo currently only works on the diagram model, not yet towards the linked flow (i.e. as stored in the moml file).


#### August 16, 2013 ####
A very basic editor is working :
  * Wizard to create a new model from scratch : OK
  * D-n-D from Actor palette tree-view : OK
  * Actor configuration via double-click and/or attributes view : OK
  * Connecting ports : OK
  * Moving, reconfiguring should update underlying flow/MOML : OK
  * Free addition of bendpoints : OK
  * Collapse/Expand actors : IN PROGRESS
    * a first basic effect is working, hiding/showing parameters and minimizing actor shape
    * but nice shape adjustment to be finished
  * Running the model from inside the Graphiti editor : TO BE DONE
    * For the moment you can run from the MOML editor, i.e. the moml is correctly generated/maintained and works fine.
  * Submodels and drill-down : TO BE DONE
  * Direct editing of actor name : TO BE DONE
  * Decorators : TO BE DONE
  * Graphical/tree outline view : TO BE DONE

As a simple teaser, two screenshots of a same model :
  * The Graphiti-based "Hello World" with some heavy bendpoint steering, and the extensible context buttons pop-up for a selected actor :
![http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/Graphiti-based_HelloWorld.png](http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/Graphiti-based_HelloWorld.png)

  * The accompanying MOML file as opened in the GEF-based editor :
![http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/Gef-based_HelloWorld.png](http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/Gef-based_HelloWorld.png)


### Lessons learned ###

## Roadmap ##
  * Add support for configurable actor shape contents via a graphiti-compatible replacement for IActorFigureProvider
  * Add live status decorations during model execution
  * Integrate with new Passerelle runtime services for model storage and execution

