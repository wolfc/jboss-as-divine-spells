/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.divine.spells;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.io.IOException;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class ManagementAgentAttacher {
    private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

    // make sure this method signature does not expose tools.jar
    static String getLocalConnectorAddress(final int pid) throws IOException {
        // from tools.jar
        try {
            final VirtualMachine vm = VirtualMachine.attach(Integer.toString(pid));
            try {
                String localConnectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
                if (localConnectorAddress == null) {
                    final String agent = vm.getSystemProperties().getProperty("java.home") +
                            File.separator + "lib" + File.separator +
                            "management-agent.jar";
                    vm.loadAgent(agent);
                    localConnectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
                }
                return localConnectorAddress;
            } finally {
                vm.detach();
            }
        } catch (AgentInitializationException e) {
            throw new IOException(e);
        } catch (AgentLoadException e) {
            throw new IOException(e);
        } catch (AttachNotSupportedException e) {
            throw new IOException(e);
        }
    }
}
