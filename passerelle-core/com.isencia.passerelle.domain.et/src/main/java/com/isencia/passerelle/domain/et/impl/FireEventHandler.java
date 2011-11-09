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

package com.isencia.passerelle.domain.et.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.actor.Actor;
import com.isencia.passerelle.domain.et.ETDirector;
import com.isencia.passerelle.domain.et.Event;
import com.isencia.passerelle.domain.et.EventHandler;
import com.isencia.passerelle.domain.et.FireEvent;

/**
 * This handler figures out the Actor for which the FireEvent is meant,
 * and tries out a fire() iteration.
 * <p>
 * The normal Ptolemy/Passerelle iteration semantics are assumed to be valid :
 * <ul>
 * <li>preFire() is invoked. If it returns false, the iteration is not continued.
 * If it returns true, the actor is ready to be fired.</li>
 * <li>fire() is invoked after preFire() returned true.</li>
 * <li>postFire() is invoked. If it returns false, this actor can be wrapped up.</li>
 * </ul>
 * </p>
 * @author delerw
 *
 */
public class FireEventHandler implements EventHandler<FireEvent> {
  private final static Logger LOGGER = LoggerFactory.getLogger(FireEventHandler.class);
  
  private ETDirector director;

  public FireEventHandler(ETDirector director) {
    this.director = director;
  }

  @Override
  public void initialize() {
  }

  @Override
  public boolean canHandle(Event event) {
    return (event instanceof FireEvent);
  }
  
  @Override
  public void handle(FireEvent event) throws Exception {
    Actor actor = event.getTarget();
    
    if(!director.isActorInactive(actor)) {
      LOGGER.debug("Handling FireEvent - iterating {}.",actor.getName());
      if(actor.prefire()) {
        actor.fire();
        
        if(!actor.postfire()) {
          // actor requests to never be fired again,
          // so mark it as wrapping up
          director.notifyActorInactive(actor);
        }
      } 
    } else {
      LOGGER.debug("Handling FireEvent but actor {} is inactive.", actor.getName());
    }
  }
}
