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
package net.springfieldusa.controller.valve.fill.comp;

import org.osgi.service.component.annotations.*;
import org.osgi.service.log.LogService;

import net.springfieldusa.comp.AbstractComponent;
import net.springfieldusa.device.sensor.contact.ContactSensor;
import net.springfieldusa.device.sensor.contact.ContactSensorListener;
import net.springfieldusa.device.valve.Valve;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public class ValveFillController extends AbstractComponent implements ContactSensorListener
{
  public @interface Config
  {
    String name();

    long fillTimeout() default 1;
  }

  private volatile Valve valve;
  private volatile ContactSensor sensor;
  private String name;
  private long fillTimeout;
  private Thread fillTimer;

  @Activate
  public void activate(Config config)
  {
    name = config.name();
    fillTimeout = config.fillTimeout() * 60000;
  }

  @Deactivate
  public void deactivate()
  {
    sensor.removeListener(this);
  }

  @Override
  public void sensorStateChanged(boolean active)
  {
    if (active)
      openValve();
    else
      closeValve();
  }

  @Reference(unbind = "-")
  public void bindValve(Valve valve)
  {
    this.valve = valve;
  }

  @Reference
  public void bindContactSensor(ContactSensor sensor)
  {
    sensor.addListener(this);
  }

  public void unbindContactSensor(ContactSensor sensor)
  {
    sensor.removeListener(this);
  }

  private void openValve()
  {
    if (!valve.isOpen())
    {
      valve.open().thenRun(() -> {
        log(LogService.LOG_INFO, name + " has started filling");
        fillTimer = new Thread(() -> {
          try
          {
            Thread.sleep(fillTimeout);
            log(LogService.LOG_WARNING, name + " timed out while filling");
            valve.close();
          }
          catch (InterruptedException e)
          {}
        });
        
        fillTimer.start();
      });
    }
    else
    {
      log(LogService.LOG_WARNING, name + " was requested to start filling while valve was open - ignoring");
    }
  }

  private void closeValve()
  {
    if (valve.isOpen())
    {
      fillTimer.interrupt();
      valve.close();
      log(LogService.LOG_INFO, name + " has stopped filling");
    }
    else
    {
      log(LogService.LOG_WARNING, name + " was requested to stop filling when valve was already closed - ignoring");
    }
  }
}
