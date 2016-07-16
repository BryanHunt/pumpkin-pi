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

import java.util.BitSet;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.service.GpioService;

import net.springfieldusa.comp.AbstractComponent;

@Component(service = OpenSprinkler.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class OpenSprinkler extends AbstractComponent
{
  public @interface Config
  {
    String name();

    int numberZones();
  }

  private String name;
  private int numberZones;
  private BitSet valveState;
  private volatile boolean active;
  private volatile GpioService gpioService;
  private GpioPinDigitalOutput shiftRegisterClockPin;
  private GpioPinDigitalOutput shiftRegisterOutputEnablePin;
  private GpioPinDigitalOutput shiftRegisterDataPin;
  private GpioPinDigitalOutput shiftRegisterLatchPin;

  private WaterManager waterManager;

  @Activate
  public void activate(Config config)
  {
    name = config.name();
    numberZones = config.numberZones();
    valveState = new BitSet(numberZones);

    shiftRegisterClockPin = gpioService.provisionDigitalOutputPin(RaspiPin.GPIO_07);
    shiftRegisterOutputEnablePin = gpioService.provisionDigitalOutputPin(RaspiPin.GPIO_00);

    shiftRegisterOutputEnablePin.setState(PinState.HIGH); // Disable the shift register OE

    shiftRegisterDataPin = gpioService.provisionDigitalOutputPin(RaspiPin.GPIO_02);
    shiftRegisterLatchPin = gpioService.provisionDigitalOutputPin(RaspiPin.GPIO_03);

    updateShiftRegister(); // Make sure all of the valves are off
    shiftRegisterOutputEnablePin.setState(PinState.LOW); // Enable the shift register OE

    waterManager = new WaterManager(this);
    waterManager.start();
    active = true;
  }

  @Deactivate
  public synchronized void deactivate()
  {
    waterManager.interrupt();
    valveState.clear();
    updateShiftRegister();
    active = false;
  }

  public CompletableFuture<Void> requestWater(SprinklerValve valve)
  {
    return waterManager.requestWater(valve);
  }

  public synchronized void openValve(int zone)
  {
    if(!active)
      return;
    
    if(!valveState.isEmpty())
    {
      String message = name + " attempted to open valve for zone " + zone + " when another valve was already open";
      log(LogService.LOG_ERROR, message);  
      throw new IllegalStateException(message);
    }
    
    log(LogService.LOG_INFO, name + " is opening valve for zone " + zone);
    valveState.set(zone);
    updateShiftRegister();
  }
  
  public synchronized void closeValve(int zone)
  {
    if(!active)
      return;
    
    log(LogService.LOG_INFO, name + " is closing valve for zone " + zone);
    valveState.clear(zone);
    updateShiftRegister();
    waterManager.releaseWater(zone);
  }

  @Reference(unbind = "-")
  public void bindGpioService(GpioService gpioService)
  {
    this.gpioService = gpioService;
  }

  private void updateShiftRegister()
  {
    shiftRegisterClockPin.setState(PinState.LOW);
    shiftRegisterLatchPin.setState(PinState.LOW);

    for (int i = numberZones - 1; i >= 0; i--)
    {
      shiftRegisterClockPin.setState(PinState.LOW);
      shiftRegisterDataPin.setState(valveState.get(i));
      shiftRegisterClockPin.setState(PinState.HIGH);
    }

    shiftRegisterLatchPin.setState(PinState.HIGH);
  }
}
