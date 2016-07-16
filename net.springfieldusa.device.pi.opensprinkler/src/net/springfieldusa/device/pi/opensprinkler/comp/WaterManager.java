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
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class WaterManager extends Thread
{
  private OpenSprinkler controller;
  private ReentrantLock lock;
  private Condition condition;
  private int activeZone;
  private boolean done;
  private PriorityBlockingQueue<WaterRequest> waterRequests = new PriorityBlockingQueue<>();

  public WaterManager(OpenSprinkler controller)
  {
    this.controller = controller;

    done = false;
    activeZone = -1;
    lock = new ReentrantLock();
    condition = lock.newCondition();
  }

  public CompletableFuture<Void> requestWater(SprinklerValve valve)
  {
    WaterRequest request = new WaterRequest(valve);
    waterRequests.add(request);
    return request.getPromise();
  }

  public void releaseWater(int zone)
  {
    if (zone == activeZone)
    {
      activeZone = -1;
      
      try
      {
        lock.lock();
        condition.signal();
      }
      finally
      {
        lock.unlock();
      }
    }
  }

  @Override
  public void run()
  {
    while (!done)
    {
      try
      {
        lock.lock();
        WaterRequest request = waterRequests.take();

        try
        {
          controller.openValve(request.getZone());
          request.supplyWater();
          activeZone = request.getZone();
        }
        catch (IllegalStateException e)
        {
          request.cancel();
        }

        condition.await();
      }
      catch (InterruptedException e)
      {
        done = true;
      }
      finally
      {
        lock.unlock();
      }
    }
  }
}
