# Introduction #
> The goal of this page is to describe the concrete setup of an actor development project
> and how to package and deploy your new actors in the RCP workbench.


# Development and packaging of new Passerelle Actors #

## Actors as eclipse plugins for RCP and web ##
Since Passerelle v4.x, the web-based Passerelle Manager has been implemented as an OSGi-based Java enterprise application.
It has been possible since then to upload new/updated actor implementations as OSGi bundles, to allow their usage in the managed sequences.
But the actual design/definition of the sequences was then still done using the Java-Swing-based Passerelle IDE, which was a "plain" Java SE application.

Since Passerelle v5.5, the Passerelle Manager has been extended with a web-based graphical editor.
And with Passerelle v7.x, a functioning eclipse RCP Passerelle workbench has been added.
Both these tools now fully exploit OSGi and eclipse plugin features to develop and use Passerelle actors.

Furthermore these OSGi/eclipse-based tools have become the prefered ones to develop and execute actors and models, which has an impact on the approach to develop actors :
  * actors must be developed in eclipse plugins
  * the actors must be exposed via specific Passerelle extension points
  * the editor's actor palette is constructed based on the actors registered via the extension points
  * the actor implementation classes are loaded via a ModelElementClassProvider that must be registered as an OSGi service

This section will explain the development approach using eclipse's PDE as development tool.

## Setting up your workspace ##
TODO

## Creating an actor plugin project in eclipse ##

An actor plugin project can be easily started using the normal eclipse PDE wizard to create a plugin project.

A typical bundle MANIFEST is similar to the following (taken from the bundle _com.isencia.passerelle.actor.examples_ ) :
```
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: Actor dvp Examples
Bundle-SymbolicName: com.isencia.passerelle.actor.examples
Bundle-Version: n.n.n
Bundle-Vendor: ISENCIA
Bundle-RequiredExecutionEnvironment: JavaSE-1.6
Eclipse-RegisterBuddy: ptolemy.core
Import-Package: com.isencia.passerelle.actor;version="x.x.x",
 com.isencia.passerelle.actor.v5;version="x.x.x",
 com.isencia.passerelle.core;version="x.x.x",
 com.isencia.passerelle.ext;version="x.x.x",
 com.isencia.passerelle.message;version="x.x.x",
 com.isencia.passerelle.util;version="x.x.x",
 com.isencia.passerelle.validation.version;version="x.x.x",
 com.isencia.sherpa.commons,
 org.osgi.framework;version="1.3.0",
 org.slf4j,
 ptolemy.actor,
 ptolemy.actor.gui,
 ptolemy.actor.gui.style,
 ptolemy.data,
 ptolemy.data.expr,
 ptolemy.data.type,
 ptolemy.gui,
 ptolemy.kernel,
 ptolemy.kernel.util,
 ptolemy.moml
 ... any extra dependencies you may need for your custom actors ...
Export-Package: com.isencia.passerelle.actor.examples
Bundle-Activator: com.isencia.passerelle.actor.examples.activator.Activator
```

Important elements in there are :
  * Your bundle's symbolic name, following normal Java package/project naming conventions
  * Your bundle's version : replace n.n.n with your actual version
  * Eclipse-RegisterBuddy : Equinox-specific setting to allow Ptolemy's MomlParser to "see" your actor classes. Needed until a natively OSGi-enabled Ptolemy becomes available.
  * Passerelle dependencies : replace x.x.x version with your actual Passerelle versions
  * Export-Packages : list all your actor packages.
  * Bundle-Activator : if you use an Activator to register your actor classes (alternatively you could use OSGi Declarative Services)

## Registering your new actors ##

In an OSGi-based Passerelle runtime, actor implementation classes are made available by registering OSGi services that implement _com.isencia.passerelle.ext.ModelElementClassProvider_ .
This approach serves the following purposes :
  * Promote modular design, where groups of related actors can be developed and packaged and deployed as separate bundles
  * Benefit from the dynamic features and lifecycle offered by using OSGi services :
    * You can add actor bundles to a running Passerelle system and the actors become available dynamically.
    * Similarly actor maintenance can be done using standard OSGi bundle and service maintenance without system downtime.
  * Version management for actor implementations can be supported by the combination of OSGi bundle version management and the API of the _ModelElementClassProvider_ .
  * Additional actor lookup-logic can be plugged in as needed (e.g. class-name aliasing, filtering logic, ...)

In _com.isencia.passerelle.actor.examples_ , this is implemented via _com.isencia.passerelle.actor.examples.activator.ActorProvider_

```
public class ActorProvider implements ModelElementClassProvider {
  public Class<? extends NamedObj> getClass(String className, VersionSpecification versionSpec) throws ClassNotFoundException {
    if(className.startsWith("com.isencia.passerelle.actor.examples")) {
      return (Class<? extends NamedObj>) this.getClass().getClassLoader().loadClass(className);
    } else {
      throw new ClassNotFoundException();
    }
  }
}
```

that is then registered as a _ModelElementClassProvider_ service by the _com.isencia.passerelle.actor.examples.activator.Activator_

```
public class Activator implements BundleActivator {
  @SuppressWarnings("rawtypes")
  private ServiceRegistration apSvcReg;
  
  public void start(BundleContext context) throws Exception {
    apSvcReg = context.registerService(ModelElementClassProvider.class.getName(), new ActorProvider(), null);
  }
  public void stop(BundleContext context) throws Exception {
    apSvcReg.unregister();
  }
}
```

Such an _ActorProvider_ based on the bundle's classloading system is simple to write but has some performance issues in larger systems, as the classloader hierarchy
is traversed for the actor class lookup. The filter on the class-name-prefix is already an improvement, but not optimal either.

