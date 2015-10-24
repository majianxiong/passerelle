# Introduction #
> The goal of this page is to work on a guide that must ultimately be delivered as document/PDF/...
> So it's not appropriate to structure this as many Wiki pages with cross-links etc.
> It's better to build one complete page.



# Development guide for Passerelle Actors #

## Introduction ##

### Scope and intended audience ###
This development guide is intended for JavaTM developers that wish to start developing Passerelle actors.

After a short introduction about the concepts that are at the basis of the Passerelle tools suite, this guide delves straight into the details about actors, their lifecycle and their development.

For more information about the actual usage of Passerelle to define and execute data- and process flows in different settings, please consult the "Passerelle user guide" (in preparation).

This guide assumes a "medior"-level experience with JavaTM, with at least JDK 1.5 language compatability, and with Object-Oriented design and development (applied to JavaTM).
Basic principles of collections and generics should be known. Some experience with the techniques and issues involved in developing JavaTM-based solutions for concurrent systems and event-based systems is advisable.

### Document conventions ###
The Passerelle engine is to a large extent a wrapper around the underlying Ptolemy II framework (see below).
Many Passerelle concepts are directly inherited from Ptolemy.
But wherever something is discussed in this guide that is a Passerelle-specific extension or adaptation, it will be indicated by _Passerelle specific_.


---


## Interesting stuff to get prepared ##

It should be possible to develop fully working Passerelle actors, without bothering about the underlying technologies and vision. But it would be a pitty to not be aware of the interesting stuff mentioned below. Especially as the persons we should thank for creating these great ideas and technologies, have also made the effort to create some great documentation to support these! What's more, even ignoring any directly applicable use, the ideas behind these technologies (and the concrete execution of them) will enrich anyone's thinking on software-related problems and solutions.

So here we go :
  1. **Patterns of software architecture & design**, as documented in some reference works like :
    * "Pattern-oriented software architecture" aka POSA, Volume 1, by F. Buschmann et al.
    * "Design Patterns" aka GOF, by E. Gamma et al.
    * "Enterprise Integration Patterns" aka EIP, by G. Hohpe et al.
  1. **OSGi, the advanced and proven framework for modular Java solutions**, as documented in e.g. :
    * "OSGI Service Platform - Core Specification", which can be obtained from "http://www.osgi.org"
    * "OSGi and Equinox", by J. Mc Affer et al.
    * Many authors writing blogs with OSGi introductions, some of them referenced from "http://www.osgi.org/Links/BasicEducation"
  1. **Ptolemy II**, "an open-source software framework supporting experimentation with actor-oriented design", created at U.C. Berkeley by the "CHESS" research group of a.o. Professor E.A. Lee. Ptolemy comes with a couple of extensive guides, of which the following one is the perfect introduction :
    * Volume 1 : Introduction to Ptolemy II, by Chr. Brooks et al.


---


## Basic Concepts ##

### Ptolemy & actor-based assemblies/sequences ###

TBD

### Pipe-and-filter / Asynchronous processing / messages&events ###

Passerelle is a direct implementation of an architectural pattern called "Pipes and Filters" (see POSA - Volume 1, pg 53) :
> "Provides a structure for systems that process a stream of data. Each  processing step is encapsulated in a filter component.
> Data is passed through pipes between adjacent filters. Recombining filters allows you to build families of related systems."


The "Pipes and Filters" architecture fits well with dataflow systems where streams of data must be processed and/or transformed through a sequence of several processing stages.
When each stage or processing step gets a single well-defined responsibility, these can be implemented with low coupling and they become easy to reuse and to recombine in different scenarios or end-to-end process definitions. Filters also typically implement a common API, which ensures uniform development, and easy recombination.
Filters are easy to test since they have a clearly defined responsibility, they must be completely decoupled from each other and they have a uniform API.
So it is easy to also create uniform and reusable test tools.

**Filter????**
> The term "filter" is used in two different meanings. The "filter" in the architectural pattern is a generic processing component, without any a-priori assumption about its concrete functionality. In many other contexts (also in the library of Passerelle actors), "filters" denote components with a "filtering function", i.e. excluding subsets of data or messages from further processing.


Furthermore, such an architecture fits well with tools to define and configure executable processes in a graphical way.
When this is done, we often speak of "graphical models", typically representing the underlying solution with directed graphs.
When these models can be used as direct input to execute the corresponding process, we can talk about "executable models".

This in turn enables a clear split in roles for the delivery of software solutions, their operational usage and maintenance.
For example:
  * Functional experts/analysts can assemble solutions by picking, configuring and interconnecting filters from a library.
  * Application administrators can execute and monitor the processes, where the graphical view offers them visibility on what the processes actually do.
  * Application developers are responsible for implementing filters with clearly defined responsibilities.
  * Testers are mainly occupied with defining test data inputs and expected resulting outputs, and can take advantage of uniform test tools.

In Passerelle, the models are executable. The filters are implemented as actors. Each actor is in control of its own operations.
I.e. it decides when to read input data, how to process it and when to send output. This fits with the "Scenario IV" of POSA.

The pipes are the links/relations between the actors. Passerelle actors communicate by passing messages via their output ports across the configured links towards the input ports of other actors.
Links also act as dynamic buffers between actors, as they are based on FIFO queues, effectively decoupling the delivery of a message from its processing by the receiver.
In this way, Passerelle's processing is inherently asynchronous, and resilient to temporary local congestion.

The result is a flexible process execution system, resembling the operations of an "assembly line" in a factory.
Each actor corresponds to a resource, performing one step in the assembly of the final result.
At any time, many different "products" (messages) are traveling through the line, each at a different step in its process.
Local congestion is handled transparently by Passerelle's internal buffering (cfr. waiting lines).

This approach allows a high throughput with limited resources, and is resilient against temporary peak loads.

