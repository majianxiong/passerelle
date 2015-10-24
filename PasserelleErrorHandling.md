# Introduction #

Passerelle models can be executed in different settings, both with possibilities for user interactions via a UI
and in fully-automated runtimes without the possibility/need for user interactions.
In some use-cases, such as scientific workflows, the actual number of runs per time is limited, but the processing done per run may be complex and long.
In other cases, many tens of thousands of runs are done per day.

In all these cases, there is a need for deterministic error processing.

Below, the different error handling features of Passerelle are presented, in the context of model executions.
We look at what happens when one or more actors encounter an error, as the actors are the central elements to implement all logic in Passerelle models.
"Hardcore" technical errors within the engine itself are outside of the scope of this documentation.

# Error handling within the model #

## At model startup ##
When a model is launched, several steps need to be taken before it is fully operational :
  * Parse the model definition, from the xml/moml file
  * Construct all contained entities, their relations, configuration parameters etc
  * Invoke the `preInitialize` and `initialize` methods on all actors.
> > This is also where Passerelle adds validation steps.

When errors are encountered in these initial steps, the model execution will be impossible or will automatically stop.
If the execution happens within a UI-application, the user will typically be notified about such errors via pop-ups or log viewers.

## During model execution ##
When the actor initializations are done, the model is fully in "processing" mode.
The exact logic to go from initialization to processing depends on the type of Director that is used, but can be ignored for our purposes here.

When an actor encounters an error in its processing, the resulting behaviour depends on the error's Severity:
  * Fatal errors by default cause the model execution to stop abruptly.
  * For non-fatal errors, modelers can include specific error-handling logic inside the models :
    * If the actor-in-error's error output port is connected, Passerelle sends an error message containing the error info and the actor's last received message.
    * If the actor's error output is not connected, the actor hands over the error info to the model Director.

These options for non-fatal errors are somewhat similar to the distinction between local try/catch structures
with explicit error handling close to the source of the error (the case with connected error port),
versus "uncaught" errors being propagated to the top-level in the call-stack, or model hierarchy in this case.

### Continuation via the actor's error output port ###
This requires that the error port is actually connected to something.
If the modeler chooses this option, the actor's error output port can be connected
to a `com.isencia.passerelle.actor.error.ErrorCatcher` actor, that is able to "unwrap" the error message and then forward the contained "original" message.
The `ErrorCatcher` actor can then be connected to a specific error-continuation logic within the model.
In special cases, when the error can just be ignored, the `com.isencia.passerelle.actor.general.DevNullActor` can come in handy.

A simple use-case could be to catch an error, and then send an email with the failed message, as illustrated below.

![http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/Model_ErrorCatcher.jpg](http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/Model_ErrorCatcher.jpg)

### Error propagation via the Director ###
When the error-handling is delegated to the Director, this one will check the model for the presence of `com.isencia.passerelle.ext.ErrorCollector` instances.
If there are some, the error is delivered to them and its processing can continue from there.
`com.isencia.passerelle.actor.error.ErrorObserver` actors are typically used in this way.
Their usage allows to have one-or-more central error handling solutions within a model, that can be triggered
whenever an error occurs in any of the model actors, without needing to connect all actors' error output ports to it.

In this model, the `ErrorObserver` is used to gather any error that can occur during the model execution, and simply puts the request
that is being processed in an error status.
I.c.o. an error in the final part of the sequence, to prevent endless loops with the `ErrorObserver`, a `DevNull` is connected to the error ports.

![http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/Model_ErrorObserver_DevNull.jpg](http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/Model_ErrorObserver_DevNull.jpg)

Besides the `ErrorObserver` actor, that can be added by the modelers, `ErrorCollector` implementations can also be registered by the runtime/engine,
e.g. to log "uncaught" errors in dedicated views or store them in a DB etc.


> For the "techies" : a submodel corresponds to an instance of `ptolemy.kernel.CompositeEntity`.
> Model elements can be of different kinds, but for our purposes we consider either Actors or `ptolemy.kernel.util.Attribute`s.
> The runtime can dynamically add "Mixin"-implementations of `ErrorCollector` and `Attribute` to a `CompositeEntity` and thus to a (sub)model.

**For the near future**
Currently, all `ErrorCollector`s in a model are treated "as equals".
With the next Passerelle core version (v8.2), this will change to having an error collection "scope" per model/submodel level.
When the error port is not connected, the errors of an actor in a submodel will first be delivered to the `ErrorCollector`(s) present in the submodel.
Also, work is ongoing to add filtering/selection options for `ErrorObserver`s, so more fine-grained error handling can be modeled, depending on the type of error.
When an error can not be delivered to a matching submodel's `ErrorObServer` it will be propagated upwards in the model hierarchy.

# Actor error handling #
## Actor template methods and error control strategy ##
The possibilities described above are the default behaviour of Passerelle, and are provided by the default implementation of
a `com.isencia.passerelle.ext.ErrorControlStrategy`, namely `com.isencia.passerelle.ext.impl.DefaultActorErrorControlStrategy`.

