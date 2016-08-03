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

import com.salesforce.dva.orchestra.OrchestraException;
import com.splunk.Event;
import com.splunk.ResultsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.salesforce.dva.orchestra.util.Assert.requireArgument;

/**
 * Parses Splunk results.
 *
 * @author  Tom Valine (tvaline@salesforce.com)
 */
abstract class SplunkParser<T> {

    //~ Static fields/initializers *******************************************************************************************************************

    protected static final Logger LOGGER = LoggerFactory.getLogger(SplunkParser.class);

    //~ Instance fields ******************************************************************************************************************************

    protected final SimpleDateFormat _dateFormatter;
    protected final SplunkConfiguration _configuration;
    protected final Map<String, String> _keyMap;
    protected final Map<String, String> _metricMap;
    protected final Map<String, String> _tagMap;

    //~ Constructors *********************************************************************************************************************************

    /**
     * Creates a new SplunkParser object.
     *
     * @param  configuration  The Splunk configuration to use. Cannot be null.
     */
    SplunkParser(SplunkConfiguration configuration) {
        requireArgument(configuration != null, "Configuration cannot be null.");
        _configuration = configuration;
        _dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        _dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        _keyMap = configuration.getKeyMapping();
        _metricMap = configuration.getMetricMapping();
        _tagMap = configuration.getTagMapping();
    }

    //~ Methods **************************************************************************************************************************************

    /**
     * Parses the results from the reader according to the provided configuration.
     *
     * @param   reader       The reader to obtain results from.
     * @param   queryParams  The parameters used for the query.
     *
     * @return  The parsed results.
     */
    abstract List<T> parse(ResultsReader reader, List<String> queryParams);

    /**
     * Parses the collection scope from the event.
     *
     * @param   event        The event to parse. Cannot be null.
     * @param   queryParams  The query parameters used to obtain the result set from which the event was obtained.
     *
     * @return  The parsed scope.
     */
    protected String parseScope(Event event, List<String> queryParams) {
        return applySubstitutions(event, _configuration.getProperty(SplunkConfiguration.Parameter.SCOPE), queryParams);
    }

    /**
     * Parses the timestamp from the event.
     *
     * @param   event  The event to parse. Cannot be null.
     *
     * @return  The timestamp epoch milliseconds.
     *
     * @throws  OrchestraException  If the event format does not conform to MM/dd/yyyy:HH:MM:SS.
     */
    protected long parseTimestamp(Event event) {
        try {
            return _dateFormatter.parse(event.get(_configuration.getProperty(SplunkConfiguration.Parameter.TIMESTAMP))).getTime();
        } catch (ParseException ex) {
            throw new OrchestraException(ex);
        }
    }

    /**
     * Parses the tags from a given event.
     *
     * @param   event  The event to parse. Cannot be null.
     *
     * @return  A map of tag names to tag values.
     */
    protected Map<String, String> parseTags(Event event) {
        return parseMetrics(event, _tagMap);
    }

    /**
     * Parses the metrics from a given event.
     *
     * @param   event  The event to parse. Cannot be null.
     *
     * @return  A map of metric names to metric values.
     */
    protected Map<String, String> parseMetrics(Event event) {
        return parseMetrics(event, _metricMap);
    }

    private Map<String, String> parseMetrics(Event event, Map<String, String> nameToColumnMap) {
        Map<String, String> result = new HashMap<>(nameToColumnMap.size());

        for (Map.Entry<String, String> entry : nameToColumnMap.entrySet()) {
            result.put(entry.getKey(), event.get(entry.getValue()));
        }
        return result;
    }

    /**
     * Returns the annotation metric name against which annotations shall be stored.
     *
     * @return  The annotation metric name.
     */
    protected String getAnnotationMetric() {
        return _configuration.getProperty(SplunkConfiguration.Parameter.ANNOTATION_METRICNAME);
    }

    /**
     * Returns the annotation metric type for collected annotations.
     *
     * @return  The annotation type.
     */
    protected String getAnnotationType() {
        return _configuration.getProperty(SplunkConfiguration.Parameter.ANNOTATION_TYPE);
    }

    /**
     * Returns the annotation ID result set field from which the annotation source ID will be parsed.
     *
     * @return  The annotation ID field name.
     */
    protected String getAnnotationIdField() {
        return _configuration.getProperty(SplunkConfiguration.Parameter.ANNOTATION_ID_FIELD);
    }

    /**
     * Applies parameter and key substitutions to a string for a given event. This method replaces parameter and key references with their
     * corresponding values from the event and the query parameters used to obtain the result set.
     *
     * @param   event        The event from which key values shall be obtained. Cannot be null.
     * @param   pattern      The pattern on which substitution shall be performed. Cannot be null.
     * @param   queryParams  The query parameters used to obtain the event result set. Cannot be null.
     *
     * @return  The final text having all references substituted. Will never be null.
     */
    protected String applySubstitutions(Event event, String pattern, List<String> queryParams) {
        String result = pattern;

        for (int i = 0; i < queryParams.size(); i++) {
            result = result.replaceAll("\\$param\\." + i + "\\$", queryParams.get(i));
        }
        for (Map.Entry<String, String> keyEntry : _keyMap.entrySet()) {
            int index = Integer.parseInt(keyEntry.getKey());
            String value = event.get(keyEntry.getValue());

            result = result.replaceAll("\\$key\\." + index + "\\$", value);
        }
        return result;
    }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