For an actor developer, all of the above is good news. It means that the developer can concentrate on the specific functionality of the actor, and can ignore most of the issues typically related to concurrent systems.

### Hierarchic models ###

Once the level of "Hello World"-like demos has been left behind, and the real-world problems must be tackled, the required processes rapidly increase in complexity. Secondly, during the implementation of different solutions in closely-related problem domains, reusable "sub-processes" may be discovered.

For both phenomena, it is of interest to be able to define processes as hierarchic models. The hierarchy can serve as a way to represent the process at different levels of abstraction and it allows easy reuse of predefined sub-processes.

In Ptolemy and Passerelle, hierarchic models are a native concept and can be fully exploited as needed, e.g. with an unlimited number of hierarchic levels.
The sub-processes (or "composite" actors) are represented and treated to a large extent in the same way as "simple" atomic actors, both internally and graphically.

### Passerelle and OSGi ###
_Passerelle specific_

Since Passerelle v4.x, the complete set of Passerelle tools has been converted to embrace OSGi as the core application framework.
Both the Passerelle tool itself, and extensions like new actor libraries etc., are now developed as a collection of OSGi bundles.
This allows the deployment of Passerelle-based solutions and tools in a well-managed way, in many different contexts.

For the actor developer, this implies that eclipse, with its PDE, is a natural choice as development IDE.
But it also means that some core concepts of OSGi must be known to successfully develop and deliver new actors.


---

## Model execution & Actor lifecycle ##

### Model start-up and shutdown ###
Passerelle offers several ways to launch a process model, e.g. using a shell script, from inside a graphical editor in a desktop application or via the web-based Passerelle Manager.

In each case, the actual execution process is the same, and goes through the following steps :
  1. Read the model from its definition, maintained in an XML file (typically with extension '.moml')
  1. Parse the file and construct all actors in it (including those contained in referred sub-models)
  1. Build all interconnections
  1. Trigger the actual execution via the so-called model _Director_. From here on, the Director controls the following steps.
  1. Start all required threads
  1. Initialize all actors
  1. Start the actor iterations

During their iterations, the actors wait until one or more messages (depending on the concrete needs for each type of actor) arrive on their input ports. When an actor has received sufficient data, it can process it and send out results, typically via one or more output ports. Then it will perform a next iteration, waiting again for the next messages to arrive etc.

As in any process graph, there is a special role for the "starting nodes". These are called _source actors_. Source actors do not receive their data via input ports, but get it from elsewhere, typically from "outside" the process. They then "inject" data messages in the model via their output ports and connections to the next actors in the model.

For example, a FileReader actor gets its data from a file and can then inject each text line from the file as a separate message into the model. Another type of source actor can be a SocketServer. This one opens a server socket and may inject data received from a socket client into the model.

Similarly, each model also has "end nodes". In many cases these actors send their results to an external system, or storage etc. These are called _sink actors_. For example, a MailSender actor may send a message across SMTP based on data received on its input port.

Depending on the actual actors used in a model, Passerelle is capable of automatically wrapping up an executing model, once it has "finished". Such an automatic decision on the termination of a model is not always possible. For a model with a SocketServer, there is no clear-cut rule that can determine when it can be "shut down". On the other hand, when a model has a simple FileReader as source, it seems logical that in most cases the model can terminate when the file has been completely processed.

_So, the source actors determine if/when a model's execution may be automatically terminated._

The actual shutdown process of a running model most be well managed. As Passerelle is fully asynchronous, there will typically still be work ongoing by actors further in the process, there may be messages pending in internal buffers etc. To guarantee complete treatment of all data, Passerelle includes a logic to gradually shut down the idle parts of a model, once it is certain that all preceding actors have finished, and all related input buffers are empty. In this way the automatic shutdown ripples through the model (becoming asynchronous itself), until all actors have finished. When all actors have finished, the Director shuts down the model as a whole.

An executing model may also be forced to stop, or be temporarily suspended until a resume is done, for example via some action in a UI.
Stop and suspend requests are broadcast to all actors in the active model. Actors should then terminate or interrupt their ongoing processing as soon as possible, after which the model will be shut down, respectively be suspended.

Contrary to the automated "ripple" shutdown, forced stop/suspend do not guarantee that all data is completely processed. It is only presumed that each actor finishes its ongoing work in a clean way, if possible.

### Std Passerelle director ###

TBD

### Actors : structure and lifecycle ###

Passerelle actors encapsulate configurable units of work, each with a specific responsibility and functionality, as part of a complete process definition.

Actors normally interact via sending messages to each other via an asynchronous send-and-forget communication system. Regular communication paths are explicitly defined in the process model, as interconnections between an output port of a sending actor and input port(s) of one-or-more receiving actors.

An actor can be configured by defining one or more actor attributes. Passerelle inherits a rich set of configuration features from Ptolemy, including dynamic updates, an expression language etc.

So an actor can be considered as a composition of 3 main types of elements :
  * one core actor object, representing the unit of functionality
  * a set of input and output ports that provide the communication paths to/from the actor functionality
  * a set of attributes that provide configuration elements for the actor

Inside a Passerelle model, each of these core elements is identified by a unique name. Furthermore, the naming reflects the hierarchic composition of a model.
For example, in a model named "HelloWorld", with a source actor named "Constant", and connected to a sink actor, named "Console", there are a.o. following named elements :
  * HelloWorld.Constant : the source actor
  * HelloWorld.Constant.value : the parameter of the source actor where we can configure the source data. E.g. a StringParameter with value "hello world".
  * HelloWorld.Constant.output : the output port of the source actor
  * HelloWorld.Console : the sink actor
  * HelloWorld.Console.input : the input port of the sink actor

