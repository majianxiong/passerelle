package com.isencia.passerelle.process.actor;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;
import com.isencia.passerelle.actor.FlowUtils;
import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.ext.impl.DefaultActorErrorControlStrategy;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;
import com.isencia.passerelle.process.common.exception.ErrorCode;
import com.isencia.passerelle.process.model.AttributeNames;
import com.isencia.passerelle.process.model.Context;
import com.isencia.passerelle.process.model.ContextEvent;
import com.isencia.passerelle.process.model.ContextProcessingCallback;
import com.isencia.passerelle.process.model.Request;
import com.isencia.passerelle.process.model.Status;
import com.isencia.passerelle.process.model.Task;
import com.isencia.passerelle.process.model.persist.ProcessPersister;
import com.isencia.passerelle.process.model.util.ProcessModelUtils;
import com.isencia.passerelle.process.service.ProcessManager;
import com.isencia.passerelle.util.ExecutionTracerService;

@SuppressWarnings("serial")
public abstract class TaskBasedActor extends Actor {
  private final static Logger LOGGER = LoggerFactory.getLogger(TaskBasedActor.class);

  public static final String RESULT_TAG = "Result tag";
  public static final String CONDITION_TAG = "Condition tag";
  public static final String ERROR_STRATEGY = "Error Strategy";
  public static final String ERROR_VIA_ERROR_PORT = "Error via error port";
  public static final String CONTINUE_VIA_ERROR_PORT = "Continue via error port";
  public static final String CONTINUE_VIA_OUTPUT_PORT = "Continue via output port";

  public Port output; // NOSONAR
  public Port input; // NOSONAR
  // by default the actor name is set as task/result type
  public StringParameter taskTypeParam; // NOSONAR
  public StringParameter resultTypeParam; // NOSONAR
  public StringParameter errorStrategyParameter;// NOSONAR

  /**
   * When this parameter is non-empty, it should contain a comma-separated list of names/keys/tags that must be present in a header of a received message,
   * otherwise the actor will not perform any actual processing. I.e. then it will just forward any received messages, unchanged, on its output port.
   */
  public StringParameter conditionTagParameter;// NOSONAR
  /**
   * When this parameter is non-empty, its value is set as an attribute on each resultblock created by each processed task. This can be used to differentiate
   * results with a same basic result type, e.g. generated by several instances of a same actor class in a same flow.
   */
  public StringParameter resultTagParameter;// NOSONAR

  private Set<ContextProcessingCallback> pendingListeners = Collections.synchronizedSet(new HashSet<ContextProcessingCallback>());

  public TaskBasedActor(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);
    input = PortFactory.getInstance().createInputPort(this, null);
    output = PortFactory.getInstance().createOutputPort(this);

    taskTypeParam = new StringParameter(this, AttributeNames.TASK_TYPE);
    resultTypeParam = new StringParameter(this, AttributeNames.RESULT_TYPE);

    // TODO: the default should come from the DirectorAdapter
    errorStrategyParameter = new StringParameter(this, ERROR_STRATEGY);
    errorStrategyParameter.addChoice(CONTINUE_VIA_OUTPUT_PORT);
    errorStrategyParameter.addChoice(CONTINUE_VIA_ERROR_PORT);
    errorStrategyParameter.addChoice(ERROR_VIA_ERROR_PORT);
    errorStrategyParameter.setExpression(ERROR_VIA_ERROR_PORT);

