# Introduction #

Testing Passerelle models and/or actors can be done manually, by designing and running some test models from one of the available model editors and runtimes. But this is not a scalable approach, nor can it be integrated in automated builds, continuous integration setups etc.

This guide introduces some test features of Passerelle, to support the construction of unit tests that can become part of automated test suites.

**Remark that the described test support is available on the current development trunk, but not yet included with the v7.0.0 release.**

# Main goals #

The Passerelle test support tools are oriented towards :
  * Setting up a flow definition in Java code, i.o. requiring a graphical editor
  * Simple way to execute a model from test code, with support for overriding actor parameter values
  * Setting expected counts for messages sent or received by actor ports
  * Setting expected counts for actor fire/process iterations per actor

With these things in place, a wide variety of test scenarios can be implemented.

# Overview of a test case #

The Passerelle test support builds on [JUnit](http://junit.org/).
Any kind of JUnit-based test approach could be followed, but in this introduction we assume a plain usage of JUnit 3 `junit.framework.TestCase` implementations.

Using plain actor constructors combined with the API provided by `com.isencia.passerelle.model.Flow` and `com.isencia.passerelle.model.FlowManager`, it is possible to define
and execute a Passerelle model in code :
  1. first a Flow instance must be created, and a Director must be assigned to it
  1. then all required actors can be constructed in that Flow
    * the actor parameter configuration can also be done via directly setting parameter values
    * but in most cases it is preferred to set parameter values via so-called "parameter overrides" with the flow execution, later on
  1. the Flow API has several utility methods to connect actor ports
  1. the FlowManager has methods to execute models in blocking and non-blocking ways.
    * For simple test control, the blocking approach is preferred.
    * For advanced and/or high-volume testing, non-blocking testing may be advisable, exploiting an ExecutionListener to be notified when a certain execution has finished.
  1. expected test results can be specified and asserted using `com.isencia.passerelle.testsupport.FlowStatisticsAssertion`

# Some examples #

The project `com.isencia.passerelle.actor.examples.test` in `contributions-actors` contains some test examples
in `com.isencia.passerelle.actor.examples.test.PasserelleExamplesTest`.

The test setup is extremely simple :

```
/* Copyright 2012 - iSencia Belgium NV
...
*/

public class PasserelleExamplesTest extends TestCase {
			  private Flow flow;
	  private FlowManager flowMgr;

	  protected void setUp() throws Exception {
		    flow = new Flow("unit test",null);
		    flowMgr = new FlowManager();
	  }
//...
```

The only things it takes, is optionally preparing reusable instances of a `Flow` and a `FlowManager`. Then we're set to implement the actual test methods...

## Hello Passerelle ##

`com.isencia.passerelle.actor.examples.test.PasserelleExamplesTest.testHelloPasserelle()` uses the example with the
`HelloPasserelle` example actor from the [actor development guide](PasserelleActorDevelopmentGuide#Hello_Passerelle.md).

![http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/HelloPasserelleModel.jpg](http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/HelloPasserelleModel.jpg)

This flow can also be created programmatically, as follows :

```
//...
  /**
   * A unit test for the HelloPasserelle model from the actor development guide.
   * 
   * @throws Exception
   */
  public void testHelloPasserelle() throws Exception {
    // ETDirector is for the new event-driven execution domain
    flow.setDirector(new ETDirector(flow,"director"));
    
    Const constant = new Const(flow,"Constant");
    HelloPasserelle helloHello = new HelloPasserelle(flow, "HelloHello");
    TracerConsole tracerConsole = new TracerConsole(flow, "TracerConsole");
    
    flow.connect(constant, helloHello);
    flow.connect(helloHello, tracerConsole);

//...
```

The `Flow` class provides utility methods to connect actors.
When the actors have default-named input and output ports, it is sufficient to just pass the actors themselves, in the right order.

Actor parameters can be configured by passing in a `Map` of parameter names and values. (This is somewhat similar as overriding a model's actor parameter values when launching a model using
Passerelle's `CommandLineExecutor`.)

```
//...
    // set remaining cfg params as map passed for execution
    // params are referenced via their name as specified in the actor code
    Map<String, String> props = new HashMap<String, String>();
    props.put("Constant.value", "Hello world");
    props.put("HelloHello.Changed text", "Hello Passerelle");
    // run it till the finish
    flowMgr.executeBlockingLocally(flow,props);
//...   
```

Once the model execution is done, we may be interested in checking some execution statistics, as follows :

```
//...
    new FlowStatisticsAssertion()
    // the constant should have sent 1 message
    // as it has a default-named output port, we can just pass the actor as 1st argument
    .expectMsgSentCount(constant, 1L)
    // same thing for the final console, but this time we expect 1 received msg
    .expectMsgReceiptCount(tracerConsole, 1L)
    // in-between the hello actor should have done 1 iteration,
    // resulting in 1 outgoing message. 
    // But we're lazy and skip this final expectation here, 
    // as we already specified that the console should have received this message anyway...
    .expectActorIterationCount(helloHello, 1L)
    // check our expectations
    .assertFlow(flow);
//...
```

`com.isencia.passerelle.testsupport.FlowStatisticsAssertion` offers a somewhat fluent API to define test expectations and assert them.
Test expectations can be set related to :
  * number of messages sent by an output port.
  * number of messages received by an input port.
  * number of iterations done by an actor

If the actor has a default-named output or input port, we can directly pass the actor instance in the respective `expectMsg...()` methods,
similarly to the `Flow.connect()` approach.

The above code simply expresses that we hope that the `constant` actor generates exactly one message, through its output port named "output". And also that a message arrives at the `tracerConsole` via its input port named "input". Sometime during the model execution, we also wish that the `helloHello` actor performed exactly 1 processing iteration.

Remark that we don't check actual message contents, or actor processing results. Such "advanced" test conditions will be added in a later stage.

## Using a HeaderFilter ##

`PasserelleExamplesTest.testAddRemoveMessageHeaderWithMatch()`
and `testAddRemoveMessageHeaderWithNoMatch()` use the example with the `HeaderFilter` and `AddRemoveMessageHeader` example actors
from the [actor development guide](PasserelleActorDevelopmentGuide#Filter_actors.md).

![http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/HdrFilterConfigPanel.jpg](http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/HdrFilterConfigPanel.jpg)

This flow can also be created programmatically, as follows :

```
//...
    flow.setDirector(new ETDirector(flow,"director"));
    
    Const constant = new Const(flow,"Constant");
    AddRemoveMessageHeader addRemoveMessageHeader = new AddRemoveMessageHeader(flow, "AddRemoveMessageHeader");
    HeaderFilter headerFilter = new HeaderFilter(flow, "HeaderFilter");
    TracerConsole tracerConsole = new TracerConsole(flow, "TracerConsole");
    ErrorConsole errorConsole = new ErrorConsole(flow, "ErrorConsole");
    
    flow.connect(constant, addRemoveMessageHeader);
    flow.connect(addRemoveMessageHeader, headerFilter);
    flow.connect(headerFilter.outputMatch, tracerConsole.input);
    flow.connect(headerFilter.outputNoMatch, errorConsole.input);
//...
```

Since the `headerFilter` has output ports with non-default names, we need to use the `Flow.connect(Port,Port)` method.

The first test method, `testAddRemoveMessageHeaderWithMatch()`, checks the case where the filter actor finds a match in the received message.
This can be done e.g. by the following parameter configuration for the model execution:

```
//...
    Map<String, String> props = new HashMap<String, String>();
    props.put("Constant.value", "Hello world");
    props.put("AddRemoveMessageHeader.Header name", "MyHeader");
    props.put("AddRemoveMessageHeader.Header value", "something");
    props.put("AddRemoveMessageHeader.mode", "Add");
    props.put("HeaderFilter.Header name", "MyHeader");
    flowMgr.executeBlockingLocally(flow,props);
//...
```

We want to ensure that the `headerFilter` has identified a "match", so has sent the outgoing message via its `outputMatch` output port. So the message must have arrived at the `tracerConsole`, and not at the `errorConsole`. Together with some other straightforward expectations, this can be done as follows :

```
//...
    new FlowStatisticsAssertion()
      .expectMsgSentCount(constant, 1L)
      .expectMsgSentCount(addRemoveMessageHeader, 1L)
      .expectMsgSentCount(headerFilter.outputMatch, 1L)
      .expectMsgReceiptCount(tracerConsole, 1L)
      .expectMsgReceiptCount(errorConsole, 0L)
      .assertFlow(flow);
//...
```

To check that the "no match" case also works as expected, we can execute the model with overridden parameters,
ensuring that the `headerFilter` has an impossible-to-meet condition, e.g. as follows :

```
    Map<String, String> props = new HashMap<String, String>();
    props.put("Constant.value", "Hello world");
    props.put("AddRemoveMessageHeader.Header name", "MyHeader");
    props.put("AddRemoveMessageHeader.Header value", "something");
    props.put("AddRemoveMessageHeader.mode", "Add");
    // let's set a filter that will certainly not match
    props.put("HeaderFilter.Header name", "AnotherHeader");

    flowMgr.executeBlockingLocally(flow,props);
```

And we can check the result as follows (adding an extra check, just for fun, on both filter outputs) :

```
    new FlowStatisticsAssertion()
      .expectMsgSentCount(constant, 1L)
      .expectMsgSentCount(addRemoveMessageHeader, 1L)
      .expectMsgSentCount(headerFilter.outputMatch, 0L)
      .expectMsgSentCount(headerFilter.outputNoMatch, 1L)
      .expectMsgReceiptCount(tracerConsole, 0L)
      .expectMsgReceiptCount(errorConsole, 1L)
      .assertFlow(flow);
```

# The result #

If all goes well, you can run all of this as a plain JUnit test from inside your eclipse workspace,
and the result should be :

![http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/PasserelleGreenBar.jpg](http://svn.codespot.com/a/eclipselabs.org/passerelle/wiki/img/PasserelleGreenBar.jpg)

Enjoy!