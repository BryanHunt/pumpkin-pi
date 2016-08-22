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
package net.springfieldusa.device.sensor.contact;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <p>
 * This is an example of an interface that is expected to be implemented by Providers of the API.
 * Adding methods to this interface is a minor change, because only Providers will be affected.
 * </p>
 * 
 * @see ProviderType
 * @since 1.0
 */
@ProviderType
public interface ContactSensor
{
  String SENSOR_CHANGE_EVENT = "net/springfieldusa/device/sensor/contact";
  String getName();
  boolean isActive();
}
