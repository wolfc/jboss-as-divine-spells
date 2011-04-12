package org.jboss.as.divine.spells;

import sun.management.ConnectorAddressLink;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MissingDependency {
    private static String getLocalConnectorAddress(int pid) throws IOException {
        try {
            return ManagementAgentAttacher.getLocalConnectorAddress(pid);
        } catch (NoClassDefFoundError e) {
            if (e.getMessage().startsWith("com/sun/tools")) {
                System.err.println("Unable to find tools.jar, try adding -Xbootclasspath/a:$JAVA_HOME/lib/tools.jar");
            }
            throw e;
        }
    }

    public static void main(String args[]) throws IOException, MalformedObjectNameException, InstanceNotFoundException, ReflectionException, MBeanException {
        if (args.length != 1) {
            System.err.println("Usage: MissingDependency <pid>");
            System.exit(1);
        }
        final int pid = Integer.valueOf(args[0]);
        String localConnectorAddress = ConnectorAddressLink.importFrom(pid);
        if (localConnectorAddress == null)
            localConnectorAddress = getLocalConnectorAddress(pid);
        if (localConnectorAddress == null)
            throw new IllegalArgumentException("Can't find connector address link for " + pid);
        final JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(localConnectorAddress));
        final MBeanServerConnection connection = connector.getMBeanServerConnection();
        final ObjectName name = new ObjectName("jboss.msc:type=container,name=jboss-as");
        final CompositeData[] serviceStatuses = (CompositeData[]) connection.invoke(name, "queryServiceStatuses", null, null);
        final Map<String, CompositeData> services = new HashMap<String, CompositeData>();
        final List<String> servicesMissingDependencies = new LinkedList<String>();
        for (final CompositeData serviceStatus : serviceStatuses) {
            final String serviceName = (String) serviceStatus.get("serviceName");
            services.put(serviceName, serviceStatus);
            final boolean dependencyMissing = (Boolean) serviceStatus.get("dependencyMissing");
            final boolean dependencyFailed = (Boolean) serviceStatus.get("dependencyFailed");
            if (dependencyMissing || dependencyFailed) {
                servicesMissingDependencies.add(serviceName);
            }
        }
        for (final String serviceName : servicesMissingDependencies) {
            System.out.print(serviceName + " ");
            final CompositeData service = services.get(serviceName);
            final String myState = (String) service.get("stateName");
            if (myState.equals("START_FAILED")) {
                System.out.print("(START_FAILED)");
                if (service.containsKey("startExceptionMessage"))
                    System.out.print(": " + service.get("startExceptionMessage"));
            }
            else {
                System.out.print("-> ");
                final String[] dependencies = (String[]) service.get("dependencies");
                for (final String dependency : dependencies) {
                    String state;
                    final CompositeData dependencyServiceState = services.get(dependency);
                    if (dependencyServiceState == null)
                        state = "***MISSING***";
                    else
                        state = (String) dependencyServiceState.get("stateName");
                    if (!state.equals("UP")) {
                        System.out.print(" " + dependency + " (" + state + ")");
                    }
                }
            }
            System.out.println();
        }
        if (servicesMissingDependencies.isEmpty())
            System.out.println("No services with missing dependencies found.");
    }
}
