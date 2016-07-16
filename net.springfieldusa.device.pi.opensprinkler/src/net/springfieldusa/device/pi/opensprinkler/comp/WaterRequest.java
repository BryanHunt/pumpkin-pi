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

public class WaterRequest implements Comparable<WaterRequest>
{
  private SprinklerValve valve;
  private CompletableFuture<Void> promise;
  
  public WaterRequest(SprinklerValve valve)
  {
    this.valve = valve;
    promise = new CompletableFuture<>();
  }

  public int getZone()
  {
    return valve.getZone();
  }
  
  public CompletableFuture<Void> getPromise()
  {
    return promise;
  }
  
  public void supplyWater()
  {
    valve.setOpen(true);
    promise.complete(null);
  }

  public void cancel()
  {
    promise.cancel(true);
  }
  
  @Override
  public int compareTo(WaterRequest other)
  {
    return this.valve.compareTo(other.valve);
  }
}
