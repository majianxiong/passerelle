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

package com.isencia.passerelle.model;

import com.isencia.passerelle.core.ErrorCode;
import com.isencia.passerelle.core.PasserelleException;

/**
 * Exception thrown when someone tries to stop the execution of a given Flow while it is not executing.
 * 
 * @author erwin
 */
@SuppressWarnings("serial")
public class FlowNotExecutingException extends PasserelleException {

  /**
   * @param context
   */
  public FlowNotExecutingException(Object context) {
    super(ErrorCode.FLOW_STATE_ERROR, "Flow not executing", context, null);
  }
  /**
   * @param errorCode
   * @param context
   */
  public FlowNotExecutingException(ErrorCode errorCode, Object context) {
    super(errorCode, context, null);
  }
}