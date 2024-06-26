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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import com.codahale.metrics.DefaultSettableGauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.NoopMetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.vertx.core.Vertx;
import io.vertx.ext.dropwizard.ThroughputMeter;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.cassandra.sidecar.server.MainModule;
import org.apache.cassandra.sidecar.server.Server;
import org.apache.cassandra.sidecar.server.SidecarServerEvents;

import static org.apache.cassandra.sidecar.common.ResourceUtils.writeResourceToPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for filtering of metrics
 */
@ExtendWith(VertxExtension.class)
class FilteringMetricRegistryTest
{
    private static final MetricRegistry NO_OP_METRIC_REGISTRY = new NoopMetricRegistry();
    @TempDir
    private Path confPath;

    @AfterEach
    void cleanup()
    {
        SharedMetricRegistries.clear();
    }

    @Test
    void testNoopInstanceRetrieved()
    {
        MetricFilter.Regex testFilter = new MetricFilter.Regex("testMetric.*");
        MetricRegistryFactory registryFactory = new MetricRegistryFactory("cassandra_sidecar_" + UUID.randomUUID(),
                                                                          Collections.emptyList(),
                                                                          Collections.singletonList(testFilter));
        FilteringMetricRegistry metricRegistry = (FilteringMetricRegistry) registryFactory.getOrCreate();

        assertThat(metricRegistry.timer("testMetricTimer")).isSameAs(NO_OP_METRIC_REGISTRY.timer("any"));
        assertThat(metricRegistry.meter("testMetricMeter")).isSameAs(NO_OP_METRIC_REGISTRY.meter("any"));
        assertThat(metricRegistry.counter("testMetricCounter")).isSameAs(NO_OP_METRIC_REGISTRY.counter("any"));
        assertThat(metricRegistry.histogram("testMetricHistogram")).isSameAs(NO_OP_METRIC_REGISTRY.histogram("any"));

        metricRegistry.register("testMetricThroughputMeter", new ThroughputMeter());
        assertThat(metricRegistry.getIncludedMetrics()).doesNotContainKey("testMetricThroughputMeter");
    }