    resultTagParameter = new StringParameter(this, RESULT_TAG);
    conditionTagParameter = new StringParameter(this, CONDITION_TAG);
    registerExpertParameter(taskTypeParam);
    registerExpertParameter(resultTypeParam);
    registerExpertParameter(errorStrategyParameter);
    registerExpertParameter(resultTagParameter);
    registerExpertParameter(conditionTagParameter);
  }

  @Override
  public final ProcessingMode getProcessingMode(ProcessRequest request) {
    return ProcessingMode.ASYNCHRONOUS;
  }

  @Override
  public Object clone(Workspace workspace) throws CloneNotSupportedException {
    final TaskBasedActor actor = (TaskBasedActor) super.clone(workspace);
    actor.pendingListeners = Collections.synchronizedSet(new HashSet<ContextProcessingCallback>());
    return actor;
  }

  @Override
  protected void doInitialize() throws InitializationException {
    super.doInitialize();
    pendingListeners.clear();

    String errStrategy = readParameter(errorStrategyParameter);
    switch (errStrategy) {
    case CONTINUE_VIA_OUTPUT_PORT:
      // Continue with the message on the output port
      setErrorControlStrategy(new ContinueOnOutputControlStrategy());
      break;
    case CONTINUE_VIA_ERROR_PORT:
      // Continue with the message on the error port
      setErrorControlStrategy(new ContinueOnErrorControlStrategy());
      break;
    case ERROR_VIA_ERROR_PORT:
      // normal approach : error msg on the error port
      setErrorControlStrategy(new DefaultActorErrorControlStrategy());
      break;
    default:
      setErrorControlStrategy(new DefaultActorErrorControlStrategy());
    }
    String taskType = readParameter(taskTypeParam);
    if (StringUtils.isBlank(taskType)) {
      taskType = getName();
      taskTypeParam.setExpression(taskType);
    }
    String resultType = readParameter(resultTypeParam);
    if (StringUtils.isBlank(resultType)) {
      taskTypeParam.setExpression(taskType);
    }
  }

  /**
   * Allow ErrorControlStrategy to write to the output port
   */
  @Override
  protected void sendOutputMsg(Port port, ManagedMessage message) throws ProcessingException {
    super.sendOutputMsg(port, message);
  }

  @Override
  public final void process(ProcessManager processManager, ProcessRequest procReq, ProcessResponse procResp) throws ProcessingException {
    ManagedMessage message = procReq.getMessage(input);
    if (message != null) {
      Task task = null;
      try {
        // IMPORTANT : This is the right way to get the processContext from the received message,
        // with correct retrieval of branched context scopes.
        String scopeGroup = message.getSingleHeader(ProcessRequest.HEADER_CTXT_SCOPE_GRP);
        String scope = message.getSingleHeader(ProcessRequest.HEADER_CTXT_SCOPE);
        Context processContext = processManager.getScopedProcessContext(scopeGroup, scope);
        if (!doRestart(processManager, message, procResp)) {
          if (mustProcess(message, processManager)) {
            // create attributes and entries, in case of error catch and rethrow after task creation
            Map<String, String> taskAttributes = new HashMap<String, String>();
            if (scopeGroup != null) {
              taskAttributes.put(Task.HEADER_CTXT_SCOPE_GRP, scopeGroup);
            }
            if (scope != null) {
              taskAttributes.put(Task.HEADER_CTXT_SCOPE, scope);
            }
            Map<String, Serializable> taskContextEntries = new HashMap<String, Serializable>();
            Exception exceptionDuringCreation = null;
            try {
              taskAttributes = createImmutableTaskAtts(processContext, taskAttributes);
              // allow subclasses to add their own attributes, mostly based on data in the received processContext
              addActorSpecificTaskAttributes(message, taskAttributes);
              addActorSpecificTaskAttributes(processContext, taskAttributes);
              addActorSpecificTaskContextEntries(processContext, taskContextEntries);
            } catch (Exception e) {
              exceptionDuringCreation = e;
            }
            task = createTask(processManager, processContext, taskAttributes, taskContextEntries);
            if (exceptionDuringCreation != null) {
              // re-throw here, exception will be attached to taskContext
              throw new PasserelleException(ErrorCode.ACTOR_EXECUTION_FATAL, this, exceptionDuringCreation);
            }
            process(task, processManager, procResp);
            postProcess(message, task, procResp);
          } else {
            procResp.addOutputMessage(output, message);
            processFinished(processManager, procReq, procResp);
          }
        } else {
          processFinished(processManager, procReq, procResp);
        }
      } catch (PasserelleException ex) {
        ExecutionTracerService.trace(this, ex.getMessage());
        processManager.notifyError(task, ex);
        procResp.setException(new ProcessingException(ex.getErrorCode(), ex.getSimpleMessage(), this, message, ex.getCause()));
        processFinished(processManager, procReq, procResp);
      } catch (Throwable t) {
        ExecutionTracerService.trace(this, t.getMessage());
        processManager.notifyError(task, t);
        procResp.setException(new ProcessingException(ErrorCode.TASK_ERROR, "Error processing task", this, message, t));
        processFinished(processManager, procReq, procResp);
      }
    } else {
      // should not happen, but one never knows, e.g. when a requestFinish msg arrived or so...
      getLogger().warn("Actor " + this.getFullName() + " received empty message in process()");
      processFinished(processManager, procReq, procResp);
    }
  }

  /**
   * Override this in specific cases where default postProcessing is not OK. Default is to register a TaskContextListener that will send the processing Context
   * onwards when the Task is done.
   * 
   * @param message
   * @param task
   * @param response
   * @throws Exception
   */
  protected void postProcess(ManagedMessage message, Task task, ProcessResponse response) throws Exception {
    TaskContextListener listener = new TaskContextListener(message, response);
    pendingListeners.add(listener);
    response.getProcessManager().subscribe(task, listener);
  }

  /**
   * Should perform the actual processing of the task. For most simple/fast cases, this can be done in a synchronous fashion. For complex/long-running
   * processing, the usage of a ServiceBasedActor is advisable.
   * 
   * @param task
   *          the new task that must be processed
   * @param processManager
   * @param processResponse
   *          TODO
   * @throws ProcessingException
   */
  protected abstract void process(Task task, ProcessManager processManager, ProcessResponse processResponse) throws ProcessingException;

  /**
   * Override this method to define the logic for potentially skipping the processing of a received message.
   * <p>
   * Sample cases could be : check for mock mode, filter on certain request elements etc
   * </p>
   * Subclass overrides should always include a super.mustProcess() invocation, unless the default handling of the conditionTags can be dropped.
   * <p>
   * This variation of the mustProcess checks for the presence of a body header in the ManagedMessage, with one of the tags configured in the "condition tag"
   * actor parameter.
   * </p>
   * <p>
   * If the "condition tag" parameter is empty, this method returns true.
   * </p>
   * 
   * @param message
   * @return
   * @throws MessageException
   */
  protected boolean mustProcess(ManagedMessage message, ProcessManager processManager) throws MessageException {
    String conditionTagStr = conditionTagParameter.getExpression();
    if (StringUtils.isBlank(conditionTagStr)) {
      return true;
    } else {
      String[] conditionTags = conditionTagStr.trim().split(",");
      boolean result = false;
      for (String conditiontag : conditionTags) {
        if (message.hasBodyHeader(conditiontag)) {
          result = true;
          break;
        }
      }
      return result;
    }
  }

  /**
   * Checks if the process is being restarted and if the task for this actor is the one where the restart should pick in.
   * 
   * @param processManager
   * @param message
   * @param response
   * @return
   * @throws MessageException
   * @throws ProcessingException
   */
  protected boolean doRestart(ProcessManager processManager, ManagedMessage message, ProcessResponse response) throws MessageException, ProcessingException {
    Context processContext = processManager.getRequest().getProcessingContext();
    if (Status.RESTARTED.equals(processContext.getStatus())) {
      for (int taskIdx = processContext.getTasks().size() - 1; taskIdx >= 0; taskIdx--) {
        Task task = processContext.getTasks().get(taskIdx);
        if (!Status.CANCELLED.equals(task.getProcessingContext().getStatus())) {
          try {
            URI uri = new URI(task.getInitiator());
            if (FlowUtils.getOriginalFullName(this).substring(1).equals(uri.getPath().substring(1))) {
              if (task.getProcessingContext().isFinished()) {
                beforeRestart(task, processContext);
                onTaskFinished(task, message, response);
                return true;
              }
              if (Status.RESTARTED.equals(task.getProcessingContext().getStatus())) {
                processManager.notifyStarted();
                processManager.notifyCancelled(task);
                onRestart(task, processContext);
                break;
              }
            }
          } catch (URISyntaxException e) {
            continue;
          }
        }
      }
    }
    return false;
  }

  protected void onRestart(Task task, Context flowContext) {
  }

  protected void beforeRestart(Task task, Context flowContext) {
  }

  /**
   * This method creates a Task instance of the right implementation class, with the given attributes etc, and persists the new task.
   * 
   * @param processManager
   * @param parentRequest
   * @param taskAttributes
   * @param taskContextEntries
   * @return the new task
   * @throws Exception
   *           TODO ensure task is on the right scoped context if any, and then not yet on the "real" processcontext until the Join
   */
  private Task createTask(ProcessManager processManager, Context processContext, Map<String, String> taskAttributes,
      Map<String, Serializable> taskContextEntries) throws Exception {
    String initiator = getTaskInitiator();
    Request request = processManager.getRequest();
    Task task = processManager.getFactory().createTask(getTaskClass(request), processContext, initiator, getTaskType());
    for (Entry<String, String> attr : taskAttributes.entrySet()) {
      processManager.getFactory().createAttribute(task, attr.getKey(), attr.getValue());
    }
    for (String key : taskContextEntries.keySet()) {
      Serializable value = taskContextEntries.get(key);
      task.getProcessingContext().putEntry(key, value);
    }
    ProcessPersister persister = processManager.getPersister();
    boolean shouldClose = persister.open(true);
    persister.persistTask(task);
    if (shouldClose) {
      persister.close();
    }
    return task;
  }

  /**
   * Defines the initiator for a new Task started by this actor.
   * <p>
   * Default implementation uses an URI-syntax as follows : <br/>
   * <code>actor:/<flow name>.[<subflow name>...].<actor name></code> <br/>
   * i.e. the actor's full name (without leading .) is used as URI path
   * </p>
   * 
   * @return
   * @throws Exception
   */
  protected String getTaskInitiator() throws Exception {
    return new URI("actor", null, "/" + FlowUtils.getOriginalFullName(this).substring(1), null, null).toString();
  }

  /**
   * By default reads the task type from the taskTypeParameter on the actor. Override this if other type determination logic is needed.
   * 
   * @return the task type to be used for new task instances
   */
  protected String getTaskType() {
    try {
      return taskTypeParam.stringValue();
    } catch (IllegalActionException e) {
      getLogger().error(ErrorCode.TASK_INIT_ERROR + " - Error reading taskTypeParam - using alternate read", e);
      return taskTypeParam.getExpression();
    }
  }

  /**
   * @param parentRequest
   * @return the java class of the Task implementation entity. Default is null. With the default ProcessFactoryImpl this leads to using
   *         com.isencia.passerelle.process.model.impl.TaskImpl.
   */
  protected Class<? extends Task> getTaskClass(Request parentRequest) {
    return null;
  }

  @Override
  protected boolean doPostFire() throws ProcessingException {
    boolean result = super.doPostFire();
    if (!result) {
      synchronized (pendingListeners) {
        while (!pendingListeners.isEmpty()) {
          try {
            pendingListeners.wait(1000);
          } catch (InterruptedException e) {
            break;
          }
        }
      }
    }
    return result;
  }

  /**
   * Method to configure the attributes for the task that the actor wants to get executed. The actor implementation should add entries in the taskAttributes map
   * as needed for its type of task. Attribute data is typically obtained either from the received processContext and/or from the actor's parameters.
   * 
   * @param processContext
   * @param taskAttributes
   * @throws ProcessingException
   */
  protected void addActorSpecificTaskAttributes(final Context processContext, Map<String, String> taskAttributes) throws ProcessingException {
    try {
      ProcessModelUtils.storeContextItemValueInMap(taskAttributes, processContext, AttributeNames.RESULT_TYPE, resultTypeParam);
    } catch (IllegalActionException e) {
      throw new ProcessingException(ErrorCode.TASK_INIT_ERROR, "Error setting result type", this, e);
    }
  }

  protected void addActorSpecificTaskAttributes(final ManagedMessage message, Map<String, String> taskAttributes) throws ProcessingException {
  }

  /**
   * Method to allow actor implementations to pass specific context entries into the task that will be created and executed. Similar to
   * <code>addActorSpecificTaskAttributes</code> but :
   * <ul>
   * <li>context entries can contain any serializable object i.o. just strings</li>
   * <li>context entries are typically not persisted, but only valid in memory during the process execution!</li>
   * </ul>
   * 
   * @param processContext
   * @param taskContextEntries
   */
  protected void addActorSpecificTaskContextEntries(final Context processContext, Map<String, Serializable> taskContextEntries) throws ProcessingException {
    try {
      String tagValue = resultTagParameter.stringValue();
      if (StringUtils.isNotBlank(tagValue)) {
        taskContextEntries.put(AttributeNames.RESULT_TAG, tagValue);
      }
    } catch (IllegalActionException e) {
      throw new ProcessingException(ErrorCode.TASK_INIT_ERROR, "Error setting result tag", this, e);
    }
  }

  @Override
  protected final String getAuditTrailMessage(ManagedMessage message, Port port) {
    try {
      if (message.getBodyContent() instanceof Context) {
        Context processContext = (Context) message.getBodyContent();
        return port.getFullName() + " - msg for request " + processContext.getRequest().getId();
      } else {
        return super.getAuditTrailMessage(message, port);
      }
    } catch (Exception e) {
      // TODO do something in case of exception
      return null;
    }
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  /**
   * Callback method that is invoked after a task has been started by the actor. By default it does nothing.
   * <p>
   * This method may be overridden for special cases where extra logic is needed when starting a task.
   * </p>
   * <p>
   * REMARK : any implementation must be fast and non-blocking as this is invoked on the task execution thread! Implementations should not throw any exceptions!
   * </p>
   * 
   * @param task
   */
  protected void onTaskStarted(Task task) {
    // do nothing by default
  }

  /**
   * Callback method that is invoked after a task has been finished by the actor. By default it sends the received message on the actor's output port.
   * <p>
   * This method may be overridden for special cases where e.g. extra/custom output ports must be used or extra logic must be triggered after finishing a task.
   * </p>
   * <p>
   * REMARK : any implementation must be fast and non-blocking as this is invoked on the task execution thread!
   * </p>
   * 
   * @param task
   * @param message
   * @param processResponse
   * @throws ProcessingException
   */
  protected void onTaskFinished(Task task, ManagedMessage message, ProcessResponse processResponse) throws ProcessingException {
    // by default send out on output port
    if (output != null && output.getContainer() != null) {
      processResponse.addOutputMessage(output, message);
    }
  }

  /**
   * Callback method that is invoked after a task has been finished by the actor.
   * 
   * @param task
   * @param error
   */
  protected void onTaskError(Task task, Throwable error) {

  }

  protected Map<String, String> createImmutableTaskAtts(Context processContext, Map<String, String> taskAttributes) throws ProcessingException {
    String requestId = Long.toString(processContext.getRequest().getId());
    String referenceId = Long.toString(processContext.getRequest().getCase().getId());

    taskAttributes.put(AttributeNames.CREATOR_ATTRIBUTE, getFullName());
    taskAttributes.put(AttributeNames.REF_ID, referenceId);
    taskAttributes.put(AttributeNames.REQUEST_ID, requestId);
    return taskAttributes;
  }

  private final class TaskContextListener implements ContextProcessingCallback {

    private ManagedMessage message;
    private ProcessResponse processResponse;
    private boolean consumed;

    public TaskContextListener(ManagedMessage message, ProcessResponse processResponse) {
      this.message = message;
      this.processResponse = processResponse;
    }

    private void removeMeAsListener() {
      synchronized (pendingListeners) {
        pendingListeners.remove(this);
        pendingListeners.notifyAll();
      }
    }

    public synchronized void contextStarted(ContextEvent event) {
      // allow a hook
      if (!isConsumed()) {
        Task task = (Task) event.getContext().getRequest();
        onTaskStarted(task);
      }
    }

    public synchronized void contextInterrupted(ContextEvent event) {
      removeMeAsListener();
    }

    public void contextWasCancelled(ContextEvent event) {
      removeMeAsListener();
    }

    public synchronized void contextError(ContextEvent event, Throwable error) {
      if (!isConsumed()) {
        Task task = (Task) event.getContext().getRequest();
        Request parentrequest = task.getParentContext().getRequest();
        final String errorMsg = "Error executing task " + task.getType() + " with task ID " + task.getId() + " for request " + parentrequest.getId();
        ProcessingException exception = new ProcessingException(ErrorCode.TASK_ERROR, errorMsg, TaskBasedActor.this, message, error);
        onTaskError(task, error);
        try {
          getErrorControlStrategy().handleFireException(TaskBasedActor.this, exception);
          setConsumed(true);
        } catch (Exception e) {
          // this line serves to get the constructed PasserelleException in the
          // log file
          getLogger().error("Failed to send error msg, so dumping its stacktrace here ", exception);
          // and this one to also get the IllegalActionException in there
          getLogger().error("Failed to send error msg because of ", e);
        } finally {
          removeMeAsListener();
        }
      }
    }

    public synchronized void contextFinished(ContextEvent event) {
      if (!isConsumed()) {
        Task task = (Task) event.getContext().getRequest();
        Request parentrequest = task.getParentContext().getRequest();
        try {
          onTaskFinished(task, message, processResponse);
          setConsumed(true);
        } catch (Exception e) {
          getLogger().error("Failed to send result msg for task " + task.getId() + " for request " + parentrequest.getId(), e);
          contextError(event, e);
        } finally {
          removeMeAsListener();
        }
      }
    }

    @Override
    public void contextPendingCompletion(ContextEvent event) {
      // this used to be an almost-copy of the logic of contextFinished() up to 1.5.
      // we now expect that a completely uniform handling works as well.
      // the only/main actor using the pending-completion state will/should indeed work correctly with the new version.
      contextFinished(event);
    }

    public void contextTimeOut(ContextEvent event) {
      if (!isConsumed()) {
        Task task = (Task) event.getContext().getRequest();
        Request parentrequest = task.getParentContext().getRequest();
        final String errorMsg = "Timeout invoking task " + task.getType() + " with task ID " + task.getId() + " for request " + parentrequest.getId();
        ProcessingException exception = new ProcessingException(ErrorCode.TASK_TIMEOUT, errorMsg, TaskBasedActor.this, message, null);
        try {
          ExecutionTracerService.trace(TaskBasedActor.this, exception);
          getErrorControlStrategy().handleFireException(TaskBasedActor.this, exception);
          setConsumed(true);
        } catch (Exception e) {
          ExecutionTracerService.trace(TaskBasedActor.this, exception);
          // this line serves to get the constructed PasserelleException in the
          // log file
          getLogger().error("Failed to send timeout error msg, so dumping its stacktrace here ", exception);
          // and this one to also get the IllegalActionException in there
          getLogger().error("Failed to send timeout error msg because of ", e);
        } finally {
          removeMeAsListener();
        }
      }
    }

    public boolean isConsumed() {
      return consumed;
    }

    public void setConsumed(boolean consumed) {
      this.consumed = consumed;
      if (consumed) {
        processFinished(processResponse.getProcessManager(), processResponse.getRequest(), processResponse);
      }
    }
  }
}