The "named" nature of model elements is a core aspect of Ptolemy and thus of Passerelle. In fact "ptolemy.kernel.util.NamedObj" is the base class of almost all Ptolemy/Passerelle classes. A second aspect, inherited from Ptolemy, is the native support fot hierarchical structures. Passerelle elements can be contained in parent elements and can act as containers themselves as well. For example, actors can be contained in composite actors which can be contained in further composite actors etc. Actors contain ports and attributes. Ports are contained in their respective actors, but can contain attributes themselves. Etc.

Among these types of hierarchical model elements, actors are special as they can perform work during a model execution. To achieve this, actors must execute a predefined lifecycle, expressed via an API defined by "ptolemy.actor.Executable". As a consequence, an actor has following main phases in its lifecycle :
  1. Construction : this is where the actor is constructed as a Java object, after which it is linked into the containing model. After construction the actor is in an "idle" state, unable to perform any work yet.
  1. Ready but idle : When a model has been parsed, all actors are constructed and linked as defined. I.e. the model is "live" in memory. But until the model is started for execution, nothing is happening yet in any actor.
  1. Execution initialization : at the start of a model execution, all actors get the opportunity to perform some initialization logic, via an invocation of their _initialize()_ method. This is where required resources should be configured and/or acquired etc.
  1. Firing iterations : this is the main "active" time of an actor, where it repeatedly iterates between waiting for inputs, performing its processing logic and sending results on its output(s). Each iteration consists of three parts : _prefire(), fire(), postfire()_.
  1. Execution wrapping up : By returning a boolean _false_ from its postfire(), an actor indicates that it has finished its work as part of the executing model. The execution engine will then stop iterating that actor, and will invoke its _wrapup()_, where any post-execution cleanup can be done, resources can be released etc.
  1. Ready but idle again : an actor should be capable of being "re-executed", starting again from a clean initial state. A parsed model may remain present in the application's memory space, and it may be used for several consecutive executions. So an actor must return to a clean state before a next execution, by a combination of logic contained in _wrapup()_ and _initialize()_.

### Fast iterations versus slow/blocking iterations ; role of stopFire() ###

Many actors perform "finite" processing on the data they receive on their input ports. So their firing iterations cycle rapidly between waiting for input data.
But some actors may have slow or even blocking iteration logic, for example when they need to wait for some external event.

When models containing such slow/blocking actors must be stopped or suspended, this requires a means to interrupt the ongoing fire iteration. For this purpose, a _stopFire()_ method is present in the API. Well-behaved actors should either have "fast" iterations, or have a correctly working stopFire() that is able to perform a fast interrupt on an ongoing firing iteration.



---


## Actor development ##
### Hello Passerelle ###

As a first example of an actor implementation, we will present a simple transformer of text messages.
The actor will have one input port, one output port and a parameter to define the transformed text.
```
/* Copyright 2011 - iSencia Belgium NV

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.isencia.passerelle.actor.examples.hello;

import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.v5.Actor;
import com.isencia.passerelle.actor.v5.ActorContext;
import com.isencia.passerelle.actor.v5.ProcessRequest;
import com.isencia.passerelle.actor.v5.ProcessResponse;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.message.ManagedMessage;

/**
 * A first actor : change the text in a received message.
 *
 */
public class HelloPasserelle extends Actor {
  
  public Port input;
  public Port output;
  
  public Parameter changedTextParameter;

  /**
   * @param container
   * @param name
   * @throws IllegalActionException
   * @throws NameDuplicationException
   */
  public HelloPasserelle(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);
    // In case of a single input port, this shortcut factory method can be used.
    // It creates a blocking port with name "input".
    // The String.class as second parameter guarantees that the received messages will contain a String,
    // or will be transformed into a String by Passerelle's automated type conversion chain.
    input = PortFactory.getInstance().createInputPort(this, String.class);
    // In case of a single output port, this shortcut factory method can be used.
    // It creates a port with name "output".
    output = PortFactory.getInstance().createOutputPort(this);
    // A Parameter gets a name (2nd parameter below), which is also used to automatically generate configuration forms.
    // Different specific Parameter classes correspond to different specific form widgets (e.g. text field, combobox, file chooser etc.)
    changedTextParameter = new StringParameter(this, "Changed text");
    // Set the default value.
    changedTextParameter.setExpression("Hello Passerelle");
  }

  @Override
  protected void process(ActorContext ctxt, ProcessRequest request, ProcessResponse response) throws ProcessingException {
    // Get the message received on the input port, from the request.
    ManagedMessage receivedMsg = request.getMessage(input);
    try {
      // We know the message will contain a String in its body.
      String receivedText = (String) receivedMsg.getBodyContent();
      // Get the configured value for the changed text.
      String changedText = changedTextParameter.getExpression();
      // Create a new outgoing msg, "caused by" the received input msg
      ManagedMessage outputMsg = createMessageFromCauses(receivedMsg);
      outputMsg.setBodyContentPlainText("Changed ["+receivedText+"] to ["+changedText+"]");
      // Set the outgoing msg to be sent on the output port
      response.addOutputMessage(output, outputMsg);
    } catch (Exception e) {
      // When something failed, throw a ProcessingException which will be handled as needed
      // by Passerelle's default error handling mechanisms.
      throw new ProcessingException("Failed to transform the received text", receivedMsg, e);
    }
  }
}
```

Such an actor can be used in a small variation on the classic "Hello World", adapted to Passerelle :

![http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/HelloPasserelleModel.jpg](http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/HelloPasserelleModel.jpg)

When this model is launched, the tracer-log-view will show the transformed message :

![http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/HelloPasserelleResult.jpg](http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/HelloPasserelleResult.jpg)



### General actor design principles ###

#### Don't develop new actors ####

First check the libraries of existing ones.

#### Understand the Passerelle ManagedMessage ####
Actors communicate across relations/channels between their ports, by sending messages. In Ptolemy each message is wrapped in an instance of a (subtype of) ptolemy.data.Token. Passerelle standardizes on a Token containing an instance of com.isencia.passerelle.message.ManagedMessage.

