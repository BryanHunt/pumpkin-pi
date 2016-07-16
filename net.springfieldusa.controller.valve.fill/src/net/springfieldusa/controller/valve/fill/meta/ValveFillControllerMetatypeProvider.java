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
package net.springfieldusa.controller.valve.fill.meta;

import java.util.Map;

import org.eclipselabs.emeta.AttributeDefinitionImpl;
import org.eclipselabs.emeta.IntegerAttributeDefinitionImpl;
import org.eclipselabs.emeta.ObjectClassDefinitionImpl;
import org.eclipselabs.emeta.ServiceChoiceAttributeDefinitionImpl;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

import net.springfieldusa.device.sensor.contact.ContactSensor;
import net.springfieldusa.device.valve.Valve;

@Component(service = MetaTypeProvider.class, property = { "metatype.factory.pid=net.springfieldusa.controller.valve.fill.comp.ValveFillController" })
public class ValveFillControllerMetatypeProvider implements MetaTypeProvider
{
  private ServiceChoiceAttributeDefinitionImpl valveAttribute;
  private ServiceChoiceAttributeDefinitionImpl contactSensorAttribute;

  public ValveFillControllerMetatypeProvider()
  {
    valveAttribute = new ServiceChoiceAttributeDefinitionImpl("valve", "Valve", "name");
    valveAttribute.setDescription("The valve to use for filling");

    contactSensorAttribute = new ServiceChoiceAttributeDefinitionImpl("contactSensor", "Contact Sensor", "name");
    contactSensorAttribute.setDescription("The sensor that monitors the fluid level");
  }

  @Override
  public ObjectClassDefinition getObjectClassDefinition(String id, String locale)
  {
    AttributeDefinitionImpl nameAttribute = new AttributeDefinitionImpl("name", "Name", AttributeDefinition.STRING);
    nameAttribute.setDescription("The valve name");

    IntegerAttributeDefinitionImpl fillTimeoutAttribute = new IntegerAttributeDefinitionImpl("fillTimeout", "Fill Timeout");
    fillTimeoutAttribute.setDescription("Fill timeout in minutes");
    
    ObjectClassDefinitionImpl ocd = new ObjectClassDefinitionImpl("net.springfieldusa.controller.valve.fill.comp.ValveFillController", "Valve Fill Controller", "Valve fill controller configuration");
    ocd.addRequiredAttribute(nameAttribute);
    ocd.addRequiredAttribute(fillTimeoutAttribute);
    ocd.addRequiredAttribute(valveAttribute);
    ocd.addRequiredAttribute(contactSensorAttribute);

    return ocd;
  }

  @Override
  public String[] getLocales()
  {
    return null;
  }

  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void bindValve(Valve valve, Map<String, Object> properties)
  {
    valveAttribute.addService(properties);
  }

  public void unbindValve(Valve waterValve, Map<String, Object> properties)
  {
    valveAttribute.removeService(properties);
  }

  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void bindContactSensor(ContactSensor valve, Map<String, Object> properties)
  {
    contactSensorAttribute.addService(properties);
  }

  public void unbindContactSensor(ContactSensor waterValve, Map<String, Object> properties)
  {
    contactSensorAttribute.removeService(properties);
  }
}
