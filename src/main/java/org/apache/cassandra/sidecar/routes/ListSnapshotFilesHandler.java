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

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import org.apache.cassandra.sidecar.Configuration;
import org.apache.cassandra.sidecar.common.data.ListSnapshotFilesRequest;
import org.apache.cassandra.sidecar.common.data.ListSnapshotFilesResponse;
import org.apache.cassandra.sidecar.snapshots.SnapshotPathBuilder;

/**
 * ListSnapshotFilesHandler class lists paths of all the snapshot files of a given snapshot name.
 * Query param includeSecondaryIndexFiles is used to request secondary index files along with other files
 * For example:
 *
 * <p>
 * /api/v1/keyspace/ks/table/tbl/snapshots/testSnapshot
 * lists all SSTable component files for the "testSnapshot" snapshot for the "ks" keyspace and the "tbl" table
 * <p>
 * /api/v1/keyspace/ks/table/tbl/snapshots/testSnapshot?includeSecondaryIndexFiles=true
 * lists all SSTable component files including secondary index files for the "testSnapshot" snapshot for the "ks"
 * keyspace and the "tbl" table
 */
public class ListSnapshotFilesHandler extends AbstractHandler
{
    private static final Logger logger = LoggerFactory.getLogger(ListSnapshotFilesHandler.class);
    private static final String INCLUDE_SECONDARY_INDEX_FILES = "includeSecondaryIndexFiles";
    private final SnapshotPathBuilder builder;
    private final Configuration configuration;

    @Inject
    public ListSnapshotFilesHandler(SnapshotPathBuilder builder, Configuration configuration)
    {
        super(configuration.getInstancesConfig());
        this.builder = builder;
        this.configuration = configuration;
    }

    @Override
    public void handle(RoutingContext context)
    {
        final HttpServerRequest request = context.request();
        final String host = getHost(context);
        final SocketAddress remoteAddress = request.remoteAddress();
        final ListSnapshotFilesRequest requestParams = extractParamsOrThrow(context);
        logger.debug("ListSnapshotFilesHandler received request: {} from: {}. Instance: {}",
                     requestParams, remoteAddress, host);

        boolean secondaryIndexFiles = requestParams.includeSecondaryIndexFiles();

        builder.build(host, requestParams)
               .compose(file -> builder.listSnapshotDirectory(host, file, secondaryIndexFiles))
               .onSuccess(fileList ->
                          {
                              if (fileList.isEmpty())
                              {
                                  String payload = "Snapshot '" + requestParams.getSnapshotName() + "' not found";
                                  context.fail(new HttpException(HttpResponseStatus.NOT_FOUND.code(), payload));
                              }
                              else
                              {
                                  logger.debug("ListSnapshotFilesHandler handled {} for {}. Instance: {}",
                                               requestParams, remoteAddress, host);
                                  context.json(buildResponse(host, requestParams, fileList));
                              }
                          })
               .onFailure(cause ->
                          {
                              logger.error("ListSnapshotFilesHandler failed for request: {} from: {}. Instance: {}",
                                           requestParams, remoteAddress, host);
                              if (cause instanceof FileNotFoundException ||
                                  cause instanceof NoSuchFileException)
                              {
                                  context.fail(new HttpException(HttpResponseStatus.NOT_FOUND.code(),
                                                                 cause.getMessage()));
                              }
                              else
                              {
                                  context.fail(new HttpException(HttpResponseStatus.BAD_REQUEST.code(),
                                                                 "Invalid request for " + requestParams));
                              }
                          });
    }

    private ListSnapshotFilesResponse buildResponse(String host,
                                                    ListSnapshotFilesRequest request,
                                                    List<SnapshotPathBuilder.SnapshotFile> fileList)
    {
        ListSnapshotFilesResponse response = new ListSnapshotFilesResponse();
        String snapshotName = request.getSnapshotName();
        int sidecarPort = configuration.getPort();

        for (SnapshotPathBuilder.SnapshotFile snapshotFile : fileList)
        {
            response.addSnapshotFile(new ListSnapshotFilesResponse.FileInfo(snapshotFile.size(),
                                                                            host,
                                                                            sidecarPort,
                                                                            snapshotFile.dataDirectoryIndex(),
                                                                            snapshotName,
                                                                            snapshotFile.getKeyspace(),
                                                                            snapshotFile.getTableName(),
                                                                            snapshotFile.getFileName()));
        }
        return response;
    }

    private ListSnapshotFilesRequest extractParamsOrThrow(final RoutingContext context)
    {
        boolean includeSecondaryIndexFiles =
        "true".equalsIgnoreCase(context.request().getParam(INCLUDE_SECONDARY_INDEX_FILES, "false"));

        return new ListSnapshotFilesRequest(context.pathParam("keyspace"),
                                            context.pathParam("table"),
                                            context.pathParam("snapshot"),
                                            includeSecondaryIndexFiles
        );
    }
}