A ManagedMessage contains :
  * system headers : managed by Passerelle. They serve to add meta-data to a message like a message ID, grouping keys, correlation IDs etc.
  * a body consisting of body headers and content : for usage by the actors

Headers are simple String-based key/value pairs.
Actors can use the body headers to tag messages with meta-data that may be used later in sequences for easy filtering, routing etc.

The message body is implemented based on javax.mail.internet.MimeBodyPart, with support for contents with different MIME types.
In general, the body is used to store and transport Java objects between actors.

For simple scenarios, these could just be Strings. This has the advantage that it is easy to build a large collection of reusable actors, performing all kinds of operations on Strings. For such cases, the message content can be set with ManagedMessage.setBodyContentPlainText(). This was the case in the HelloPasserelle example above.
```
...
      // Create a new outgoing msg, "caused by" the received input msg
      ManagedMessage outputMsg = createMessageFromCauses(receivedMsg);
      outputMsg.setBodyContentPlainText("Changed ["+receivedText+"] to ["+changedText+"]");
      // Set the outgoing msg to be sent on the output port
      response.addOutputMessage(output, outputMsg)
...
```

An actor receiving such a message can also easily obtain the String content as follows (also from HelloPasserelle) :

```
...
  @Override
  protected void process(ActorContext ctxt, ProcessRequest request, ProcessResponse response) throws ProcessingException {
    // Get the message received on the input port, from the request.
    ManagedMessage receivedMsg = request.getMessage(input);
    try {
      // We know the message will contain a String in its body.
      String receivedText = receivedMsg.getBodyContentAsString();
      ...
    } catch (Exception e) {
      ...
    }
  }
...
```


But in most real-life scenarios, ManagedMessages contain instances of a custom domain model. For such scenarios, also specific actor libraries must then be developed that are aware of the domain model.

```
...
      Foo someObject = ....;
      ...
      // Create a new outgoing msg, "caused by" the received input msg
      ManagedMessage outputMsg = createMessageFromCauses(receivedMsg);
      outputMsg.setBodyContent(someObject,  ManagedMessage.objectContentType);
      ...
...
```

An actor receiving such a message can obtain the contents as follows, casting to the expected object class :

```
...
  @Override
  protected void process(ActorContext ctxt, ProcessRequest request, ProcessResponse response) throws ProcessingException {
    // Get the message received on the input port, from the request.
    ManagedMessage receivedMsg = request.getMessage(input);
    try {
      // We know the message will contain a Foo in its body.
      Foo receivedObject = (Foo) receivedMsg.getBodyContent();
      ...
    } catch (Exception e) {
      ...
    }
  }
...
```

In the examples above, the method Actor.createMessageFromCauses(ManagedMessage... msgs) creates a new message instance, with a reference to the IDs of the msgs passed as arguments. These are maintained as "causes IDs". It is good practice to indicate all related input messages as "causes" for outgoing messages, to support (future) causality analyses and other advanced analysis services on Passerelle messages.

#### Define a domain model for the message payloads ####

As mentioned above, Passerelle messages typically serve as envelopes for Java objects, enriched with metadata. The metadata can be related to Passerelle's internal handling (in the system headers) and/or can be added by specific actors (in the body headers).

In simple scenarios, a combination of plain-text message contents with the usage of body headers may be sufficient to build complete solutions.
Many typical messaging patterns (cfr EIP) like content-based routing, fork/join, ... can be implemented on this simple basis.

However, once the data structures involved become too big, or the logic that must be applied too complex, a real domain model is advisable.
This can then be implemented with plain Java classes or with any desired approach (e.g. JPA entities etc.). Passerelle imposes no constraints on the types of objects that can be passed around.

The usage of a domain model implies the creation of a set of domain-specific actors that can take advantage of the defined model. When actors represent domain-specific logic applied on domain objects, the resulting models typically become simpler and more understandable, also for less-technical persons like domain experts.

Often it even becomes possible to perform joint solution modeling by IT- and domain experts, using Passerelle to express the definition of the requested business processes. This has the major advantage that the resulting models are directly executable!

The usage of a domain model for the message flow means that there is a common lingua franca between the actors in the model.
Typically source and sink actors must then be take the responsibility to transform data from/to outside the model into the desired domain model.

For specialized cases, one can take advantage of Passerelle's TypeConverter service. It contains a configurable chain of TypeConverters that can automatically convert message contents into required types defined on the receiving input ports. See "Specialized Topics" fore more info.

#### Define the smallest possible units of reusable logic ####
When designing solutions based on Passerelle actors, one may feel that one has to compromise between actor complexity and model complexity.

The simplest model may contain just one actor, which contains all processing logic.
This approach is sometimes referred to with the friendly denomination "big ball of mud", and it destroys any possible benefit of an actor-based solution design.

The simplest actors have very limited functional behaviour. A complete process definition may then require a model with many actors, but this option is always preferrable to the big-ball-of-mud!

By exploiting the hierarchic features of Passerelle modeling, it is up to the _model designer_ to decide how to combine simple actors into reusable sub-models!
Only in very rare and specific situations, it is sometimes of interest to build complex logic, performing multiple processing steps, into a single actor from the start.
But in general, the choice on how to aggregate simple functionalities into bigger units-of-work should not be made during the actor development.

#### Delegate implementation of the logic ####
For all cases except the really trivial ones, an actor should be considered as just an integration component between the "control" layer and the "service" layer.
  * The control layer is provided by the Passerelle engine and the collection of active models.
  * The service layer is where the functionality is really implemented, without any dependency on Ptolemy/Passerelle types etc.

