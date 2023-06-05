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

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cassandra.sidecar.adapters.trunk.StorageJmxOperations;

/**
 * A shim that adapts responses from a Cassandra 3.11 storage proxy instance
 * by enriching the results with the appropriate port numbers.
 */
public class Cassandra311StorageProxyShim implements StorageJmxOperations
{
    private final StorageJmxOperations311 probe;
    private final int port;

    public Cassandra311StorageProxyShim(StorageJmxOperations311 probe, int port)
    {
        this.probe = probe;
        this.port = port;
    }

    @Override
    public List<String> getLiveNodesWithPort()
    {
        return enrichWithPort(probe.getLiveNodes());
    }

    @Override
    public List<String> getUnreachableNodesWithPort()
    {
        return enrichWithPort(probe.getUnreachableNodes());
    }

    @Override
    public List<String> getJoiningNodesWithPort()
    {
        return enrichWithPort(probe.getJoiningNodes());
    }

    @Override
    public List<String> getLeavingNodesWithPort()
    {
        return enrichWithPort(probe.getLeavingNodes());
    }

    @Override
    public List<String> getMovingNodesWithPort()
    {
        return enrichWithPort(probe.getMovingNodes());
    }

    @Override
    public Map<String, String> getLoadMapWithPort()
    {
        return probe.getLoadMap();
    }

    @Override
    public Map<String, String> getTokenToEndpointWithPortMap()
    {
        return enrichWithPortHostSecond(probe.getTokenToEndpointMap());
    }

    @Override
    public Map<String, Float> effectiveOwnershipWithPort(String keyspace) throws IllegalStateException
    {
        return enrichInetAddressWithPort(probe.effectiveOwnership(keyspace));
    }

    @Override
    public Map<String, Float> getOwnershipWithPort()
    {
        return enrichInetAddressWithPort(probe.getOwnership());
    }

    @Override
    public Map<String, String> getEndpointWithPortToHostId()
    {
        return enrichWithPort(probe.getEndpointToHostId());
    }

    @Override
    public void takeSnapshot(String tag, Map<String, String> options, String... entities)
    {
        probe.takeSnapshot(tag, options, entities);
    }

    @Override
    public void clearSnapshot(String tag, String... keyspaceNames)
    {
        probe.clearSnapshot(tag, keyspaceNames);
    }

    private List<String> enrichWithPort(List<String> nodes)
    {
        return nodes.stream().map(this::enrichWithPort).collect(Collectors.toList());
    }


    private <T> Map<String, T> enrichWithPort(Map<String, T> map)
    {
        return map.entrySet().stream()
                  .collect(Collectors.toMap(
                           e -> enrichWithPort(e.getKey()),
                           Map.Entry::getValue)
                  );
    }

    private <T> Map<T, String> enrichWithPortHostSecond(Map<T, String> map)
    {
        return map.entrySet().stream()
                  .collect(Collectors.toMap(Map.Entry::getKey,
                                            e -> enrichWithPort(e.getValue())));
    }

    private <T> Map<String, T> enrichInetAddressWithPort(Map<InetAddress, T> map)
    {
        return map.entrySet().stream()
                  .collect(Collectors.toMap(
                           e -> enrichWithPort(e.getKey().getHostAddress()),
                           Map.Entry::getValue)
                  );
    }

    private String enrichWithPort(String host)
    {
        return host + ":" + port;
    }
}
