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
package net.springfieldusa.device.pi.opensprinkler.meta;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipselabs.emeta.AttributeDefinitionImpl;
import org.eclipselabs.emeta.IntegerAttributeDefinitionImpl;
import org.eclipselabs.emeta.ObjectClassDefinitionImpl;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

import net.springfieldusa.device.pi.opensprinkler.comp.OpenSprinkler;
import net.springfieldusa.device.valve.Valve;

@Component(service = MetaTypeProvider.class, property = { "metatype.factory.pid=net.springfieldusa.device.pi.opensprinkler.comp.SprinklerValve" })
public class SprinklerValveMetatypeProvider implements MetaTypeProvider
{
  private Set<Integer> availableZones = new HashSet<>();
  private AttributeDefinitionImpl zoneAttribute;

  @Override
  public ObjectClassDefinition getObjectClassDefinition(String id, String locale)
  {
    AttributeDefinitionImpl nameAttribute = new AttributeDefinitionImpl("name", "Name", AttributeDefinition.STRING);
    nameAttribute.setDescription("The valve name");

    AttributeDefinitionImpl priorityAttribute = new IntegerAttributeDefinitionImpl("priority", "Priority", 0, 100);
    priorityAttribute.setDescription("The valve priority");
    priorityAttribute.setDefaultValue(new String[] {"0"});
    
    ObjectClassDefinitionImpl ocd = new ObjectClassDefinitionImpl("net.springfieldusa.device.opensprinkler.comp.SprinklerValve", "Open Sprinkler Valve", "Valve configuration");
    ocd.addRequiredAttribute(nameAttribute);
    ocd.addRequiredAttribute(zoneAttribute);
    ocd.addRequiredAttribute(priorityAttribute);

    return ocd;
  }

  @Override
  public String[] getLocales()
  {
    return null;
  }

  @Reference(unbind = "-")
  public void bindOpenSprinkler(OpenSprinkler controller, Map<String, Object> properties)
  {
    int numberZones = (int) properties.get("numberZones");
    zoneAttribute = new IntegerAttributeDefinitionImpl("zone", "Zone", 0, numberZones);
    zoneAttribute.setDescription("The zone number");    

    for(int i = 0; i < numberZones; i++)
      availableZones.add(i);
    
    updateChoices();
  }
  
  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void bindWaterValve(Valve waterValve, Map<String, Object> properties)
  {
    availableZones.remove(properties.get("zone"));
    updateChoices();
  }

  public void unbindWaterValve(Valve waterValve, Map<String, Object> properties)
  {
    availableZones.add((Integer) properties.get("zone"));
    updateChoices();
  }
  
  private synchronized void updateChoices()
  {
    String[] labels = new String[availableZones.size()];
    
    Integer[] zones = availableZones.toArray(new Integer[0]);
    Arrays.sort(zones);
    
    for (int i = 0; i < zones.length; i++)
      labels[i] = zones[i].toString();
    
    zoneAttribute.setOptionLabels(labels);
    zoneAttribute.setOptionValues(labels);

    if (labels.length > 0)
      zoneAttribute.setDefaultValue(new String[] { labels[0] });
  }
}