The responsibility of an actor is then limited to :
  * collecting sufficient info from its inputs and passing it into the right service
  * collecting results from the service and sending them out on the right output ports
  * collecting service errors and reporting them via the dedicated approach foreseen in Passerelle _Passerelle specific_

As a consequence the actor development becomes straightforward, once the service layer is defined.

#### Actor port types ####
_Passerelle specific_

Passerelle offers specialized types of ports in two different ways :
  * Data ports versus Control ports and Error ports
    * Data ports are the "plain" kind of ports that can be defined by actor developers as input and output ports.
    * Control ports are created automatically on each Passerelle actor :
      * requestFinish input : can be used to force an actor to finish its iterations and to wrap up, by sending an arbitrary message on this port.
      * hasFired output : generates a simple trigger message with arbitrary content, each time the actor has performed one firing iteration
      * hasFinished output : generates a simple trigger message with arbitrary content, when the actor has finished its wrapup
    * Error output port is created automatically on each Passerelle actor.
      * Errors in an actor's processing are sent via this port as PasserelleException objects with full context information. By connecting this port to dedicated error handling errors, models can be enriched with integrated error handling.
  * Blocking versus non-blocking input ports
    * Blocking input ports should be used to define input channels for mandatory messages. Each actor firing iteration requires one message from each blocking input port. These ports correspond to PortMode.PULL, which is the default.
    * Non-blocking ports should be used for optional messages. Actor firing iterations do not block/wait for incoming messages on such ports.
These ports use PortMode.PUSH, corresponding to the fact that messages arriving on non-blocking ports are pushed to a buffer, from where they can be retrieved during a next firing iteration.

Blocking input ports are the default in the Ptolemy "Process Networks" domain, which is the basis of Passerelle's runtime. In fact, blocking ports are the _only_ option available there.

Non-blocking ports are implemented in Passerelle, as an extension on Ptolemy, in two different ways :
  * Legacy approach : a Passerelle actor creates an extra thread per input channel of each non-blocking port. These threads wrap the blocking message retrieval and then push the received messages to an internal buffer. This is implemented via PortHandlers.
  * New approach since Actor v5 API (see below) : Passerelle overrides the default internal design of Ptolemy and implements a direct feed from each channel to the internal buffer, without requiring extra threads.

#### Consequences of asynchronous model execution ####

TBD



---

### Standard actor life-cycle & API ###
-- template methods in Passerelle Actor base-class
-- conventions for parameters/ports, constructor, doPreFire(), doFire(), doPostFire(), doWrapup(), doStopFire(), doStop()....
-- some usefull base classes
-- actors & threads
-- examples : source, sink, transformer, filter

### Passerelle's simplified v5 Actor API ###
The "v5 Actor API" in com.isencia.passerelle.actor.v5.Actor should be the preferred starting point for all actor development. Only in rare, very specific cases, should it be necessary to base one's actor development on the more complex com.isencia.passerelle.actor.Actor, following more closely the native Ptolemy Actor API as described above.

The HelloPasserelle actor above has already illustrated the basic elements of an Actor, based on the v5 API. Here we will first describe some of the concepts in this API and then present some more advanced examples.

#### API concepts ####
The actor development is centered around the Actor.process() method, with three parameters :
  * ActorContext : this is currently just a place holder. In the future this will be the access point to obtain state information about the actor.
  * ProcessRequest : contains all received messages since the previous iteration, mapped to the input port on which they were received.
  * ProcessResponse : where the actor should assign outgoing messages to the corresponding output ports.

When an actor needs to perform some specific initialization logic, for example allocating some resource for its processing needs,
this can be done by overriding Actor.doInitialize(), similarly as described in the previous section. Typically this also requires custom cleanup logic, which can then be implemented in an overridden Actor.doWrapup(). One must take care to not forget the call to super.doInitialize() and super.doWrapup() in such cases!
```
	@Override
	protected void doInitialize() throws InitializationException {
		super.doInitialize();
		try {
                  ....
		} catch (Exception e) {
		  throw new InitializationException("Error initializing my actor", this, e);
		}
	}
	...
	@Override
	protected void doWrapUp() throws TerminationException {
		try {
		  ...
		} catch (Exception e) {
		  throw new TerminationException("Error wrapping up my actor", this, e);
		} finally {
		  super.doWrapUp();
		}
	}
```

#### MessageBuffer and the modified threading model ####
In native Ptolemy, in the domain used for Passerelle, all input ports are blocking. Even worse, each input channel (i.e. each incoming link into an input port) is working in this way. The reason is that each channel has its own blocking queue at the receiver side, that must be read individually by the containing actor.

This implies that multi-input actors should receive a message on each of their input channels, for each firing iteration. This severely limits the usage of multiple inputs, and that's why Passerelle has introduced the concept of "PUSH" ports, with the accompanying PortHandlers. These are internal components, handling their own thread for each input and pushing each received message to an internal queue. This approach provides a flexible usage of multi-input actors, but leads to a rapidly increasing number of required threads for complex models.

With the "v5" actors, this threading problem has been resolved, by introducing a central MessageBuffer per actor where all PUSH input channels directly deliver the received messages. This MessageBuffer replaces the multiple blocking queues in the "native" Ptolemy domain implementation.

This changed message queueing approach is completely encapsulated within the v5 Actor API. The actor developer must decide which ports should be blocking (PULL) and which non-blocking (PUSH), to identify the "input conditions" before a next iteration can be done. I.e. the process() method will only be invoked when at least one message has been received on each blocking/PULL port. And any messages received on PUSH ports in the meantime are also offered in the ProcessRequest.

Remark that it is now possible that multiple messages, received on the same PUSH port(s) between iterations, must be processed in one process() invocation.

#### Using parameters & ports ####

**Parameters and Ports should be declared as public actor properties**\\
This is required by Ptolemy's internally used cloning mechanisms. These are triggered e.g. when dragging&dropping a submodel from the library/palette on the model canvas. If a port is not declared as public, this may result in lost connections to/from this port, at runtime, when used in submodels.

