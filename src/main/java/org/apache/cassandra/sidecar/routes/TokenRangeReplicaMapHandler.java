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

package org.apache.cassandra.sidecar.routes;

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.Metadata;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import org.apache.cassandra.sidecar.cluster.CassandraAdapterDelegate;
import org.apache.cassandra.sidecar.common.server.StorageOperations;
import org.apache.cassandra.sidecar.common.server.data.Name;
import org.apache.cassandra.sidecar.concurrent.ExecutorPools;
import org.apache.cassandra.sidecar.utils.CassandraInputValidator;
import org.apache.cassandra.sidecar.utils.HttpExceptions;
import org.apache.cassandra.sidecar.utils.InstanceMetadataFetcher;

import static org.apache.cassandra.sidecar.utils.HttpExceptions.cassandraServiceUnavailable;
import static org.apache.cassandra.sidecar.utils.HttpExceptions.wrapHttpException;

/**
 * Handler which provides token range to read and write replica mapping
 *
 * <p>This handler provides token range replicas along with the state of the replicas. For the purpose
 * of identifying the state of a newly joining node to replace a dead node from a newly joining node,
 * a new state 'Replacing' has been added.
 * It is represented by
 * {@code org.apache.cassandra.sidecar.adapters.base.TokenRangeReplicaProvider.StateWithReplacement}
 */
@Singleton
public class TokenRangeReplicaMapHandler extends AbstractHandler<Name>
{

    @Inject
    public TokenRangeReplicaMapHandler(InstanceMetadataFetcher metadataFetcher,
                                       CassandraInputValidator validator,
                                       ExecutorPools executorPools)
    {
        super(metadataFetcher, executorPools, validator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleInternal(RoutingContext context,
                               HttpServerRequest httpRequest,
                               String host,
                               SocketAddress remoteAddress,
                               Name keyspace)
    {
        CassandraAdapterDelegate delegate = metadataFetcher.delegate(host);
        if (delegate == null)
        {
            context.fail(cassandraServiceUnavailable());
            return;
        }

        StorageOperations operations = delegate.storageOperations();
        Metadata metadata = delegate.metadata();
        if (operations == null || metadata == null)
        {
            context.fail(cassandraServiceUnavailable());
            return;
        }

        executorPools.service()
                     .executeBlocking(() -> operations.tokenRangeReplicas(keyspace, metadata.getPartitioner()))
                     .onSuccess(context::json)
                     .onFailure(cause -> processFailure(cause, context, host, remoteAddress, keyspace));
    }

    @Override
    protected Name extractParamsOrThrow(RoutingContext context)
    {
        Name keyspace = keyspace(context, true);
        if (keyspace == null)
        {
            throw wrapHttpException(HttpResponseStatus.BAD_REQUEST, "'keyspace' is required but not supplied");
        }
        return keyspace;
    }

    @Override
    protected void processFailure(Throwable cause,
                                  RoutingContext context,
                                  String host,
                                  SocketAddress remoteAddress,
                                  Name keyspace)
    {
        if (cause instanceof AssertionError &&
            StringUtils.contains(cause.getMessage(), "Unknown keyspace"))
        {
            context.fail(HttpExceptions.wrapHttpException(HttpResponseStatus.NOT_FOUND, cause.getMessage()));
            return;
        }

        super.processFailure(cause, context, host, remoteAddress, keyspace);
    }
}
