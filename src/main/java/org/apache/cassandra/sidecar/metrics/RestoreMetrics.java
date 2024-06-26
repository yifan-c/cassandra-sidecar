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

import java.util.Objects;

import com.codahale.metrics.DefaultSettableGauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import static org.apache.cassandra.sidecar.metrics.ServerMetrics.SERVER_PREFIX;

/**
 * Tracks metrics related to restore functionality provided by Sidecar.
 */
public class RestoreMetrics
{
    public static final String DOMAIN = SERVER_PREFIX + ".Restore";
    protected final MetricRegistry metricRegistry;
    public final NamedMetric<Timer> sliceReplicationTime;
    public final NamedMetric<Timer> jobCompletionTime;
    public final NamedMetric<DeltaGauge> successfulJobs;
    public final NamedMetric<DeltaGauge> failedJobs;
    public final NamedMetric<DefaultSettableGauge<Integer>> activeJobs;
    public final NamedMetric<DeltaGauge> tokenRefreshed;
    public final NamedMetric<DeltaGauge> tokenUnauthorized;
    public final NamedMetric<DeltaGauge> tokenExpired;

    public RestoreMetrics(MetricRegistry metricRegistry)
    {
        this.metricRegistry = Objects.requireNonNull(metricRegistry, "Metric registry can not be null");

        sliceReplicationTime
        = NamedMetric.builder(metricRegistry::timer).withDomain(DOMAIN).withName("SliceReplicationTime").build();
        jobCompletionTime
        = NamedMetric.builder(metricRegistry::timer).withDomain(DOMAIN).withName("JobCompletionTime").build();
        successfulJobs
        = NamedMetric.builder(name -> metricRegistry.gauge(name, DeltaGauge::new))
                     .withDomain(DOMAIN)
                     .withName("SuccessfulJobs")
                     .build();
        failedJobs
        = NamedMetric.builder(name -> metricRegistry.gauge(name, DeltaGauge::new))
                     .withDomain(DOMAIN)
                     .withName("FailedJobs")
                     .build();
        activeJobs
        = NamedMetric.builder(name -> metricRegistry.gauge(name, () -> new DefaultSettableGauge<>(0)))
                     .withDomain(DOMAIN)
                     .withName("ActiveJobs")
                     .build();
        tokenRefreshed
        = NamedMetric.builder(name -> metricRegistry.gauge(name, DeltaGauge::new))
                     .withDomain(DOMAIN)
                     .withName("TokenRefreshed")
                     .build();
        tokenUnauthorized
        = NamedMetric.builder(name -> metricRegistry.gauge(name, DeltaGauge::new))
                     .withDomain(DOMAIN)
                     .withName("TokenUnauthorized")
                     .build();
        tokenExpired
        = NamedMetric.builder(name -> metricRegistry.gauge(name, DeltaGauge::new))
                     .withDomain(DOMAIN)
                     .withName("TokenExpired")
                     .build();
    }
}