**Ports should be constructed using the Passerelle PortFactory**\\
This is an easy and safe way to construct ports of the right type and that get correctly initialized.

When designing actors, one must decide about what must be made configurable, as opposed to being dynamic (i.e. based on the incoming data) :
  * configurable items : these can be defined/changed before each model execution, for example using a Passerelle GUI. During the model execution they will normally remain stable.
  * dynamic items : these are derived from data in the received messages. So they can change with each actor firing iteration.

Parameters should be created to represent the configurable items. Ptolemy supports extreme dynamism, also for run-time changes in parameter values.
But this dramatically reduces the transparency & understandability of a model execution, so this is strongly dissuaded for Passerelle.

Ports are the entry points to deliver data to an actor, corresponding to a specific function for this data. Actors with their ports can be compared to electronic components where each pin has a certain unique function. So it is good practice to design actors with multiple input ports, when a clear split can be defined for the functions that the actor will perform on the incoming messages.

Stated differently, multiple ports allow to easily differentiate the incoming and outgoing message streams according to their function. From the modeler's perspective, the ports also allow to easily differentiate the flows within the model, for the different message streams.


#### Typical implementation for process() ####
Typically, an actor will first read the received messages, then perform some processing on the contained information, and finally send results via one or more output ports.

Reading messages can be done in two ways :
  * Get the last obtained message on a specific port.
  * Iterate over all received messages

The first approach can work both based on the port's name or using the port instance itself as a key :
```
  public Port input1;
  public Port input2;
  ...
  
  public SomeActorWith2Inputs(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);
    input1 = PortFactory.getInstance().createInputPort(this, String.class);
    input2 = PortFactory.getInstance().createInputPort(this, "inputPort2", String.class);
  }
  
  @Override
  protected void process(ActorContext ctxt, ProcessRequest request, ProcessResponse response) throws ProcessingException {
    ManagedMessage receivedMsg1 = request.getMessage(input1);
    ManagedMessage receivedMsg2 = request.getMessage("inputPort2");
	...
  }
```

For the second approach, a classic iteration loop can be done as in :
```
  @Override
  protected void process(ActorContext ctxt, ProcessRequest request, ProcessResponse response) throws ProcessingException {
	Iterator<MessageInputContext> inputContexts = request.getAllInputContexts();
	while (inputContexts.hasNext()) {
		MessageInputContext inputContext = (MessageInputContext) inputContexts.next();
		String portName = inputContext.getPortName();
		ManagedMessage msg = inputContext.getMsg();
		...
	}
  }
```

For simple actors, with only PULL/blocking input ports, the first approach is the most straightforward.
In more dynamic or complex cases (e.g. with PUSH ports), where it count of received messages is not predictable, the second approach is preferable as it guarantees that all received messages are retrieved for processing.

Once the actor has results available, they are normally sent via one or more output ports.
It is good practice to create new ManagedMessage instances in each actor that is sending outputs,
and to correlate the outgoing message(s) to the received ones that have "caused" the obtained results.
Passerelle's Actor base classes provide some utility methods to get this done.
```
      ManagedMessage outputMsg1 = createMessageFromCauses(receivedMsg1,receivedMsg2);
      ManagedMessage outputMsg2 = createMessageFromCauses(receivedMsg1,receivedMsg2);
	  // set the contents of the outgoing messages
	  ...
      response.addOutputMessage(output1, outputMsg1);
      response.addOutputMessage(output2, outputMsg2);
```

#### Some examples ####

##### Transformer #####
Transformers are a category of actors that typically have one blocking input port and one output port.
They send outgoing messages with contents based on some transformation logic applied on the information contained in the received messages.

Whereas in many existing data-flow/messaging systems, transformers have an important role to translate external data structures into an internal "canonical" one,
this is not often done using actors, in Passerelle. Such "technical" transformations are typically integrated in Passerelle's Type Convertor mechanism (see below).
This is especially the case if they must be applied in many different sequences sharing a common internal messaging format.

The HelloPasserelle actor above is an example of a very simple transformer. In this section, we will introduce a transformer that adds or removes a body header, and leaves the message body untouched.

```
/* Copyright 2011 - iSencia Belgium N

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.isencia.passerelle.actor.examples;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.ValidationException;
import com.isencia.passerelle.actor.v5.Actor;
import com.isencia.passerelle.actor.v5.ActorContext;
import com.isencia.passerelle.actor.v5.ProcessRequest;
import com.isencia.passerelle.actor.v5.ProcessResponse;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageFactory;

/**
 * A transformer actor that adds or removes a header to a received message.
 *
 */
public class AddRemoveMessageHeader extends Actor {

  private final static String MODE_ADD = "Add";
  private final static String MODE_REMOVE = "Remove";

  public Port input;
  public Port output;
  
  public StringParameter headerNameParameter;
  public StringParameter headerValueParameter;
  public StringParameter modeParameter;

  /**
   * @param container
   * @param name
   * @throws IllegalActionException
   * @throws NameDuplicationException
   */
  public AddRemoveMessageHeader(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);
    input = PortFactory.getInstance().createInputPort(this, String.class);
    output = PortFactory.getInstance().createOutputPort(this);
    
    headerNameParameter = new StringParameter(this, "Header name");
    headerValueParameter = new StringParameter(this, "Header value");
    modeParameter = new StringParameter(this, "Mode");
    // parameters can be configured with a list of options/choices
    // such parameters are rendered automatically with a combo-box
    modeParameter.addChoice(MODE_ADD);
    modeParameter.addChoice(MODE_REMOVE);
    // default value is Add
    modeParameter.setExpression(MODE_ADD);
  }
  //...
```
The actor now has three parameters. A header name and value can be specified as text values. The Add/Remove options are set as choices on the Mode parameter.
Passerelle actors can validate their initialization state during the startup of a model, before the actor iterations are allowed to begin.
```
  //...
  /**
   * An illustration of validating the parameter settings.
   * E.g. for the mode parameter, we expect either Add or Remove,
   * but this constraint can not be enforced with 100% certainty in model files.
   * So we can check it again here.
   */
  @Override
  protected void validateInitialization() throws ValidationException {
    super.validateInitialization();
    
    String mode = modeParameter.getExpression();
    if(!MODE_REMOVE.equalsIgnoreCase(mode) && !MODE_ADD.equalsIgnoreCase(mode)) {
      throw new ValidationException("Invalid mode "+mode, this, null);
    }
    
    String headerName = headerNameParameter.getExpression().trim();
    if(headerName.length()==0) {
      throw new ValidationException("Undefined header name", this, null);
    }
  }
```

