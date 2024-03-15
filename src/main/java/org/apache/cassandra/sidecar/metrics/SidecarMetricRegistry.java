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

package org.apache.cassandra.sidecar.metrics;

import java.util.ArrayList;
import java.util.List;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.NoopMetricRegistry;
import io.vertx.ext.dropwizard.Match;

// allows filtering to reject register
public class SidecarMetricRegistry extends MetricRegistry
{
    private List<Match> allowList;
    private NoopMetricRegistry noopMetricRegistry; // supplies no-op metrics

    /**
     * Check if the metric name is allowed to register
     * @param name
     * @return true if allowed; false otherwise
     */
    public boolean isAllowed(String metricName)
    {
        return true;
    }

    public void configureAllowList(List<Match> allowList)
    {
        this.allowList = allowList;
    }

    @Override
    public <T extends Gauge> T gauge(String name, MetricSupplier<T> supplier)
    {
        if (isAllowed(name))
        {
            return super.gauge(name, supplier);
        }

        // skip adding the metric to registry.
        // Note it cannot simply return NoopGauge, as the expected type T is different
        return supplier.newMetric();
    }

    @Override
    public Histogram histogram(String name, MetricSupplier<Histogram> supplier)
    {
        if (isAllowed(name))
        {
            return super.histogram(name, supplier);
        }

        return noopMetricRegistry.histogram(name);
    }

    // add more metric types
}
