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

package org.apache.cassandra.sidecar.routes.tokenrange;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Range;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.sidecar.testing.BootstrapBBUtils;
import org.apache.cassandra.testing.CassandraIntegrationTest;
import org.apache.cassandra.testing.ConfigurableCassandraTestContext;

/**
 * Cluster expansion scenarios integration tests for token range replica mapping endpoint with the in-jvm
 * dtest framework.
 *
 * Note: Some related test classes are broken down to have a single test case to parallelize test execution and
 * therefore limit the instance size required to run the tests from CircleCI as the in-jvm-dtests tests are memory bound
 */
@ExtendWith(VertxExtension.class)
public class JoiningTestSingleNode extends JoiningBaseTest
{
    @CassandraIntegrationTest(nodesPerDc = 5, newNodesPerDc = 1, network = true, buildCluster = false)
    void retrieveMappingWithJoiningNode(VertxTestContext context,
                                        ConfigurableCassandraTestContext cassandraTestContext) throws Exception
    {
        BBHelperSingleJoiningNode.reset();
        runJoiningTestScenario(context,
                               cassandraTestContext,
                               BBHelperSingleJoiningNode::install,
                               BBHelperSingleJoiningNode.transientStateStart,
                               BBHelperSingleJoiningNode.transientStateEnd,
                               generateExpectedRangeMappingSingleJoiningNode());
    }

    /**
     * Generates expected token range and replica mappings specific to the test case involving a 5 node cluster
     * with the additional node joining the cluster
     * <p>
     * Expected ranges are generated by adding RF replicas per range in increasing order. The replica-sets in subsequent
     * ranges cascade with the next range excluding the first replica, and including the next replica from the nodes.
     * eg.
     * Range 1 - A, B, C
     * Range 2 - B, C, D
     * <p>
     * Ranges that include the joining node will have [RF + no. joining nodes in replica-set] replicas with
     * the replicas being the existing nodes in ring-order.
     * eg.
     * Range 1 - A, B, C
     * Range 2 - B, C, D (with E being the joining node)
     * Expected Range 2 - B, C, D, E
     */
    private HashMap<String, Map<Range<BigInteger>, List<String>>> generateExpectedRangeMappingSingleJoiningNode()
    {
        List<Range<BigInteger>> expectedRanges = generateExpectedRanges();
        Map<Range<BigInteger>, List<String>> mapping = new HashMap<>();
        mapping.put(expectedRanges.get(0), Arrays.asList("127.0.0.1", "127.0.0.2", "127.0.0.3", "127.0.0.6"));
        mapping.put(expectedRanges.get(1), Arrays.asList("127.0.0.2", "127.0.0.3", "127.0.0.4", "127.0.0.6"));

        mapping.put(expectedRanges.get(2), Arrays.asList("127.0.0.2", "127.0.0.3", "127.0.0.4"));
        mapping.put(expectedRanges.get(3), Arrays.asList("127.0.0.3", "127.0.0.4", "127.0.0.5"));
        mapping.put(expectedRanges.get(4), Arrays.asList("127.0.0.4", "127.0.0.5", "127.0.0.1"));
        mapping.put(expectedRanges.get(5), Arrays.asList("127.0.0.5", "127.0.0.1", "127.0.0.2", "127.0.0.6"));
        mapping.put(expectedRanges.get(6), Arrays.asList("127.0.0.1", "127.0.0.2", "127.0.0.3", "127.0.0.6"));

        return new HashMap<String, Map<Range<BigInteger>, List<String>>>()
        {
            {
                put("datacenter1", mapping);
            }
        };
    }

    /**
     * ByteBuddy helper for a single joining node
     */
    public static class BBHelperSingleJoiningNode
    {
        static CountDownLatch transientStateStart = new CountDownLatch(1);
        static CountDownLatch transientStateEnd = new CountDownLatch(1);

        public static void install(ClassLoader cl, Integer nodeNumber)
        {
            // Test case involves 3 node cluster with 1 joining node
            // We intercept the bootstrap of the leaving node (4) to validate token ranges
            if (nodeNumber == 6)
            {
                BootstrapBBUtils.installSetBoostrapStateIntercepter(cl, BBHelperSingleJoiningNode.class);
            }
        }

        public static void setBootstrapState(SystemKeyspace.BootstrapState state, @SuperCall Callable<Void> orig) throws Exception
        {
            if (state == SystemKeyspace.BootstrapState.COMPLETED)
            {
                // trigger bootstrap start and wait until bootstrap is ready from test
                transientStateStart.countDown();
                awaitLatchOrTimeout(transientStateEnd, 2, TimeUnit.MINUTES, "transientStateEnd");
            }
            orig.call();
        }

        public static void reset()
        {
            transientStateStart = new CountDownLatch(1);
            transientStateEnd = new CountDownLatch(1);
        }
    }
}
