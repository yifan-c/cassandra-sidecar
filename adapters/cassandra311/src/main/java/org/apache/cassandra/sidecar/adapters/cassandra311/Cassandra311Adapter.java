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

import org.apache.cassandra.sidecar.adapters.trunk.CassandraAdapter;
import org.apache.cassandra.sidecar.adapters.trunk.CassandraStorageOperations;
import org.apache.cassandra.sidecar.common.CQLSessionProvider;
import org.apache.cassandra.sidecar.common.ClusterMembershipOperations;
import org.apache.cassandra.sidecar.common.JmxClient;
import org.apache.cassandra.sidecar.common.StorageOperations;
import org.apache.cassandra.sidecar.common.dns.DnsResolver;

/**
 * A CassandraAdapter that works with Cassandra 3.11
 */
public class Cassandra311Adapter extends CassandraAdapter
{
    public Cassandra311Adapter(DnsResolver dnsResolver, JmxClient jmxClient, CQLSessionProvider session)
    {
        super(dnsResolver, jmxClient, session);
    }

    @Override
    public StorageOperations storageOperations()
    {
        return new CassandraStorageOperations(jmxClient, new Cassandra311RingProvider(jmxClient, dnsResolver));
    }

    @Override
    public ClusterMembershipOperations clusterMembershipOperations()
    {
        return new Cassandra311ClusterMembershipOperations(jmxClient);
    }

}