Since Passerelle v8.4 a more efficient _com.isencia.passerelle.ext.impl.DefaultModelElementClassProvider_ implementation is available that can be used as an alternative.
With this provider, all provided actor classes must be listed, and the above could be rewritten as follows :

```
public class Activator implements BundleActivator {
  @SuppressWarnings("rawtypes")
  private ServiceRegistration apSvcReg;
  
  @SuppressWarnings("unchecked")
  public void start(BundleContext context) throws Exception {
    apSvcReg = context.registerService(ModelElementClassProvider.class.getName(), 
        new DefaultModelElementClassProvider(
            AddRemoveMessageHeader.class,
            DelayWithExecutionTrace.class,
            Forwarder.class,
            HeaderFilter.class,
            HelloPasserelle.class,
            MultiInputsTracerConsole.class,
            RandomMessageRouter.class,
            TextSource.class
            ), 
        null);
  }
  public void stop(BundleContext context) throws Exception {
    apSvcReg.unregister();
  }
}
```

Remark that the above is also valid for when you want to add other types of model elements like new Director implementations.

## Packaging your new actor plugin(s) for your RCP workbench ##

To be able to add a new actor bundle to your RCP workbench, two extra things are required :
  * Define the palette-related meta-data for your new actors, mainly :
    * Group name(s) and actor names that should be used in the editor palette
    * Image/icon that must be used
  * Provide an eclipse feature project to package a deployable unit

The palette definition is done using extension points.
As this is purely GUI-related, and as it requires a "real" eclipse plugin, it is good practice to split this from the actual actor implementation bundle.

E.g. to register two example actors in one new group _"Example Actors"_ , we can provide a plugin _com.isencia.passerelle.actor.examples.conf_ with a _plugin.xml_
```
<plugin>
   <extension point="com.isencia.passerelle.engine.actorGroups">
      <actorgroup
            id="com.isencia.passerelle.actor.examples.actorgroup"
            name="Example Actors"
            open="false">
      </actorgroup>
   </extension>
   <extension point="com.isencia.passerelle.engine.actors">
      <actor
            class="com.isencia.passerelle.actor.examples.MultiInputsTracerConsole"
            group="com.isencia.passerelle.actor.examples.actorgroup"
            id="com.isencia.passerelle.actor.examples.MultiInputsTracerConsole"
            name="MultiInputsTracerConsole">
      </actor>
      <actor
            class="com.isencia.passerelle.actor.examples.DelayWithExecutionTrace"
            group="com.isencia.passerelle.actor.examples.actorgroup"
            id="com.isencia.passerelle.actor.examples.DelayWithExecutionTrace"
			icon="icons/time.png"
            name="DelayWithExecutionTrace">
      </actor>
   </extension>
</plugin>
```

and a trivial manifest with main purpose to declare the _singleton:=true_ constraint for a plugin

```
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: example actors Conf
Bundle-SymbolicName: com.isencia.passerelle.actor.examples.conf;singleton:=true
Bundle-Version: 1.0.0.qualifier
Bundle-Vendor: ISENCIA
Bundle-RequiredExecutionEnvironment: JavaSE-1.6
```

Finally, we still need to implement a feature project _com.isencia.passerelle.actor.examples.feature_
It should contain a _feature.xml_ that lists the contents of our new actor package, its main dependencies and some extra info like licensing etc.
Eclipse provides a custom editor for this, but the final result is just an xml file as follows :
```
<?xml version="1.0" encoding="UTF-8"?>
<feature
      id="com.isencia.passerelle.actor.examples.feature"
      label="Feature for example actors"
      version="8.3.0.qualifier"
      provider-name="ISENCIA">

   <description>
      This feature adds the Passerelle example actors to your workbench or runtime.
   </description>

   <copyright>
      Copyright 2013 - iSencia Belgium NV
   </copyright>

   <license url="http://www.apache.org/licenses/LICENSE-2.0">
      /* Copyright 2013 - iSencia Belgium NV
Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
or implied.
See the License for the specific language governing permissions
and
limitations under the License.
*/
   </license>

   <requires>
      <import plugin="com.isencia.passerelle.engine" version="8.3.0" match="greaterOrEqual"/>
      <import plugin="com.isencia.passerelle.actor"/>
      <import plugin="com.isencia.sherpa.commons.reduced"/>
      <import plugin="org.eclipse.osgi"/>
      <import plugin="slf4j.api"/>
      <import plugin="ptolemy.core"/>
      <import plugin="ptolemy.gui"/>
   </requires>

   <plugin
         id="com.isencia.passerelle.actor.examples"
         download-size="0"
         install-size="0"
         version="0.0.0"
         unpack="false"/>

   <plugin
         id="com.isencia.passerelle.actor.examples.conf"
         download-size="0"
         install-size="0"
         version="0.0.0"
         unpack="false"/>

</feature>
```

Once this is ready, we can simply export our new actor package e.g. to a .zip archive from the feature editor :

![http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/ActorFeatureExport.jpg](http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/ActorFeatureExport.jpg)

The result could be e.g. a _exampleactors.zip_ containing the _actor_ and _actor.conf_ bundles.

## Adding a new actor package to your workbench ##

The v8.4.0 workbench contains the required eclipse plugins to enable the update UI menu items.
From the menu _Software>Install New Software..._ the standard eclipse extensions installation wizard is opened.
There you can _Add..._ the exported zip-file as an archive-based repository.

Remark that no category was configured in our example, so you should deselect "Group items by category" to see your actor feature.
From there you can select it and follow the normal installation flow.

When the installation is done, your new actors should appear in the actor palette and are ready for use!