Once the actor iterations have started, the process() method will be invoked when a message has been received on the input port.
As the input port is a blocking/PULL one, each received message will cause a separate (sequential) process() invocation.
It is good practice to create new message instances for outgoing messages, and not send out (modified) received messages just like that.
In fact, to prevent any risk for concurrency issues, received messages should be considered immutable.

Below, the actor first makes a copy of the received message and indicates that it is caused by the received message.
The configured header modification is then applied on the copy, and this one will be sent out.
```
  @Override
  protected void process(ActorContext ctxt, ProcessRequest request, ProcessResponse response) throws ProcessingException {
    ManagedMessage receivedMsg = request.getMessage(input);
    try {
      // Create a new outgoing msg, "caused by" the received input msg
      // and for the rest a complete copy of the received msg
      ManagedMessage outputMsg = MessageFactory.getInstance().copyMessage(receivedMsg);
      outputMsg.addCauseID(receivedMsg.getID());
      
      String headerName = headerNameParameter.getExpression();
      String headerValue = headerValueParameter.getExpression();
      String mode = modeParameter.getExpression();
      
      if(MODE_ADD.equalsIgnoreCase(mode)) {
        outputMsg.addBodyHeader(headerName, headerValue);
      } else if(MODE_REMOVE.equalsIgnoreCase(mode)) {
        outputMsg.removeBodyHeader(headerName);
      } else {
        // should not happen, at least if the initialization validation is active.
        getLogger().warn("Invalid mode "+mode);
      }
      response.addOutputMessage(output, outputMsg);
    } catch (Exception e) {
      throw new ProcessingException("Failed to add/remove a header", receivedMsg, e);
    }
  }
}
```

The actor's parameters can be configured via an automatically generated panel, e.g. in the Swing HMI :

![http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/AddRemoveMsgHdrConfigPanel.jpg](http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/AddRemoveMsgHdrConfigPanel.jpg)

Running this model will result in a final message being sent to the TracerConsole with the added header.
From the logs we can see that indeed a header _MyHeader_ was added in the _Body_ :
```
<Message>
  <Header name="__PSRL_CAUSES_IDS" value="2" />
  <Header name="__PSRL_SRC_REF" value=".HelloWithTransformer.Constant" />
  <Header name="__PSRL_ID" value="4" />
  <Header name="__PSRL_VERSION" value="1" />
  <Header name="__PSRL_TIMESTAMP_CREATION" value="25-okt-2011 21:23:07" />
  <Body>
    <Part>
      <Header name="Content-Disposition" value="inline" />
      <Header name="MyHeader" value="something" />
      <Body>Hello world</Body>
    </Part>
  </Body>
</Message>
```

##### Filter actors #####
A _Filter_ actor is an implementation of EIP's _Message Filter_ pattern. An EIP _Message Filter_ typically evaluates incoming messages according to some boolean condition. If the condition evaluates to _true_, the message is sent along in the sequence, and if it's _false_ the message is dropped.

Whereas in the plain world-of-EIP a pure Message Filter is a useful concept, this is not the case in typical Passerelle use cases. Messages should not be dropped, but they should potentially be processed in alternative ways. For this purpose, filter actors typically provide two output ports : a MATCH and a NOMATCH. When the filter condition evaluates to _true_, the message is sent out via MATCH, if it's _false_ it is sent out via NOMATCH. Such a need is typically addressed in EIP by using a _Message Router_. A filter can be considered as a "degenerate" case of a router with only two options, controlled by a boolean condition. In the rare cases where "false" messages can be dropped, this just means that the NOMATCH output port will not be connected to a next actor.

As we can imagine many different filter implementations, all sharing a common basic structure, we will start by defining a base class for filter actors. The base class will have one input port and two output ports (match,noMatch) and an abstract method to implement a boolean condition on each received message. To improve the model's clarity and the searchability of execution traces, we will allow to rename the output port names to clearly identify the meaning of the condition. For example, a filter that checks for the presence of a VOIP service on a telecommunications line could get output ports _voip_ and _noVoip_. Another actor checking for the presence of a header "MyHeader" could have output ports _hasMyHeader_ and _noMyHeader_ etc.

We start with an abstract Filter actor with one input port, two output ports and parameters to set the names of the output ports.
```
public abstract class Filter extends Actor {
  public Port input;
  public Port outputMatch;
  public Port outputNoMatch;
  public StringParameter outputMatchPortNameParameter;
  public StringParameter outputNoMatchPortNameParameter;

  /**
   * @param container
   * @param name
   * @throws IllegalActionException
   * @throws NameDuplicationException
   */
  public Filter(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);

    // create the ports with their default names
    input = PortFactory.getInstance().createInputPort(this, null);
    outputMatch = PortFactory.getInstance().createOutputPort(this, "match");
    outputNoMatch = PortFactory.getInstance().createOutputPort(this, "noMatch");
    // provide parameters to be able to customize the output port names
    outputMatchPortNameParameter = new StringParameter(this,"Name for match output");
    outputMatchPortNameParameter.setExpression(outputMatch.getName());
    outputNoMatchPortNameParameter = new StringParameter(this,"Name for noMatch output");
    outputNoMatchPortNameParameter.setExpression(outputNoMatch.getName());
  }
  //...
```

