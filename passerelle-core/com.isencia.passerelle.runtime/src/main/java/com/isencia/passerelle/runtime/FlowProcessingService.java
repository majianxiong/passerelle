/* Copyright 2013 - iSencia Belgium NV

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
package com.isencia.passerelle.runtime;

import java.util.List;
import java.util.Map;
import com.isencia.passerelle.core.Event;

/**
 * A service interface for everything related to executing a flow,
 * including support for stopping/pausing/resuming/stepping/breakpoints etc.
 * 
 * @author erwin
 *
 */
public interface FlowProcessingService {
  
  enum StartMode {
    NORMAL, DEBUG, STEP;
  }

  /**
   * Start a process in the specified mode.
   * <p>
   * This method should run the model in a non-blocking way, i.e. should return swiftly with the created <code>ProcessHandle</code>
   * while the process may keep on running in the background for a longer period of time.
   * </p>
   * <p>
   * The optional listener will be notified of all detailed <code>ProcessEvent</code>s.
   * Remark that in the absence of a listener, steps/resume may still be triggered via <code>step()</code> and <code>resume()</code>, 
   * and status info may be obtained via iteratively invoking <code>refresh(Processhandle)</code>.
   * <br/>
   * But such a "polling" approach is not desirable. A listener-based approach is almost always more efficient and more powerful.
   * </p>
   * <p>
   * In <b>NORMAL</b> mode, the execution will typically run in one shot until the end.
   * </p>
   * <p>
   * Via a <code>suspend()</code> request, the execution can be suspended.
   * After which it can be continued again via <code>resume()</code>, or per <code>step()</code> etc.
   * </p>
   * <p>
   * In <b>STEP</b> mode, actor iterations are done one-by-one, each time a <code>step()</code> has been requested.
   * Via <code>resume()</code>, the execution can continue as in <b>NORMAL</b> mode.
   * </p>
   * <p>
   * In <b>DEBUG</b> mode, the execution will suspend when one of the specified break points is reached.
   * After which it can be continued again via <code>resume()</code>, or per <code>step()</code> etc.
   * <br/> 
   * Breakpoints must refer to named elements in the running process : actors and/or ports.
   * <br/> 
   * The names given should be the full hierarchic names, without the flow's name.
   * E.g. in a HelloWorld model with a Constant actor connected to a Console, valid breakpoints could be :
   * <ul>
   * <li>Constant</li>
   * <li>Console.input</li>
   * <li>etc.</li>
   * </ul>
   * </p>
   * <p>
   * The optional <code>processContextId</code> identifies a <code>com.isencia.passerelle.process.model.Context</code> that is being processed across one or more flow executions.
   * Such <code>Context</code> instances and their <code>processContextId</code> are typically created outside of the Passerelle runtime services. 
   * For simple processes, each flow execution typically corresponds to one Context which ends up in a final status. 
   * For more complex processes, several consecutive Passerelle flow executions may be involved that are linked through delegation/redirection, sharing a same Context.
   * </p>
   * <p>
   * When the <code>processContextId</code> is null, simple models may just run without a formal <code>Context</code>, i.e. by assigning a simple arbitrary execution ID for the <code>ProcessHandle</code>.
   * More formal execution situations may choose to generate a new <code>Context</code> internally when needed/appropriate.
   * This choice can be made either in implementations of this service, or by specialised actors used in the flows.
   * </p>
   * <p>
   * Remark that loading a complete <code>Context</code> in memory, via the com.isencia.passerelle.process.model.service.ContextRepository.getContext()</code>, can be a costly operation.
   * So when detailed <code>Context</code> data is not absolutely required, it is preferable to just pass the <code>processContextId</code> around.
   * </p>
   * @param mode
   * @param flowHandle
   * @param processContextId can be null : for context-aware executions, this can be used to set/share the <code>Context</code> for a flow execution. 
   * @param parameterOverrides can be null : overridden values of flow/actor parameters
   * @param listener can be null
   * @param breakpointNames optional names of the Flow elements (ports and/or actors) where the process should place a breakpoint, if started in DEBUG mode
   * @return
   */
  ProcessHandle start(StartMode mode, FlowHandle flowHandle, String processContextId, Map<String, String> parameterOverrides, ProcessListener listener, String... breakpointNames);

  /**
   * Terminate a running process through a termination event.
   * <p>
   * Implementations may or may not block until the process is effectively dead, and should clearly document their choice.
   * Any registered <code>ProcessListener</code>s will be notified when the termination is done.
   * </p>
   * @param processHandle
   * @param time
   * @param unit
   * @return the updated processHandle
   * @throws FlowNotExecutingException when the process identified by the handle was not (or no longer) running
   */
  ProcessHandle terminate(ProcessHandle processHandle) throws FlowNotExecutingException;
  
  /**
   * 
   * @param processHandle
   * @return the updated processHandle
   * @throws FlowNotExecutingException when the process identified by the handle was not (or no longer) running
   */
  ProcessHandle suspend(ProcessHandle processHandle) throws FlowNotExecutingException;
  
  /**
   * 
   * @param processHandle
   * @return the updated processHandle
   * @throws FlowNotExecutingException when the process identified by the handle was not (or no longer) running
   */
  ProcessHandle resume(ProcessHandle processHandle) throws FlowNotExecutingException;
  
  /**
   * 
   * @param processHandle
   * @return the updated processHandle
   * @throws FlowNotExecutingException when the process identified by the handle was not (or no longer) running
   */
  ProcessHandle step(ProcessHandle processHandle) throws FlowNotExecutingException;

  /**
   * 
   * @param extraBreakpoints
   * @return the updated processHandle with extra breakpoints
   */
  ProcessHandle addBreakpoints(ProcessHandle processHandle, String... extraBreakpoints);
  
  /**
   * 
   * @param breakpointsToRemove
   * @return the updated processHandle with removed breakpoints
   */
  ProcessHandle removeBreakpoints(ProcessHandle processHandle, String... breakpointsToRemove);

  /**
   * Signal an <code>Event</code> to the running process identified by the handle.
   * These can be pure events, or may also pass more complex data (e.g. use input) into a running process.
   * 
   * @param processHandle
   * @param event
   * @return the updated processHandle after delivering the event to the running process
   * @throws FlowNotExecutingException when the process identified by the handle was not (or no longer) running
   */
  ProcessHandle signalEvent(ProcessHandle processHandle, Event event) throws FlowNotExecutingException;
  
  /**
   * 
   * @param processHandle
   * @param maxCount
   * @return the list of processing events, from newest to oldest and limited to the given maxCount
   */
  List<Event> getProcessEvents(ProcessHandle processHandle, int maxCount);
  
  /**
   * Gets the process events for a process with given UUID.
   * <p>
   * This method is useful for processes executed in the past, 
   * or other situations where the ProcessHandle is not available.
   * </p>
   * 
   * @param processId
   * @param maxCount
   * @return the list of processing events, from newest to oldest and limited to the given maxCount
   */
  List<Event> getProcessEvents(String processId, int maxCount);

  /**
   * 
   * @param processId
   * @return the handle for the given process Id; null if no active process with given Id is found.
   */
  ProcessHandle getHandle(String processId);
  
  /**
   * 
   * @param processHandle
   * @return the refreshed handle, i.e. with updated status info
   */
  ProcessHandle refresh(ProcessHandle processHandle);
}
