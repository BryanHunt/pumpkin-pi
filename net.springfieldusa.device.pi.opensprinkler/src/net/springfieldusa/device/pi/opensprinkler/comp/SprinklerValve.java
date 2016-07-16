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
package net.springfieldusa.device.pi.opensprinkler.comp;

import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import net.springfieldusa.device.valve.Valve;

@Component(service = Valve.class, configurationPolicy = ConfigurationPolicy.REQUIRE, property = {"type=water"})
public class SprinklerValve implements Valve, Comparable<SprinklerValve>
{
  public @interface Config
  {
    String name();
    int zone();
    int priority() default 0;
  }
  
  private String name;
  private int zone;
  private int priority;
  private volatile boolean open;
  private volatile OpenSprinkler controller;
  

  @Activate
  public void activate(Config config)
  {
    name = config.name();
    zone = config.zone();
    priority = config.priority();
  }
  
  @Override
  public String getName()
  {
    return name;
  }

  public int getZone()
  {
    return zone;
  }
  
  @Override
  public CompletableFuture<Void> open()
  {
    return controller.requestWater(this);
  }

  @Override
  public void close()
  {
    controller.closeValve(zone);
    open = false;
  }

  public boolean isOpen()
  {
    return open;
  }
  
  public void setOpen(boolean open)
  {
    this.open = open;
  }
  
  @Reference(unbind = "-")
  public void bindOpenSprinkler(OpenSprinkler controller)
  {
    this.controller = controller;
  }

  @Override
  public int compareTo(SprinklerValve other)
  {
    return Integer.compare(priority, other.priority);
  }
}
