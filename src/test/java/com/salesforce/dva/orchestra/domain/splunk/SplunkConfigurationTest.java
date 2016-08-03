/*
 * Copyright (c) 2016, Salesforce.com, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of Salesforce.com nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.dva.orchestra.domain.splunk;

import com.salesforce.dva.orchestra.domain.splunk.SplunkConfiguration.Parameter;
import org.junit.Test;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;

public class SplunkConfigurationTest {

    @Test
    public void testParameterization() {
        Properties props = new Properties();

        props.setProperty(Parameter.QUERY.name().toLowerCase(), "{0} {1}");
        props.setProperty("param.0", "\"0\",\"2\"");
        props.setProperty("param.1", "\"1\",\"3\"");

        SplunkConfiguration config = new SplunkConfiguration(props);
        Set<String> querySet = config.getQueries().keySet();
        String[] queries = querySet.toArray(new String[querySet.size()]);

        assertTrue(queries.length == 2);
        assertEquals("0 1", queries[0]);
        assertEquals("2 3", queries[1]);
    }

    @Test
    public void testMetricMapping() {
        Properties props = new Properties();

        props.setProperty(Parameter.QUERY.name().toLowerCase(), "select something from somewhere");
        props.setProperty("metric.0", "0");
        props.setProperty("metric.1", "1");

        SplunkConfiguration config = new SplunkConfiguration(props);
        Map<String, String> metricMapping = config.getMetricMapping();

        assertTrue(metricMapping.containsKey("0"));
        assertEquals("0", metricMapping.get("0"));
        assertTrue(metricMapping.containsKey("1"));
        assertEquals("1", metricMapping.get("1"));
    }

    @Test
    public void testTagMapping() {
        Properties props = new Properties();

        props.setProperty(Parameter.QUERY.name().toLowerCase(), "select something from somewhere");
        props.setProperty("tag.0", "0");
        props.setProperty("tag.1", "1");

        SplunkConfiguration config = new SplunkConfiguration(props);
        Map<String, String> tagMapping = config.getTagMapping();

        assertTrue(tagMapping.containsKey("0"));
        assertEquals("0", tagMapping.get("0"));
        assertTrue(tagMapping.containsKey("1"));
        assertEquals("1", tagMapping.get("1"));
    }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
