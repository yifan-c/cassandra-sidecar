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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Representation of a metric name.
 */
public class MetricName
{
    private final String feature;
    private final String name;
    private final Set<Tag> tags;

    public MetricName(String feature, String name)
    {
        this(feature, name, Collections.emptySet());
    }

    public MetricName(String feature, String name, Tag... tags)
    {
        this(feature, name, new HashSet<>(Arrays.asList(tags)));
    }

    /**
     * Constructs a new instance of {@link MetricName} with given parameters.
     *
     * @param feature feature for which metric is captured
     * @param name metric name
     * @param tags additional name tags optionally added to metric name for more clarity. Tags are usually like,
     *             component=data, route=/stream/component, etc.
     */
    public MetricName(String feature, String name, Set<Tag> tags)
    {
        this.feature = Objects.requireNonNull(feature, "Feature can not be null");
        this.name = Objects.requireNonNull(name, "Name can not be null");
        this.tags = tags;
    }

    /**
     * Sidecar feature this metric is part of.
     * @return String
     */
    public String feature()
    {
        return feature;
    }

    /**
     * If applicable, additional name tags added to metric name for more clarity. Tags are usually like,
     * component=data, route=/stream/component, etc.
     * @return a set containing additional metric name tags
     */
    public Set<Tag> tags()
    {
        return tags;
    }

    /**
     * Metric name.
     * @return String
     */
    public String name()
    {
        return name;
    }

    @Override
    public String toString()
    {
        String featurePart = feature + ".";
        String combinedTags = tags != null && !tags.isEmpty() ? combineTags() + "." : "";
        return featurePart + combinedTags + name;
    }

    private String combineTags()
    {
        return tags.stream().map(tag -> tag.key + "=" + tag.value).collect(Collectors.joining("."));
    }

    /**
     * Used for tagging {@link MetricName} for additional context.
     */
    public static class Tag
    {
        private final String key;
        private final String value;

        public Tag(String key, String value)
        {
            this.key = Objects.requireNonNull(key, "Key can not be null");
            this.value = Objects.requireNonNull(value, "Value can not be null");
        }

        public String key()
        {
            return key;
        }

        public String value()
        {
            return value;
        }
    }
}
