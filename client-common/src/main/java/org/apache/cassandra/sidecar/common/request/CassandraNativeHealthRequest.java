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

import io.netty.handler.codec.http.HttpMethod;
import org.apache.cassandra.sidecar.common.ApiEndpointsV1;
import org.apache.cassandra.sidecar.common.response.HealthResponse;

/**
 * Represents a request to retrieve the connectivity health checks performed against the Cassandra native protocol
 */
public class CassandraNativeHealthRequest extends JsonRequest<HealthResponse>
{
    /**
     * Constructs a request to retrieve the Cassandra native health
     */
    public CassandraNativeHealthRequest()
    {
        this(false);
    }

    /**
     * Constructs a request to retrieve the Cassandra native health
     *
     * @param useDeprecatedHealthEndpoint {@code true} if using the deprecated endpoint, {@code false} to use
     *                                    the new endpoint
     */
    @SuppressWarnings("deprecation")
    public CassandraNativeHealthRequest(boolean useDeprecatedHealthEndpoint)
    {
        super(useDeprecatedHealthEndpoint
              ? ApiEndpointsV1.CASSANDRA_HEALTH_ROUTE
              : ApiEndpointsV1.CASSANDRA_NATIVE_HEALTH_ROUTE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpMethod method()
    {
        return HttpMethod.GET;
    }
}
