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

package org.apache.cassandra.sidecar.common.request;

import java.util.UUID;

import io.netty.handler.codec.http.HttpMethod;
import org.apache.cassandra.sidecar.common.ApiEndpointsV1;
import org.apache.cassandra.sidecar.common.request.data.CreateSliceRequestPayload;

/**
 * Represents a request to create a restore job slice
 */
public class CreateRestoreJobSliceRequest extends Request
{
    private final CreateSliceRequestPayload payload;

    /**
     * Constructs a Sidecar request with the given {@code requestURI}
     *
     * @param keyspace name of the keyspace in the cluster
     * @param table    name of the table in the cluster
     * @param jobId    job ID of the restore job to create slice
     * @param payload  request payload
     */
    public CreateRestoreJobSliceRequest(String keyspace, String table, UUID jobId, CreateSliceRequestPayload payload)
    {
        super(requestURI(keyspace, table, jobId));
        this.payload = payload;
    }

    @Override
    public HttpMethod method()
    {
        return HttpMethod.POST;
    }

    @Override
    public Object requestBody()
    {
        return payload;
    }

    static String requestURI(String keyspace, String table, UUID jobId)
    {
        return ApiEndpointsV1.RESTORE_JOB_SLICES_ROUTE
               .replaceAll(ApiEndpointsV1.KEYSPACE_PATH_PARAM, keyspace)
               .replaceAll(ApiEndpointsV1.TABLE_PATH_PARAM, table)
               .replaceAll(ApiEndpointsV1.JOB_ID_PATH_PARAM, jobId.toString());
    }
}
