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
import org.apache.cassandra.sidecar.utils.InstanceMetadataFetcher;

import static org.apache.cassandra.sidecar.utils.HttpExceptions.cassandraServiceUnavailable;
import static org.apache.cassandra.sidecar.utils.HttpExceptions.wrapHttpException;

/**
 * A handler that provides ring information for the Cassandra cluster
 */
@Singleton
public class RingHandler extends AbstractHandler<Name>
{
    @Inject
    public RingHandler(InstanceMetadataFetcher metadataFetcher,
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

        StorageOperations storageOperations = delegate.storageOperations();

        if (storageOperations == null)
        {
            context.fail(cassandraServiceUnavailable());
            return;
        }

        executorPools.service()
                     .executeBlocking(() -> storageOperations.ring(keyspace))
                     .onSuccess(context::json)
                     .onFailure(cause -> processFailure(cause, context, host, remoteAddress, keyspace));
    }

    @Override
    protected void processFailure(Throwable cause,
                                  RoutingContext context,
                                  String host,
                                  SocketAddress remoteAddress,
                                  Name keyspace)
    {
        if (cause instanceof IllegalArgumentException &&
            StringUtils.contains(cause.getMessage(), ", does not exist"))
        {
            context.fail(wrapHttpException(HttpResponseStatus.NOT_FOUND, cause.getMessage(), cause));
            return;
        }

        super.processFailure(cause, context, host, remoteAddress, keyspace);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Name extractParamsOrThrow(RoutingContext context)
    {
        return keyspace(context, false);
    }
}