Through the common actor base class `com.isencia.passerelle.actor.Actor`, all actor life-cycle methods have been made final
to implement the template method pattern. The resulting `do...()` methods, to be implemented/overridden by actor implementations,
all have a dedicated checked exception that can be thrown, all subclasses of `com.isencia.passerelle.core.PasserelleException`.

```
// ...
public abstract class Actor extends TypedAtomicActor implements IMessageCreator {
//...
  /**
   * Template method implementation for preinitialize().
   * 
   * @throws InitializationException
   * @see ptolemy.actor.AtomicActor#preinitialize()
   */
  protected void doPreInitialize() throws InitializationException {
  }
//...
  /**
   * Template method implementation for prefire(). Method that can be overriden to implement precondition checking for the fire() loop. By default, returns
   * true. If the method returns true, the actor's fire() method will be called. If the method returns false, preFire() will be called again repetitively till
   * it returns true. So it's important that for "false" results there is some blocking/waiting mechanism implemented to avoid wild looping!
   * 
   * @return flag indicating whether the actor is ready for fire()
   * @see ptolemy.actor.AtomicActor#prefire()
   */
  protected boolean doPreFire() throws ProcessingException {
    return true;
  }
//...
  /**
   * Template method implementation for wrapup().
   * 
   * @throws TerminationException
   * @see ptolemy.actor.AtomicActor#wrapup()
   */
  protected void doWrapUp() throws TerminationException {
  }
//...
}
```

Inside the wrapping methods, these exceptions are caught and passed to the actor's error control strategy.

For example :

```
//...
  final public void wrapup() throws IllegalActionException {
    getLogger().trace("{} - wrapup() - entry", getInfo());

    try {
      doWrapUp();
    } catch (TerminationException e) {
      getErrorControlStrategy().handleTerminationException(this, e);
    }
//...
  }
//...
```

For the main actor iteration methods (preFire, fire, postFire), also `java.lang.RuntimeException`s are delegated to the error control strategy.
These are by default handled as fatal, i.e. will cause the model execution to be aborted.

## Actor development conventions ##
An actor developer should take into account following conventions, related to error handling :
  * wrap all "expected" exceptions in the checked exception types defined by the Actor API
  * put the last received message as exception context
  * put error codes in the exception's message (pending the arrival of a formal error-code field for `PasserelleException`s)
These will be discussed in some more detail, below.

### Wrapping in the actor API's defined exceptions ###
With the `com.isencia.passerelle.actor.v5.Actor` API, actor developers mostly need to be concerned about the actor's constructor and the implementation of
the `process()` method, significantly simplifying the development work compared to directly implementing the old/native `com.isencia.passerelle.actor.Actor`.

When developing the actor's `process` method, it is good practice to wrap the code in a `try / catch` structure, catching all expected exceptions
(both checked and non-fatal runtime ones), and to wrap them in an instance of `com.isencia.passerelle.actor.ProcessingException`.

I.e. : checked exceptions should in general not be "eaten". `RuntimeException`s linked to the received message can also be wrapped in
non-fatal `ProcessingExceptions`, when they are related to specific message conditions.
Fatal RuntimExceptions like out-of-memory, class loading errors or others that are linked to global application issues, and not to the specific received message, should of course not be wrapped.

### Put the last received message as exception context ###
Most actors process data per individual message. For those, it is good practice to put the current message as context of the created exception.

Also for actors that work on multiple concurrently received messages, it is a good idea to designate one of them as the VIM (very important message),
and to put that one as the exception's context.

The current error-handling-support in Passerelle enables a modeler to provide alternative flow continuations (cfr. first part of this document).
It is quite easy to perform some recovery logic for failed "single-message-processing" actors, i.e. to have fully automated and functionally complete workflow continuations.
(if there such alternatives exist of course).

On the other hand, for "multi-message-processing", there is currently no support to transparently try recovery or continuation.
But passing the `VIM` to the error catcher and the consecutive flow should at least allow to implement relevant error tracing or escalation actions.

### Put error codes in the exception's message ###
In the near future, more advanced error handling actors will be generalized from existing project-specific actors and will be open-sourced.
They will support fine-grained error scopes and error filtering.
One of the filtering options will be based on error codes.

Besides the filtering-support, using error codes is good practice anyway, e.g. to facilitate automated monitoring on your application log files etc.

### Example ###
A simple example from the `com.isencia.passerelle.actor.examples.HelloPasserelle` actor, illustrating all these conventions :

```
  @Override
  protected void process(ActorContext ctxt, ProcessRequest request, ProcessResponse response) throws ProcessingException {
    // Get the message received on the input port, from the request.
    ManagedMessage receivedMsg = request.getMessage(input);
    try {
      // We know the message will contain a String in its body.
      String receivedText = receivedMsg.getBodyContentAsString();
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
      throw new ProcessingException("[PASS-EX-1234] - Failed to transform the received text", receivedMsg, e);
    }
  }
```
