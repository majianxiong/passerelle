package com.isencia.passerelle.process.service;

import java.util.concurrent.TimeUnit;

import com.isencia.passerelle.process.model.ContextEvent;
import com.isencia.passerelle.process.model.ContextProcessingCallback;
import com.isencia.passerelle.process.model.ErrorItem;
import com.isencia.passerelle.process.model.Request;
import com.isencia.passerelle.process.model.Task;
import com.isencia.passerelle.process.model.factory.ProcessFactory;
import com.isencia.passerelle.runtime.ProcessHandle;

public interface ProcessManager {
	public static final String REPORT_EVENT_TYPE = "REPORTED";
	public static final String RESTARTING = "restarting";

	ProcessFactory getFactory();
	
	ProcessHandle getHandle();
	
	ProcessPersister getPersister();
	
	Request getRequest();

	Task getTask(Long id);

	/**
	 * Notify listeners that the processing of the request was cancelled.
	 */
	void notifyCancelled();

	/**
	 * Notify listeners that the processing of a task was cancelled.
	 * 
	 * @param task
	 *            Task that got cancelled
	 */
	void notifyCancelled(Task task);

	/**
	 * Notify listeners that the processing of the request has finished with an
	 * error.
	 * 
	 * @param error
	 *            The error that happened during processing
	 */
	void notifyError(ErrorItem error, Throwable cause);

	/**
	 * Notify listeners that the processing of a task has finished with an
	 * error.
	 * 
	 * @param task
	 *            Task that finished with an error
	 * @param error
	 *            The error that happened during processing
	 */
	void notifyError(Task task, ErrorItem error, Throwable cause);

	/**
	 * Notify listeners that the processing of a task has finished with an
	 * error.
	 * 
	 * @param task
	 *            Task that finished with an error
	 * @param error
	 *            The error that happened during processing
	 */
	void notifyError(Task task, Throwable error);

	/**
	 * Notify listeners that the processing of the request has finished with an
	 * error.
	 * 
	 * @param error
	 *            The error that happened during processing
	 */
	void notifyError(Throwable error);

	/**
	 * Notify all listeners about a given context event.
	 * 
	 * @param event
	 */
	void notifyEvent(ContextEvent event);

	/**
	 * Notify all listeners about a given context event on the request.
	 * 
	 * @param eventType
	 * @param message
	 */
	void notifyEvent(String eventType, String message);

	/**
	 * Notify all listeners about a given context event on a task.
	 * 
	 * @param task
	 * @param eventType
	 * @param message
	 */
	void notifyEvent(Task task, String eventType, String message);

	/**
	 * Notify listeners that the processing of the request has finished.
	 */
	void notifyFinished();

	/**
	 * Notify listeners that the processing of a task has finished.
	 * 
	 * @param task
	 *            Task that finished
	 */
	void notifyFinished(Task task);

	/**
	 * Notify listeners that the processing of the request is pending
	 * completion. It has done its work but remains in 'ongoing' state until
	 * something else finishes it.
	 */
	void notifyPendingCompletion();

	/**
	 * Notify listeners that the processing of a task is pending completion. It
	 * has done its work but remains in 'ongoing' state until something else
	 * finishes it.
	 * 
	 * @param task
	 *            Task that is pending completion
	 */
	void notifyPendingCompletion(Task task);

	/**
	 * Notify listeners that the processing of a task was restarted.
	 * 
	 * @param task
	 *            Task that restarted
	 */
	void notifyRestarted(Task task);

	/**
	 * Notify listeners that the processing of the request has started.
	 */
	void notifyStarted();

	/**
	 * Notify listeners that the processing of a task has started.
	 * 
	 * @param task
	 *            Task that started
	 */
	void notifyStarted(Task task);

	/**
	 * Notify listeners that the processing of the request has timed out.
	 */
	void notifyTimeOut();

	/**
	 * Notify listeners that the processing of the task has timed out.
	 * 
	 * @param task
	 *            Task that timed out
	 */
	void notifyTimeOut(Task task);

	/**
	 * Pause the flow for this request.
	 */
	boolean pause();

	/**
	 * Restart the flow for this request from the given Task.
	 */
	boolean restart(long taskId, long timeOut, TimeUnit timeOutUnit);

	/**
	 * Resume the flow for this request.
	 */
	boolean resume();
	
	/**
	 * Start the flow for this request.
	 */
	boolean start();

	/**
	 * Stop the flow for this request.
	 */
	boolean stop(long timeOut, TimeUnit timeOutUnit);

	/**
	 * Subscribe the given callback to status change notifications of the given
	 * task
	 * 
	 * @param task
	 *            Task for which status change notifications are posted
	 * @param callback
	 *            Callback that will be notified
	 */
	void subscribe(Task task, ContextProcessingCallback callback);

	/**
	 * Subscribe the given callback to status change notifications for any/all
	 * task(s).
	 * 
	 * @param callback
	 */
	void subscribeAll(ContextProcessingCallback callback);

	/**
	 * Unsubscribe the given callback. It can be an "all" subscriber or one
	 * subscribed for a specific task.
	 * 
	 * @param callback
	 */
	void unsubscribe(ContextProcessingCallback callback);
}
