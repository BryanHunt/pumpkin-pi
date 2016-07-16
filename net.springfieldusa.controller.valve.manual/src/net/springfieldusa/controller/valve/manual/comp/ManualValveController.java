/*******************************************************************************
 * Copyright (c) 2016 Bryan Hunt.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bryan Hunt - initial API and implementation
 *******************************************************************************/
package net.springfieldusa.controller.valve.manual.comp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import net.springfieldusa.device.valve.Valve;

@Component(service = ManualValveController.class, property = { "osgi.command.scope=valve", "osgi.command.function=openValve", "osgi.command.function=closeValve" , "osgi.command.function=listValves"})
public class ManualValveController
{
  private Map<String, Valve> valves = new ConcurrentHashMap<>();
  
  public void openValve(String name)
  {
    Valve valve = valves.get(name);
    
    if(valve != null)
      valve.open().thenRun(() -> {
        System.out.println("Valve " + valve.getName() + " opened");
      });
  }
  
  public void closeValve(String name)
  {
    Valve valve = valves.get(name);
    
    if(valve != null)
      valve.close();

    System.out.println("Valve " + valve.getName() + " closed");
  }
  
  public void listValves()
  {
    for(String name : valves.keySet())
      System.out.println(name);
  }
  
  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void bindValve(Valve valve)
  {
    valves.put(valve.getName(), valve);
  }
  
  public void unbindValve(Valve valve)
  {
    valves.remove(valve.getName(), valve);
  }  
}
