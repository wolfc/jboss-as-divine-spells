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
    // MSC-84
    private static boolean dependencyMissing(CompositeData serviceStatus) {
        if (serviceStatus.containsKey("dependencyMissing"))
            return (Boolean) serviceStatus.get("dependencyMissing");
        else if (serviceStatus.containsKey("dependencyUnavailable"))
            return (Boolean) serviceStatus.get("dependencyUnavailable");
        throw new RuntimeException("Service status contains neither dependencyMissing (obsolete) nor dependencyUnavailable (MSC-84)");
    }

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
            final boolean dependencyMissing = dependencyMissing(serviceStatus);
            final boolean dependencyFailed = (Boolean) serviceStatus.get("dependencyFailed");
            if (dependencyMissing || dependencyFailed) {
                servicesMissingDependencies.add(serviceName);
            }
        }
        final Node root = new Node("Missing/failed Dependencies (condensed reverse dependencies)");
        final Map<String, Node> nodeMap = new HashMap<String, Node>();
        for (final String serviceName : servicesMissingDependencies) {
            node(services, serviceName, nodeMap, root);
        }
        if(root.hasChildren())
            show(root);
        else {
            assert servicesMissingDependencies.isEmpty();
            System.out.println("No services with missing dependencies found.");
        }
    }

    private static Node node(final Map<String, CompositeData> services, final String serviceName, final Map<String, Node> nodeMap, final Node root) {
        Node node = nodeMap.get(serviceName);
        if(node != null)
            return node;
        final StringBuffer message = new StringBuffer(serviceName);
        final CompositeData service = services.get(serviceName);
        if (service == null) {
            message.append(" ***MISSING***");
        } else {
            final String myState = (String) service.get("stateName");
            message.append(" (" + myState + ")");
            if (myState.equals("START_FAILED") && service.containsKey("exception"))
                message.append(": " + service.get("exception"));
        }
        node = new Node(message.toString());
        assert node != null : "node is null for " + serviceName;
        final Node prev = nodeMap.put(serviceName, node);
        assert prev == null : "prev is not null";
        if (service != null) {
            final String[] dependencies = (String[]) service.get("dependencies");
            if(dependencies.length > 0)
                message.append(" -> ");
            for (final String dependency : dependencies) {
                final Node parent = node(services, dependency, nodeMap, root);
                assert parent != null : "can't find parent " + dependency;
                parent.addChild(node);
                String state;
                final CompositeData dependencyServiceState = services.get(dependency);
                if (dependencyServiceState == null)
                    state = "***MISSING***";
                else
                    state = (String) dependencyServiceState.get("stateName");
                if (!state.equals("UP")) {
                    message.append(" " + dependency + " (" + state + ")");
                }
            }
            node.setName(message.toString());
            final String myState = (String) service.get("stateName");
            if(!myState.equals("UP") && dependencies.length == 0)
                root.addChild(node);
        } else
            root.addChild(node);
        return node;
    }

    private static void show(final Node node) {
        show(node, -1);
    }

    private static void show(final Node node, final int indent) {
        // condensed tree, don't show nodes twice
        if(node.done())
            return;
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < indent; i++)
            sb.append("  ");
        sb.append(node.getName());
        System.out.println(sb.toString());
        for(Node child : node.getChildren()) {
            show(child, indent + 1);
        }
    }
}
