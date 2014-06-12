package com.isencia.passerelle.process.actor.forkjoin;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.data.StringToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.core.ErrorCode;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageFactory;
import com.isencia.passerelle.message.internal.MessageContainer;
import com.isencia.passerelle.message.internal.SettableMessage;
import com.isencia.passerelle.process.actor.ActorContext;
import com.isencia.passerelle.process.actor.ProcessRequest;
import com.isencia.passerelle.process.actor.ProcessResponse;
import com.isencia.passerelle.process.model.Context;

/**
 * A <code>Splitter</code> actor generates a sequence of outgoing messages for each received message. The split count & output message contents are determined
 * by the data in a named item in the received <code>Context</code>.
 * <p>
 * The split-item's name can be configured via the <code>splitItemParameter</code>. The value of the item is assumed to be potentially a concatenation of
 * multiple individual values, with a delimiter as configured in <code>splitDelimiterParameter</code>. <br/>
 * For each individual value, a separate message will be sent out in sequence, with this particalur value for the named item.
 * </p>
 * TODO split value is now set as a <code>Context</code> entry. I.e. it is not effectively replacing the original item which could be in any
 * <code>ResultBlock</code>, <code>Request</code> attribute etc. Check if this is OK in all required use cases. (i.e. in rules modules, facts are typically
 * constructed based on result blocks etc, and the context entries are not necessarily taken into account I think!?
 * 
 * @author erwin
 */
public class Splitter extends AbstractMessageSequenceGenerator {
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = LoggerFactory.getLogger(Splitter.class);

  public Port input;
  public Port output;
  /**
   *  extra output where the received msg will be sent out, when nothing was found to split on
   */
  public Port outputNoSplit;

  /**
   * Parameter to specify the name of the name/value item in the <code>Context</code>, that must be used as split criterium.
   */
  public StringParameter splitSrcItemParameter;
  public StringParameter splitOutItemParameter;
  public StringParameter splitDelimiterParameter;

  /**
   * @param container
   * @param name
   * @throws IllegalActionException
   * @throws NameDuplicationException
   */
  public Splitter(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name, true);
    input = PortFactory.getInstance().createInputPort(this, null);
    output = PortFactory.getInstance().createOutputPort(this);
    outputNoSplit = PortFactory.getInstance().createOutputPort(this, "outputNoSplit");

    splitSrcItemParameter = new StringParameter(this, "Split src item");
    splitOutItemParameter = new StringParameter(this, "Split output item");
    splitDelimiterParameter = new StringParameter(this, "Split delimiter");
    splitDelimiterParameter.setExpression(",");
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  public void process(ActorContext ctx, ProcessRequest procRequest, ProcessResponse procResponse) throws ProcessingException {
    MessageContainer message = (MessageContainer) procRequest.getMessage(input);
    if (message != null) {
      try {
        Context processContext = getRequiredContextForMessage(message);
        Long scopeId = processContext.getRequest().getId();
        registerSequenceScopeMessage(scopeId, message);

        String splitSrcItemName = ((StringToken) splitSrcItemParameter.getToken()).stringValue();
        String splitOutItemName = ((StringToken) splitOutItemParameter.getToken()).stringValue();
        if (StringUtils.isEmpty(splitOutItemName)) {
          splitOutItemName = splitSrcItemName;
        }
        String splitValue = processContext.lookupValue(splitSrcItemName);
        boolean doingSplit = false;
        if (StringUtils.isNotEmpty(splitValue)) {
          String splitDelimiter = ((StringToken) splitDelimiterParameter.getToken()).stringValue();
          String[] valueParts = splitValue.split(splitDelimiter);
          if (valueParts.length > 0) {
            try {
              if (getAuditLogger().isInfoEnabled()) {
                getAuditLogger().info("Splitting msg {} on {}:{}", new String[] { getAuditTrailMessage(message, input), splitSrcItemName, splitDelimiter });
              }
            } catch (Exception e) {
              getLogger().error("Error logging audit trail", e);
            }
            for (int i = 0; i < valueParts.length; ++i) {
              Context newOne = processContext.fork();
              newOne.putEntry(splitOutItemName, valueParts[i]);
              MessageContainer outputMsg = (MessageContainer) MessageFactory.getInstance().createMessageCloneInSequence(message,
                  processContext.getRequest().getId(), // sequence ID
                  new Long(i), // sequence position
                  (i == (valueParts.length - 1))); // end of sequence?
              // enforce single Splitter name (so use setHeader i.o. addHeader)
              outputMsg.setHeader(HEADER_SEQ_SRC, getName());
              outputMsg.setBodyContent(newOne, ManagedMessage.objectContentType);
              ((SettableMessage)outputMsg).setHeader(ProcessRequest.HEADER_PROCESS_CONTEXT, newOne.getProcessId());
              procResponse.addOutputMessage(output, outputMsg);
            }
            doingSplit = true;
          }
        }
        if (!doingSplit) {
          try {
            if (getAuditLogger().isInfoEnabled()) {
              getAuditLogger().info("Forwarding {} via outputNoSplit ; nothing found to split on {}", getAuditTrailMessage(message, input), splitSrcItemName);
            }
          } catch (Exception e) {
            getLogger().error("Error logging audit trail", e);
          }
          procResponse.addOutputMessage(outputNoSplit, message);
        }
      } catch (Exception e) {
        throw new ProcessingException(ErrorCode.ACTOR_EXECUTION_ERROR, "Error generating forked messages", this, message, e);
      }
    }
  }
}
