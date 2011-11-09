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

package com.isencia.passerelle.domain.et;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import ptolemy.data.Token;
import com.isencia.passerelle.core.Port;

/**
 * An event that represents the normal sending of a Passerelle Message to a receiving port.
 * <p>
 * Depending on the exact implementation of the execution domain, this event will be generated only
 * after the message has already been sent to the receiving port, or the event may serve to
 * represent the ongoing transport of the message...
 * </p>
 * 
 * @author delerw
 *
 */
public class SendEvent extends AbstractEvent {

  private static final long serialVersionUID = 2244804696667897740L;
  
  private Port sendingPort;
  private Port receivingPort;
  private Token token;
  
  public SendEvent(Token token, Port sendingPort, Port receivingPort) {
   this(token, sendingPort, receivingPort, new Date());
  }

  public SendEvent(Token token, Port sendingPort, Port receivingPort, Date timeStamp) {
    super(timeStamp);
    this.token = token;
    this.sendingPort = sendingPort;
    this.receivingPort = receivingPort;
  }

  /**
   * 
   * @return the port that has sent the message to the receiving port
   */
  public Port getSendingPort() {
    return sendingPort;
  }
  
  /**
   * 
   * @return the port that has (or will have) received the message
   */
  public Port getReceivingPort() {
    return receivingPort;
  }
  
  /**
   * 
   * @return the message that has been sent between the given ports
   */
  public Token getMessage() {
    return token;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((token == null) ? 0 : token.hashCode());
    result = prime * result + ((receivingPort == null) ? 0 : receivingPort.hashCode());
    result = prime * result + ((sendingPort == null) ? 0 : sendingPort.hashCode());
    result = prime * result + ((getTimestamp() == null) ? 0 : getTimestamp().hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SendEvent other = (SendEvent) obj;
    if (token == null) {
      if (other.token != null) return false;
    } else if (!token.equals(other.token)) return false;
    if (receivingPort == null) {
      if (other.receivingPort != null) return false;
    } else if (!receivingPort.equals(other.receivingPort)) return false;
    if (sendingPort == null) {
      if (other.sendingPort != null) return false;
    } else if (!sendingPort.equals(other.sendingPort)) return false;
    if (getTimestamp() == null) {
      if (other.getTimestamp() != null) return false;
    } else if (!getTimestamp().equals(other.getTimestamp())) return false;
    return true;
  }

  public String toString(DateFormat dateFormat) {
    return "SendEvent [timeStamp=" + dateFormat.format(getTimestamp()) + ", receivingPort=" + receivingPort.getFullName() + "]";
  }
}
