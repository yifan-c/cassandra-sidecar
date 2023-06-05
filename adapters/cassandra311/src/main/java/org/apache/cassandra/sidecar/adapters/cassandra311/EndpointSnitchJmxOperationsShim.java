/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.sidecar.adapters.cassandra311;

import java.net.UnknownHostException;

import com.google.common.net.HostAndPort;

import org.apache.cassandra.sidecar.adapters.trunk.EndpointSnitchJmxOperations;

/**
 * A shim that adapts calls from 4.0+ consumers to 3.11, which has no port number in these calls.
 */
public class EndpointSnitchJmxOperationsShim implements EndpointSnitchJmxOperations
{
    private EndpointSnitchJmxOperations proxy;

    public EndpointSnitchJmxOperationsShim(EndpointSnitchJmxOperations jmxProxy)
    {
        this.proxy = jmxProxy;
    }

    @Override
    public String getRack(String hostWithPort) throws UnknownHostException
    {
        return proxy.getRack(removePort(hostWithPort));
    }

    @Override
    public String getDatacenter(String hostWithPort) throws UnknownHostException
    {
        return proxy.getDatacenter(removePort(hostWithPort));
    }

    private String removePort(String hostWithPort)
    {
        return HostAndPort.fromString(hostWithPort).getHost();
    }
}
