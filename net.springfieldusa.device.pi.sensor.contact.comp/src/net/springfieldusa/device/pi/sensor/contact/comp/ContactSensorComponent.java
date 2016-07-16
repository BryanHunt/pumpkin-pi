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

import java.util.concurrent.CopyOnWriteArraySet;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;

import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.gpio.service.GpioService;

import net.springfieldusa.comp.AbstractComponent;
import net.springfieldusa.device.sensor.contact.ContactSensor;
import net.springfieldusa.device.sensor.contact.ContactSensorListener;

@Component(service = ContactSensor.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ContactSensorComponent extends AbstractComponent implements ContactSensor, GpioPinListenerDigital
{
  public @interface Config
  {
    String name();
    
    int sensorPinNumber();
    
    String pinPullResistance() default "OFF";
    
    boolean activeHigh() default true;
  }

  private String name;
  private volatile GpioService gpioService;
  private GpioPinDigitalInput sensorPin;
  private boolean activeHigh;
  private CopyOnWriteArraySet<ContactSensorListener> listeners = new CopyOnWriteArraySet<>();

  @Activate
  public void activate(Config config)
  {
    name = config.name();
    activeHigh = config.activeHigh();
    
    String sensorPinName = "GPIO " + config.sensorPinNumber();
    sensorPin = gpioService.provisionDigitalInputPin(RaspiPin.getPinByName(sensorPinName), PinPullResistance.valueOf(config.pinPullResistance()));
    sensorPin.addListener(this);
  }

  @Deactivate
  public void deactivate()
  {
    if (sensorPin != null)
      sensorPin.removeListener(this);
  }
  
  @Override
  public boolean isActive()
  {
    return activeHigh ? sensorPin.isHigh() : sensorPin.isLow();
  }
  
  @Override
  public void addListener(ContactSensorListener listener)
  {
    listeners.add(listener);
  }

  @Override
  public void removeListener(ContactSensorListener listener)
  {
    listeners.remove(listener);
  }

  @Reference(unbind = "-")
  public void bindGpioService(GpioService gpioService)
  {
    this.gpioService = gpioService;
  }

  @Override
  public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event)
  {
    log(LogService.LOG_INFO, "Sensor '" + name + "' is " + event.getState());
    boolean active = activeHigh ? sensorPin.isHigh() : sensorPin.isLow();
    
    for(ContactSensorListener listener : listeners)
      listener.sensorStateChanged(active);
  }
}
