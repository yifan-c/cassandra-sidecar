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

import org.apache.cassandra.sidecar.adapters.trunk.EndpointSnitchJmxOperations;
import org.apache.cassandra.sidecar.adapters.trunk.RingProvider;
import org.apache.cassandra.sidecar.adapters.trunk.StorageJmxOperations;
import org.apache.cassandra.sidecar.common.JmxClient;
import org.apache.cassandra.sidecar.common.dns.DnsResolver;

/**
 * Ring provider that retrieves
 */
public class Cassandra311RingProvider extends RingProvider
{

    // The default storage port for an in-jvm dtest cluster using the multiple network interface
    // provisioning strategy.
    // If this were not a reference implementation for a soon-to-be EOL version of Cassandra,
    // we would push the instance configuration down to this class, so it could retrieve it
    // from the config rather than hard-coding it.
    public static final int DEFAULT_STORAGE_PORT = 7012;

    public Cassandra311RingProvider(JmxClient jmxClient, DnsResolver dnsResolver)
    {
        super(jmxClient, dnsResolver);
    }

    @Override
    protected StorageJmxOperations initializeStorageOps()
    {
        return new Cassandra311StorageProxyShim(jmxClient.proxy(StorageJmxOperations311.class,
                                                                StorageJmxOperations311.STORAGE_SERVICE_OBJ_NAME),
                                                DEFAULT_STORAGE_PORT);
    }

    @Override
    protected EndpointSnitchJmxOperations initializeEndpointProxy()
    {
        return new EndpointSnitchJmxOperationsShim(super.initializeEndpointProxy());
    }
}
