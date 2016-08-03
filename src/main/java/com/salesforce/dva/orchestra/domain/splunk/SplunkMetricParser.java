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

import com.salesforce.dva.orchestra.argus.entity.Metric;
import com.splunk.Event;
import com.splunk.ResultsReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * The Splunk parser implementation for metric data collection.
 *
 * @author  Tom Valine (tvaline@salesforce.com)
 */
class SplunkMetricParser extends SplunkParser<Metric> {

    //~ Constructors *********************************************************************************************************************************

    /**
     * Creates a new SplunkParser object.
     *
     * @param  configuration  The Splunk configuration to use. Cannot be null.
     */
    SplunkMetricParser(SplunkConfiguration configuration) {
        super(configuration);
    }

    //~ Methods **************************************************************************************************************************************

    @Override
    List<Metric> parse(ResultsReader reader, List<String> queryParams) {
        Map<Metric, Map<Long, String>> metricMap = new HashMap<>();

        if (reader != null) {
            Iterator<Event> iterator = reader.iterator();

            try {
                while (iterator.hasNext()) {
                    Event event = iterator.next();

                    parseMetrics(event, queryParams, metricMap);
                }
            } finally {
                try {
                    reader.close();
                } catch (IOException ex) {
                    LOGGER.warn("Failed to close result set.");
                    assert (false) : "This should never happen.";
                }
            }
        }
        for (Entry<Metric, Map<Long, String>> entry : metricMap.entrySet()) {
            Metric metric = entry.getKey();

            metric.setDatapoints(entry.getValue());
            LOGGER.debug("Parsed metric {}.", metric);
        }
        return new ArrayList<>(metricMap.keySet());
    }

    private void parseMetrics(Event event, List<String> queryParams, Map<Metric, Map<Long, String>> metricMap) {
        long timeStamp = parseTimestamp(event);
        String scope = parseScope(event, queryParams);

        for (Entry<String, String> entry : parseMetrics(event).entrySet()) {
            String metricName = entry.getKey();
            String metricValue = entry.getValue();

            if (metricValue != null) {
                Metric metric = new Metric(scope, metricName);

                metric.setTags(parseTags(event));

                Map<Long, String> datapoints = metricMap.get(metric);

                if (datapoints == null) {
                    datapoints = new TreeMap<>();
                    metricMap.put(metric, datapoints);
                }
                datapoints.put(timeStamp, entry.getValue());
            }
        }
    }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
