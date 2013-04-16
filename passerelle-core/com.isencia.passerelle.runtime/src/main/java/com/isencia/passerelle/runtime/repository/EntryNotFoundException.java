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
package com.isencia.passerelle.runtime.repository;

import com.isencia.passerelle.core.ErrorCode;

/**
 * @author delerw
 *
 */
public class EntryNotFoundException extends RepositoryException {
  private static final long serialVersionUID = -7997428299294386373L;

  /**
   * @param errorCode
   * @param message
   * @param rootException
   */
  public EntryNotFoundException(ErrorCode errorCode, String message, Throwable rootException) {
    super(errorCode, message, rootException);
  }
}
