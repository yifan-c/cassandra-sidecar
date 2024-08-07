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

package org.apache.cassandra.sidecar.adapters.base;

import java.net.InetSocketAddress;

import org.apache.cassandra.sidecar.common.server.CQLSessionProvider;
import org.apache.cassandra.sidecar.common.server.ICassandraAdapter;
import org.apache.cassandra.sidecar.common.server.ICassandraFactory;
import org.apache.cassandra.sidecar.common.server.JmxClient;
import org.apache.cassandra.sidecar.common.server.MinimumVersion;
import org.apache.cassandra.sidecar.common.server.dns.DnsResolver;
import org.apache.cassandra.sidecar.common.server.utils.DriverUtils;

/**
 * Factory to produce the 4.0 adapter
 */
@MinimumVersion("4.0.0")
public class CassandraFactory implements ICassandraFactory
{
    protected final DnsResolver dnsResolver;
    protected final DriverUtils driverUtils;

    public CassandraFactory(DnsResolver dnsResolver, DriverUtils driverUtils)
    {
        this.dnsResolver = dnsResolver;
        this.driverUtils = driverUtils;
    }

    /**
     * Returns a new adapter for Cassandra 4.0 clusters.
     *
     * @param session                     the session to the Cassandra database
     * @param jmxClient                   the JMX client to connect to the Cassandra database
     * @param localNativeTransportAddress the address and port on which this instance is configured to listen
     * @return a new adapter for the 4.0 clusters
     */
    @Override
    public ICassandraAdapter create(CQLSessionProvider session,
                                    JmxClient jmxClient,
                                    InetSocketAddress localNativeTransportAddress)
    {
        return new CassandraAdapter(dnsResolver, jmxClient, session, localNativeTransportAddress, driverUtils);
    }
}