Ptolemy (and thus Passerelle) provides a callback mechanism on actors to be notified about any change in parameter values.
The method is called _attributeChanged()_. It can be used here to adapt a port's visible name when a model designer has changed the corresponding parameter.
```
  /**
   * Each time an actor attribute is changed, the actor is notified about this via this callback,
   * containing the changed attribute.
   * <br/>
   * By checking if the attribute is one of our defined parameters, the impact of the change
   * can be assessed and the actor can react accordingly, e.g. as in this case by renaming a port.
   */
  @Override
  public void attributeChanged(Attribute attribute) throws IllegalActionException {
    if(outputMatchPortNameParameter==attribute) {
      // The port's display name is what is made visible in the graphical editors,
      // and what is used in the log files to trace message flows etc.
      // The normal name of a model element serves as unique identifier in a Passerelle model,
      // and is used to refer to an element when needed. 
      // E.g. a channel between two actors refers explicitly to the names of the linked output port and input port.
      // So changing it risks breaking a model's referential integrity
      // A port's display name can be freely changed, without breaking anything.
      outputMatch.setDisplayName(outputMatchPortNameParameter.getExpression());
      // To ensure that a changed display name is made visible in the graph editor,
      // this call is required.
      outputMatch.propagateValues();
    } else if(outputNoMatchPortNameParameter==attribute) {
      outputNoMatch.setDisplayName(outputNoMatchPortNameParameter.getExpression());
      outputNoMatch.propagateValues();
    } else {
      super.attributeChanged(attribute);
    }
  };
```

We use the fact that each model element in Ptolemy/Passerelle is a specialization of _ptolemy.kernel.util.NamedObj_. A NamedObj has a fully-qualified hierarchical name, uniquely identifying each element in a hierarchic model (as already described previously). For a Port this name can not easily be changed as relations/channels between actors explicitly refer to the Port name in the model definition (e.g. as visible in the MOML file). But what can be changed at will is the so-called _displayName_. This is the name that is effectively shown in the UIs, and used in execution logs to trace message flows. By default it is the same as the "normal" name, but it can be explicitly set to something different as shown in this example.

> In Ptolemy's Vergil tool, it is possible to rename all ports via a context-menu that can be opened with a right-mouse-button-click on a port.
> However this action is not supported in other editors like the eclipse workbench or the web-based editor.
> In Passerelle, it is generally preferable to explicitly model all configuration elements via parameters as shown in this abstract Filter.


Finally, the actual skeleton for the filter condition can be implemented as below. The actor's _process()_ method passes the received message to an abstract boolean method _isMatchingFilter()_ and based on the result forwards the message via the _outputMatch_ or _outputNoMatch_ output ports.

```
  @Override
  protected void process(ActorContext ctxt, ProcessRequest request, ProcessResponse response) throws ProcessingException {
    ManagedMessage inputMsg = request.getMessage(input);
    try {
      if(isMatchingFilter(inputMsg)) {
        response.addOutputMessage(outputMatch, inputMsg);
      } else {
        response.addOutputMessage(outputNoMatch, inputMsg);
      }
    } catch (ProcessingException e) {
      throw e;
    } catch (Exception e) {
      throw new ProcessingException("Error matching filter for "+getFullName(), inputMsg, e);
    }
  }
  
  /**
   * 
   * @param msg the message to be filtered
   * @return true/false depending on whether the implemented filter condition matches the message or not
   * 
   * @throws Exception
   */
  protected abstract boolean isMatchingFilter(ManagedMessage msg) throws Exception;
}
```

A simple example of a Filter implementation is given below. It checks for the presence of a header field with name as defined in the filter's parameter.

```
public class HeaderFilter extends Filter {

  public StringParameter headerNameParameter;

  /**
   * @param container
   * @param name
   * @throws IllegalActionException
   * @throws NameDuplicationException
   */
  public HeaderFilter(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);
    headerNameParameter = new StringParameter(this, "Header name");
    headerNameParameter.setExpression("MyHeader");
  }

  /**
   * Check if the header with the configured name is present.
   */
  @Override
  protected boolean isMatchingFilter(ManagedMessage msg) throws Exception {
    return msg.getBodyHeader(headerNameParameter.getExpression()) != null;
  }
}
```

This actor can be used in an extension of the model with the _AddRemoveMessageHeader_ above, to check if "MyHeader" was correctly added.
When the filter matches, the message can be routed to the TracerConsole as before.
When the filter does not match, we can now send the message to an ErrorConsole to notify someone that the header was not correctly set.

![http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/HdrFilterConfigPanel.jpg](http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/HdrFilterConfigPanel.jpg)

The screenshot also shows the configuration panel where the port names can be specified, together with the header name for which the filter will check the presence.


##### Source #####

##### Sink #####

##### A multi-input actor #####


## Specialized topics ##

- Parameters and Ptolemy's expression language
-- StringParameter.getExpression() vs getToken().toString()
- Error handling
-- Passerelle exceptions per action method
--- what to put in the exception's context
- Parameter visibility/configurability levels
-- config-tool
-- IDE
-- expert mode
- Constraining message content types on input ports
-- ...
- Type convertor chain
-- adding custom type convertors to Passerelle
- Message sequences and their applications
-- mechanism for distributing work and regrouping results
-- ...
- A simple task-based entity model
-- ...
- Logging configuration policy
-- reusing v5.Actor.getLogger()
-- overriding using v5.Actor.getLogger()
- ...?