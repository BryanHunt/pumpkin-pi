package net.springfieldusa.web.device.sensor.contact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import net.springfieldusa.device.sensor.contact.ContactSensor;
import net.springfieldusa.web.json.api.JsonApiData;
import net.springfieldusa.web.json.api.JsonApiDataCollectionWrapper;
import net.springfieldusa.web.json.api.JsonApiDataWrapper;

@Path("/contactSensors")
@Produces("application/vnd.api+json")
@Component(service = ContactSensorResource.class)
public class ContactSensorResource {
  private CopyOnWriteArraySet<ContactSensor> sensors = new CopyOnWriteArraySet<>();
  
  @GET
  public JsonApiDataCollectionWrapper getSensors()
  {
    Collection<JsonApiData> data = new ArrayList<>();
    
    for(ContactSensor sensor : sensors)
    {
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("active", sensor.isActive());
      data.add(new JsonApiData(sensor.getName(), "contact-sensors", attributes, Collections.emptyMap(), Collections.emptyMap()));
    }
    
    return new JsonApiDataCollectionWrapper(data);
  }
  
  @GET
  @Path("/{id}")
  public JsonApiDataWrapper getSensor(@PathParam(value = "id") String id)
  {
    for(ContactSensor sensor : sensors)
    {
      if(!sensor.getName().equals(id))
        continue;
        
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("active", sensor.isActive());
      return new JsonApiDataWrapper(new JsonApiData(sensor.getName(), "contact-sensors", attributes, Collections.emptyMap(), Collections.emptyMap()));      
    }
    
    throw new NotFoundException();
  }
  
  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void bindContactSensor(ContactSensor contactSensor)
  {
    sensors.add(contactSensor);
  }
  
  public void unbindContactSensor(ContactSensor contactSensor)
  {
    sensors.remove(contactSensor);
  }
}