    @Test
    void testDuplicateMetricsNotAllowed()
    {
        MetricRegistry metricRegistry = new MetricRegistry();
        assertThat(metricRegistry.timer("testMetric")).isNotNull();
        assertThatThrownBy(() -> metricRegistry.meter("testMetric"))
        .isInstanceOf(IllegalArgumentException.class);

        MetricRegistryFactory registryFactory = new MetricRegistryFactory("cassandra_sidecar_" + UUID.randomUUID(),
                                                                          Collections.emptyList(),
                                                                          Collections.emptyList());
        FilteringMetricRegistry filteringMetricRegistry = (FilteringMetricRegistry) registryFactory.getOrCreate();

        filteringMetricRegistry.timer("testMetric");
        assertThatThrownBy(() -> filteringMetricRegistry.meter("testMetric"))
        .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGaugeMetricExclusion()
    {
        MetricFilter.Regex testFilter = new MetricFilter.Regex("testMetric.*");
        MetricRegistryFactory registryFactory = new MetricRegistryFactory("cassandra_sidecar_" + UUID.randomUUID(),
                                                                          Collections.emptyList(),
                                                                          Collections.singletonList(testFilter));
        FilteringMetricRegistry metricRegistry = (FilteringMetricRegistry) registryFactory.getOrCreate();

        assertThat(metricRegistry.gauge("testMetricGauge", () -> new DefaultSettableGauge<>(0L)))
        .isInstanceOf(DefaultSettableGauge.class);
        assertThat(metricRegistry.getIncludedMetrics()).doesNotContainKey("testMetricGauge");

        metricRegistry.register("testMetricDefaultSettableGaugeLong", new DefaultSettableGauge<>(0L));
        assertThat(metricRegistry.getIncludedMetrics()).doesNotContainKey("testMetricDefaultSettableGaugeLong");

        metricRegistry.register("testMetricDefaultSettableGaugeDouble", new DefaultSettableGauge<>(0d));
        assertThat(metricRegistry.getIncludedMetrics()).doesNotContainKey("testMetricDefaultSettableGaugeDouble");
    }

    @Test
    void testOneMatchingFilter()
    {
        MetricFilter.Equals exactFilter = new MetricFilter.Equals("sidecar.metric.exact");
        MetricFilter.Regex regexFilter = new MetricFilter.Regex("vertx.*");
        MetricRegistryFactory registryFactory = new MetricRegistryFactory("cassandra_sidecar_" + UUID.randomUUID(),
                                                                          Collections.singletonList(exactFilter),
                                                                          Collections.singletonList(regexFilter));
        FilteringMetricRegistry metricRegistry = (FilteringMetricRegistry) registryFactory.getOrCreate();

        metricRegistry.meter("sidecar.metric.exact");
        assertThat(metricRegistry.getIncludedMetrics()).containsKey("sidecar.metric.exact");
    }

    @Test
    void testMultipleMatchingFilter()
    {
        MetricFilter.Equals exactFilter = new MetricFilter.Equals("sidecar.metric.exact");
        MetricFilter.Regex regexFilter = new MetricFilter.Regex("sidecar.*");
        MetricRegistryFactory registryFactory = new MetricRegistryFactory("cassandra_sidecar_" + UUID.randomUUID(),
                                                                          Arrays.asList(exactFilter, regexFilter),
                                                                          Collections.emptyList());
        FilteringMetricRegistry metricRegistry = (FilteringMetricRegistry) registryFactory.getOrCreate();

        metricRegistry.meter("sidecar.metric.exact");
        assertThat(metricRegistry.getIncludedMetrics()).containsKey("sidecar.metric.exact");
    }

    @Test
    void testExcludingEqualsMetricFilter()
    {
        MetricFilter.Equals exactFilter = new MetricFilter.Equals("sidecar.metric.exact");
        MetricRegistryFactory registryFactory = new MetricRegistryFactory("cassandra_sidecar_" + UUID.randomUUID(),
                                                                          Collections.emptyList(),
                                                                          Collections.singletonList(exactFilter));
        FilteringMetricRegistry metricRegistry = (FilteringMetricRegistry) registryFactory.getOrCreate();

        metricRegistry.meter("sidecar.metric.exact");
        assertThat(metricRegistry.getIncludedMetrics()).doesNotContainKey("sidecar.metric.exact");
    }

    @Test
    void testExcludingRegexMetricFilter()
    {
        MetricFilter.Regex vertxFilter = new MetricFilter.Regex("vertx.*");
        MetricFilter.Regex sidecarFilter = new MetricFilter.Regex("sidecar.*");
        MetricRegistryFactory registryProvider = new MetricRegistryFactory("cassandra_sidecar_" + UUID.randomUUID(),
                                                                           Collections.singletonList(sidecarFilter),
                                                                           Collections.singletonList(vertxFilter));
        FilteringMetricRegistry metricRegistry = (FilteringMetricRegistry) registryProvider.getOrCreate();

        metricRegistry.meter("sidecar.metric.exact");
        assertThat(metricRegistry.getMetrics()).containsKey("sidecar.metric.exact");
        metricRegistry.timer("vertx.eventbus.message_transfer_time");
        assertThat(metricRegistry.getIncludedMetrics()).doesNotContainKey("vertx.eventbus.message_transfer_time");
    }

    @Test
    void testMultipleMatchingFilterWithOneExclude()
    {
        MetricFilter.Equals exactFilter = new MetricFilter.Equals("sidecar.metric.exact");
        MetricFilter.Regex regexFilter = new MetricFilter.Regex("sidecar.*");
        MetricRegistryFactory registryFactory = new MetricRegistryFactory("cassandra_sidecar_" + UUID.randomUUID(),
                                                                          Collections.singletonList(regexFilter),
                                                                          Collections.singletonList(exactFilter));
        FilteringMetricRegistry metricRegistry = (FilteringMetricRegistry) registryFactory.getOrCreate();

        metricRegistry.meter("sidecar.metric.exact");
        assertThat(metricRegistry.getIncludedMetrics()).doesNotContainKey("sidecar.metric.exact");
    }

    @Test
    void testExclusionsWithServer(VertxTestContext context)
    {
        ClassLoader classLoader = FilteringMetricRegistryTest.class.getClassLoader();
        Path yamlPath = writeResourceToPath(classLoader, confPath, "config/sidecar_metrics.yaml");
        Injector injector = Guice.createInjector(new MainModule(yamlPath));
        Server server = injector.getInstance(Server.class);
        Vertx vertx = injector.getInstance(Vertx.class);

        Checkpoint serverStarted = context.checkpoint();
        Checkpoint waitUntilCheck = context.checkpoint();

        vertx.eventBus().localConsumer(SidecarServerEvents.ON_SERVER_START.address(), message -> serverStarted.flag());

        server.start()
              .onFailure(context::failNow)
              .onSuccess(v -> {
                  MetricRegistryFactory registryFactory = injector.getInstance(MetricRegistryFactory.class);
                  Pattern excludedPattern = Pattern.compile("vertx.eventbus.*");
                  FilteringMetricRegistry globalRegistry = (FilteringMetricRegistry) registryFactory.getOrCreate();
                  assertThat(globalRegistry.getIncludedMetrics().size()).isGreaterThanOrEqualTo(1);
                  assertThat(globalRegistry.getIncludedMetrics().keySet().stream())
                  .noneMatch(key -> excludedPattern.matcher(key).matches());
                  waitUntilCheck.flag();
                  context.completeNow();
              });
    }

    @Test
    void testNoFiltering(VertxTestContext context)
    {
        ClassLoader classLoader = FilteringMetricRegistryTest.class.getClassLoader();
        Path yamlPath = writeResourceToPath(classLoader, confPath, "config/sidecar_metrics_empty_filters.yaml");
        Injector injector = Guice.createInjector(new MainModule(yamlPath));
        Server server = injector.getInstance(Server.class);
        Vertx vertx = injector.getInstance(Vertx.class);

        Checkpoint serverStarted = context.checkpoint();
        Checkpoint waitUntilCheck = context.checkpoint();

        vertx.eventBus().localConsumer(SidecarServerEvents.ON_SERVER_START.address(), message -> serverStarted.flag());

        server.start()
              .onFailure(context::failNow)
              .onSuccess(v -> {
                  MetricRegistryFactory registryFactory = injector.getInstance(MetricRegistryFactory.class);
                  FilteringMetricRegistry globalRegistry = (FilteringMetricRegistry) registryFactory.getOrCreate();
                  assertThat(globalRegistry.getIncludedMetrics().size()).isGreaterThanOrEqualTo(1);
                  waitUntilCheck.flag();
                  context.completeNow();
              });
    }
}
