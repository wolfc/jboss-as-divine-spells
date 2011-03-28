package org.jboss.as.divine.spells;

import sun.management.ConnectorAddressLink;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MissingDependency {
    public static void main(String args[]) throws IOException, MalformedObjectNameException, InstanceNotFoundException, ReflectionException, MBeanException {
        if (args.length != 1) {
            System.err.println("Usage: MissingDependency <pid>");
            System.exit(1);
        }
        final int pid = Integer.valueOf(args[0]);
        final String spec = ConnectorAddressLink.importFrom(pid);
        if (spec == null)
            throw new IllegalArgumentException("Can't find connector address link for " + pid);
        final JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(spec));
        final MBeanServerConnection connection = connector.getMBeanServerConnection();
        final ObjectName name = new ObjectName("jboss.msc:type=container,name=jboss-as");
        final CompositeData[] serviceStatuses = (CompositeData[]) connection.invoke(name, "queryServiceStatuses", null, null);
        final Map<String, String> services = new HashMap<String, String>();
        final Map<String, CompositeData> servicesMissingDependencies = new HashMap<String, CompositeData>();
        for (final CompositeData serviceStatus : serviceStatuses) {
            final String serviceName = (String) serviceStatus.get("serviceName");
            final String stateName = (String) serviceStatus.get("stateName");
            services.put(serviceName, stateName);
            boolean dependencyMissing = (Boolean) serviceStatus.get("dependencyMissing");
            if (dependencyMissing) {
                servicesMissingDependencies.put(serviceName, serviceStatus);
            }
        }
        for (final Map.Entry<String, CompositeData> service : servicesMissingDependencies.entrySet()) {
            System.out.print(service.getKey() + " -> ");
            final String[] dependencies = (String[]) service.getValue().get("dependencies");
            for (final String dependency : dependencies) {
                String state = services.get(dependency);
                if (state == null)
                    state = "***MISSING***";
                if (!state.equals("UP")) {
                    System.out.print(" " + dependency + " (" + state + ")");
                }
            }
            System.out.println();
        }
        if (servicesMissingDependencies.isEmpty())
            System.out.println("No services with missing dependencies found.");
    }
}
