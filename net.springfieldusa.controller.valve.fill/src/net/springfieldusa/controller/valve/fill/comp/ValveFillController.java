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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

import net.springfieldusa.comp.AbstractComponent;
import net.springfieldusa.device.sensor.contact.ContactSensor;
import net.springfieldusa.device.valve.Valve;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, property = { EventConstants.EVENT_TOPIC + "=" + ContactSensor.SENSOR_CHANGE_EVENT })
public class ValveFillController extends AbstractComponent implements EventHandler, Runnable
{
  public @interface Config
  {
    String name();

    long startDelay() default 0;

    long stopDelay() default 0;

    long fillTimeout() default 0;
  }

  private static final String FILL_EVENT = "net/springfieldusa/controller/valve/fill";

  private volatile Valve valve;
  private volatile ContactSensor sensor;
  private volatile EventAdmin eventAdmin;
  private volatile long startFillTime;
  
  private String name;
  private long startDelay;
  private long stopDelay;
  private long fillTimeout;
  private boolean filling;
  
  private Thread fillThread;
  private ReentrantLock fillLock;
  private Condition sensorActive;
  private Condition sensorInactive;
  private Condition fillStarted;

  @Activate
  public void activate(Config config)
  {
    name = config.name();
    
    log(LogService.LOG_INFO, name + " is bound to sensor: " + sensor.getName());
    log(LogService.LOG_INFO, name + " is bound to valve: " + valve.getName());
    
    fillTimeout = config.fillTimeout();

    filling = false;

    fillLock = new ReentrantLock();
    sensorActive = fillLock.newCondition();
    sensorInactive = fillLock.newCondition();
    fillStarted = fillLock.newCondition();

    fillThread = new Thread(this, name);
    fillThread.start();

    if (sensor.isActive())
      startFill();
  }

  @Deactivate
  public void deactivate()
  {
    fillThread.interrupt();
    closeValve();
  }

  @Override
  public void run()
  {
    while (!Thread.interrupted())
    {
      try
      {
        waitForSensorToGoActive();

        Thread.sleep(startDelay);

        if (!sensor.isActive())
          continue;

        openValve();
        waitForFillToStart();

        boolean timeout = waitForSensorToGoInactiveWithTimeout();
        closeValve();

        if (timeout)
          waitForSensorToGoInactive();
      }
      catch (InterruptedException e)
      {}
    }
  }

  @Override
  public void handleEvent(Event event)
  {
    String sensorName = (String) event.getProperty("name");
    
    if(!sensor.getName().equals(sensorName))
    {
      log(LogService.LOG_INFO, name + " is ignoring sensor change event for sensor: " + sensorName);
      return;
    }
    
    boolean sensorActive = (boolean) event.getProperty("active");
    log(LogService.LOG_INFO, name + " is responding to sensor change event for sensor: " + sensorName + " active: " + sensorActive);

    if (sensorActive)
      startFill();
    else
      stopFill();
  }

  @Reference(unbind = "-")
  public void bindValve(Valve valve)
  {
    this.valve = valve;
  }

  @Reference(unbind = "-")
  public void bindContactSensor(ContactSensor sensor)
  {
    this.sensor = sensor;
  }

  @Reference(unbind = "-")
  public void bindEventAdmin(EventAdmin eventAdmin)
  {
    this.eventAdmin = eventAdmin;
  }

  private void startFill()
  {
    try
    {
      fillLock.lock();
      sensorActive.signal();
    }
    finally
    {
      fillLock.unlock();
    }
  }

  private void fillStarted()
  {
    try
    {
      fillLock.lock();
      filling = true;
      fillStarted.signal();
    }
    finally
    {
      fillLock.unlock();
    }
  }

  private void stopFill()
  {
    try
    {
      fillLock.lock();
      filling = false;
      sensorInactive.signal();
    }
    finally
    {
      fillLock.unlock();
    }
  }

  private void openValve()
  {
    if (!valve.isOpen())
    {
      valve.open().thenRun(() -> {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", name);
        properties.put("type", "start");
        startFillTime = System.currentTimeMillis();
        properties.put(EventConstants.TIMESTAMP, startFillTime);
        eventAdmin.sendEvent(new Event(FILL_EVENT, properties));
        log(LogService.LOG_INFO, name + " has started filling");
        fillStarted();
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
      valve.close();
      
      Map<String, Object> properties = new HashMap<>();
      properties.put("name", name);
      properties.put("type", "stop");
      properties.put(EventConstants.TIMESTAMP, System.currentTimeMillis());
      eventAdmin.sendEvent(new Event(FILL_EVENT, properties));
      
      properties = new HashMap<>();
      properties.put("name", name);
      properties.put("type", "fill");
      properties.put("fillTime", System.currentTimeMillis() - startFillTime);
      eventAdmin.sendEvent(new Event(FILL_EVENT, properties));

      log(LogService.LOG_INFO, name + " has stopped filling");
    }
    else
    {
      log(LogService.LOG_WARNING, name + " was requested to stop filling when valve was already closed - ignoring");
    }
  }

  private void waitForSensorToGoActive() throws InterruptedException
  {
    log(LogService.LOG_INFO, name + " is waiting for sensor to go active");
    fillLock.lock();

    try
    {
      while (!sensor.isActive())
        sensorActive.await();
    }
    finally
    {
      fillLock.unlock();
    }
  }

  private void waitForFillToStart() throws InterruptedException
  {
    log(LogService.LOG_INFO, name + " is waiting for fill to start");
    fillLock.lock();

    try
    {
      while (!filling)
        fillStarted.await();
    }
    finally
    {
      fillLock.unlock();
    }
  }

  private boolean waitForSensorToGoInactiveWithTimeout()
  {
    log(LogService.LOG_INFO, name + " is waiting for sensor to go inactive with timeout");
    fillLock.lock();

    try
    {
      if (!sensorInactive.await(fillTimeout, TimeUnit.MILLISECONDS))
        return true;

      Thread.sleep(stopDelay);
    }
    catch (InterruptedException e)
    {}
    finally
    {
      fillLock.unlock();
    }

    return false;
  }

  private void waitForSensorToGoInactive()
  {
    log(LogService.LOG_INFO, name + " is waiting for sensor to go inactive");
    fillLock.lock();

    try
    {
      while(sensor.isActive())
        sensorInactive.await();
    }
    catch (InterruptedException e)
    {}
    finally
    {
      fillLock.unlock();
    }
  }
}
