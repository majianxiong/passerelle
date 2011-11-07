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

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.actor.Actor;
import com.isencia.passerelle.domain.et.Event;
import com.isencia.passerelle.domain.et.EventHandler;
import com.isencia.passerelle.domain.et.FireEvent;
import com.isencia.passerelle.domain.et.SendEvent;

/**
 * This handler figures out the Actor for which the SendEvent is meant,
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
public class SendEventHandler implements EventHandler<SendEvent> {
  private final static Logger LOGGER = LoggerFactory.getLogger(SendEventHandler.class);
  
  private Set<Actor> inactiveActors = new HashSet<Actor>();
  
  public void initialize() {
    inactiveActors.clear();
  }
  @Override
  public boolean canHandle(Event event) {
    return (event instanceof SendEvent);
  }
  @Override
  public void handle(SendEvent event) throws Exception {
    Actor actor = (Actor) event.getReceivingPort().getContainer();
    
    if(!inactiveActors.contains(actor)) {
      LOGGER.debug("Handling SendEvent - iterating {}.",actor.getName());
      if(actor.prefire()) {
        actor.fire();
        
        if(!actor.postfire()) {
          // actor requests to never be fired again,
          // so mark it as wrapping up
          inactiveActors.add(actor);
          LOGGER.debug("Marking actor {} as inactive.", actor.getName());
        }
      } 
    } else {
      LOGGER.debug("Handling SendEvent but actor {} is inactive.", actor.getName());
    }
  }
}
