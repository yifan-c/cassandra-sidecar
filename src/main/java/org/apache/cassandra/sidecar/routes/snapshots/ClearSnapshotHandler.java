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

package org.apache.cassandra.sidecar.routes.snapshots;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import org.apache.cassandra.sidecar.cluster.CassandraAdapterDelegate;
import org.apache.cassandra.sidecar.common.StorageOperations;
import org.apache.cassandra.sidecar.concurrent.ExecutorPools;
import org.apache.cassandra.sidecar.data.SnapshotRequest;
import org.apache.cassandra.sidecar.routes.AbstractHandler;
import org.apache.cassandra.sidecar.utils.CassandraInputValidator;
import org.apache.cassandra.sidecar.utils.InstanceMetadataFetcher;

import static org.apache.cassandra.sidecar.utils.HttpExceptions.cassandraServiceUnavailable;
import static org.apache.cassandra.sidecar.utils.HttpExceptions.wrapHttpException;

/**
 * The <b>DELETE</b> verb deletes an existing snapshot for the given keyspace and table.
 */
@Singleton
public class ClearSnapshotHandler extends AbstractHandler<SnapshotRequest>
{
    @Inject
    public ClearSnapshotHandler(InstanceMetadataFetcher metadataFetcher,
                                CassandraInputValidator validator,
                                ExecutorPools executorPools)
    {
        super(metadataFetcher, executorPools, validator);
    }

    /**
     * Clears a snapshot for the given keyspace and table. **Note**: Currently, Cassandra does not support
     * the table parameter. We can add support in Cassandra for the additional parameter.
     *
     * @param context       the event to handle
     * @param httpRequest   the {@link HttpServerRequest} object
     * @param host          the name of the host
     * @param remoteAddress the remote address that originated the request
     * @param requestParams parameters obtained from the request
     */
    @Override
    public void handleInternal(RoutingContext context,
                               HttpServerRequest httpRequest,
                               String host,
                               SocketAddress remoteAddress,
                               SnapshotRequest requestParams)
    {
        executorPools.service().executeBlocking(promise -> {
            CassandraAdapterDelegate delegate = metadataFetcher.delegate(host(context));
            if (delegate == null)
            {
                promise.fail(cassandraServiceUnavailable());
                return;
            }

            StorageOperations storageOperations = delegate.storageOperations();
            if (storageOperations == null)
            {
                promise.fail(cassandraServiceUnavailable());
                return;
            }

            logger.debug("Clearing snapshot request={}, remoteAddress={}, instance={}",
                         requestParams, remoteAddress, host);
            storageOperations.clearSnapshot(requestParams.snapshotName(), requestParams.keyspace(),
                                            requestParams.tableName());
            context.response().end();
        }).onFailure(cause -> processFailure(cause, context, host, remoteAddress, requestParams));
    }

    @Override
    protected void processFailure(Throwable cause,
                                  RoutingContext context,
                                  String host,
                                  SocketAddress remoteAddress,
                                  SnapshotRequest requestParams)
    {
        logger.error("ClearSnapshotHandler failed for request={}, remoteAddress={}, instance={}, method={}",
                     requestParams, remoteAddress, host, context.request().method(), cause);
        if (cause instanceof FileNotFoundException || cause instanceof NoSuchFileException)
        {
            context.fail(wrapHttpException(HttpResponseStatus.NOT_FOUND, cause.getMessage()));
        }
        else
        {
            super.processFailure(cause, context, host, remoteAddress, requestParams);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SnapshotRequest extractParamsOrThrow(RoutingContext context)
    {
        SnapshotRequest snapshotRequest = SnapshotRequest.builder()
                                                         .qualifiedTableName(qualifiedTableName(context))
                                                         .snapshotName(context.pathParam("snapshot"))
                                                         .build();
        validate(snapshotRequest);
        return snapshotRequest;
    }

    private void validate(SnapshotRequest request)
    {
        validator.validateSnapshotName(request.snapshotName());
    }
}
