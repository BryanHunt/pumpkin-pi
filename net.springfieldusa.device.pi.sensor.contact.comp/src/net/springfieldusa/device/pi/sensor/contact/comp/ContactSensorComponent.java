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
package net.springfieldusa.device.pi.sensor.contact.comp;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;

import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.gpio.service.GpioService;

import net.springfieldusa.comp.AbstractComponent;
import net.springfieldusa.device.sensor.contact.ContactSensor;

@Component(service = ContactSensor.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ContactSensorComponent extends AbstractComponent implements ContactSensor, GpioPinListenerDigital
{
  public @interface Config
  {
    String name();

    int sensorPinNumber();

    String pinPullResistance() default "OFF";

    boolean activeHigh() default true;

    long debounceTime() default 10000;
  }

  private String name;
  private volatile GpioService gpioService;
  private volatile EventAdmin eventAdmin;
  private GpioPinDigitalInput sensorPin;
  private boolean activeHigh;
  private long debounceTime;
  private Boolean debouncing = false;

  @Activate
  public void activate(Config config)
  {
    name = config.name();
    activeHigh = config.activeHigh();
    debounceTime = config.debounceTime();

    String sensorPinName = "GPIO " + config.sensorPinNumber();
    sensorPin = gpioService.provisionDigitalInputPin(RaspiPin.getPinByName(sensorPinName), PinPullResistance.valueOf(config.pinPullResistance()));
    sensorPin.addListener(this);
  }

  @Deactivate
  public void deactivate()
  {
    if (sensorPin != null)
    {
      sensorPin.removeListener(this);
      gpioService.unprovisionPin(sensorPin);
    }
  }

  @Override
  public boolean isActive()
  {
    return activeHigh ? sensorPin.isHigh() : sensorPin.isLow();
  }

  @Override
  public String getName()
  {
    return name;
  }
  
  @Reference(unbind = "-")
  public void bindGpioService(GpioService gpioService)
  {
    this.gpioService = gpioService;
  }

  @Reference(unbind = "-")
  public void bindEventAdmin(EventAdmin eventAdmin)
  {
    this.eventAdmin = eventAdmin;
  }
  
  @Override
  public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event)
  {
    synchronized (debouncing)
    {
      if (!debouncing)
      {
        debouncing = true;

        new Thread(() -> {
          boolean lastActive = isActive();

          try
          {
            Thread.sleep(debounceTime);
          }
          catch (InterruptedException e)
          {}

          boolean active = isActive();

          if (lastActive == active)
          {
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", name);
            properties.put(EventConstants.TIMESTAMP, System.currentTimeMillis());
            properties.put("active", active);
            eventAdmin.sendEvent(new Event(SENSOR_CHANGE_EVENT, properties));
          }
          
          synchronized (debouncing)
          {
            debouncing = false;
          }
        }).start();
      }
    }
  }
}
