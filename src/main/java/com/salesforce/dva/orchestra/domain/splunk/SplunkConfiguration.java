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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Parses a Splunk configuration.
 *
 * <p>The supported configuration properties are:</p>
 *
 * <ul>
 *   <li>host - The fully qualified host name or IP address of the Splunk server. Defaults to localhost.</li>
 *   <li>port - The port number on the Splunk server to connect to. Defaults to 8214.</li>
 *   <li>username - The user name with which to connect to Splunk. (required)</li>
 *   <li>password - The password with which to connect to Splunk. (required)</li>
 *   <li>timeout_sec - The time in seconds after which an attempt will be made to interrupt and shutdown the collector. Defaults to 10000.</li>
 *   <li>worker_count - The number of worker threads used to parallelize collection. Defaults to 3.</li>
 *   <li>annotation_collection - True if an annotation collection is to be performed. Defaults to false.</li>
 *   <li>annotation_type - String literal for the type of annotation, "release" for example. Required for annotation collection. No default.</li>
 *   <li>annotation_metricname - String literal for the annotation metric. Required for annotation collection. Defaults to "global.annotations".</li>
 *   <li>annotation_id_field - String literal for the annotation id field. Required for annotation collection. Defaults to "id".</li>
 *   <li>timestamp - String literal for the 'MM/dd/yyyy hh:mm:ss' formatted timestamp field. Defaults to 'time'</li>
 *   <li>scope - The format pattern used to construct The collection scope. It may consist of string literals, metric substitutions, parameter
 *     substitutions and key substitutions. No default.</li>
 *   <li>query - The query to execute. May contain <tt>MessageFormat</tt> parameters to be populated by <tt>param.*</tt> values. (required)</li>
 *   <li>param.([\d]+) - A comma separated quoted list of parameter values used to drive the Splunk query parameters. All parameters must contain the
 *     same number of values which corresponds to the number of query iterations to execute.</li>
 *   <li>key.([\d]+) - A single key column which is a named field returned by the Splunk query and whose values are to be used in result
 *     formatting.</li>
 *   <li>metric.([\S]+) - The mapping between the Splunk result set column name and the Argus metric name or annotation field name. The portion after
 *     the <tt>metric.</tt> part of the key will be used as the raw metric name when applying the metric name format. For example, <tt>
 *     metric.mymetric=asplunkcolumn</tt> would map the raw metric name <tt>mymetric</tt> to the Splunk result set column named <tt>
 *     asplunkcolumn</tt>.</li>
 *   <li>tag.([\S]+) - The mapping between the Splunk result set column name and the Argus tag name. The portion after the <tt>tag.</tt> portion of
 *     the key will be used as the raw tag name when applying the tag name format. For example, <tt>tag.mytag=asplunkcolumn</tt> would map the raw tag
 *     name <tt>mytag</tt> to the Splunk result set column named <tt>asplunkcolumn</tt>.</li>
 * </ul>
 *
 * <p>Sample metric collection configuration.</p>
 *
 * <pre>
host=localhost
port=8000
username=splunkrobot
password=S4l3sF0rc3R0cks!
key.0=index
metric.querycount=querycount
metric.linecount=linecount
metricformat=$metric$
param.0="_audit"
query=search earliest=-30m@m index={0} | bucket_time span=10m | eval time=strftime(_time, "%m/%d/%Y %H:%M:%S") | stats count as querycount, sum(linecount) as linecount by time, index, splunk_server
scope=$key.0$
tag.server=splunk_server
tagformat=$tag$
timeout_sec=500
timestamp=time
* </pre>
 *
 * <p>Sample annotation collection configuration.</p>
 *
 * <pre>
host=localhost
port=8000
username=splunkrobot
password=S4l3sF0rc3R0cks!
key.0=index
metric.querycount=querycount
metric.linecount=linecount
param.0="_audit"
query=search earliest=-30m@m index={0} | bucket_time span=10m | eval time=strftime(_time, "%m/%d/%Y %H:%M:%S") | stats count as querycount, sum(linecount) as linecount by time, index, splunk_server
scope=$key.0$
timeout_sec=500
timestamp=time
annotation_collection=true
annotation_type=opsdata
annotation_metricname=querycounts
annotation_id_field=splunk_server
* </pre>
 *
 * @author  Anand Subramanian (a.subramanian@salesforce.com)
 * @author  Tom Valine (tvaline@salesforce.com)
 */
public class SplunkConfiguration {

    //~ Static fields/initializers *******************************************************************************************************************

    private static final Logger LOGGER = LoggerFactory.getLogger(SplunkConfiguration.class);

    //~ Instance fields ******************************************************************************************************************************

    private final Properties properties;

    //~ Constructors *********************************************************************************************************************************

    /**
     * Creates a new SplunkConfiguration object.
     *
     * @param  props  A set of properties read from disk.
     */
    public SplunkConfiguration(Properties props) {
        properties = props;
        for (String key : new TreeSet<>(props.stringPropertyNames())) {
            String msg = "Using configured value for {0} of : \"{1}\"";

            if (Parameter.QUERY.name().equalsIgnoreCase(key)) {
                LOGGER.info(MessageFormat.format(msg, key, props.getProperty(key)));
            } else if (!Parameter.PASSWORD.name().equalsIgnoreCase(key)) {
                LOGGER.debug(MessageFormat.format(msg, key, props.getProperty(key)));
            }
        }
    }

    //~ Methods **************************************************************************************************************************************

    /**
     * Returns a property value.
     *
     * @param   key  The property key value.
     *
     * @return  The property value or the default value if no value was explicitly set.
     */
    public String getProperty(Parameter key) {
        return properties.getProperty(key.name().toLowerCase(), key.defaultValue);
    }

    /**
     * Returns the metric mapping specified in the configuration file.
     *
     * @return  The metric mapping specified in the configuration file.
     */
    public Map<String, String> getMetricMapping() {
        return extractMapping(properties, "metric");
    }

    /**
     * Returns the tag mapping specified in the configuration file.
     *
     * @return  The tag mapping specified in the configuration file.
     */
    public Map<String, String> getTagMapping() {
        return extractMapping(properties, "tag");
    }

    /**
     * Returns the columns used as keys for the TSDB metric naming.
     *
     * @return  The column keys. Will never return null, but may be empty.
     */
    public Map<String, String> getKeyMapping() {
        return extractMapping(properties, "key");
    }

    /**
     * Returns the set of configured queries to run. If the <tt>SHMOO_TIME</tt> parameter has been set, the queries will be split having start and end
     * times resulting in one hour intervals from the start date to the final end date.
     *
     * @return  The set or configured queries to run.
     */
    public Map<String, List<String>> getQueries() {
        String query = getProperty(Parameter.QUERY);
        Map<String, List<String>> result = new LinkedHashMap<>();
        List<String[]> iterationParameters = loadQueryParameters(properties);

        if (iterationParameters.isEmpty()) {
            result.put(query, new LinkedList<String>());
        } else {
            for (String[] params : iterationParameters) {
                result.put(MessageFormat.format(query, (Object[]) params), Arrays.asList(params));
            }
        }
        return result;
    }

    /* Helper to load query parameters.  Assumes the query has already been extracted and is not null. */
    private List<String[]> loadQueryParameters(Properties properties) {
        Map<Long, String[]> parameterToValuesMap = new TreeMap<>();
        Map<String, String> parameterEntries = extractMapping(properties, "param");

        for (Entry<String, String> parameterEntry : parameterEntries.entrySet()) {
            String key = parameterEntry.getKey();
            String value = parameterEntry.getValue();
            String[] values = value.replaceAll("^\"", "").replaceAll("\"$", "").split("\"\\s*,\\s*\"");

            parameterToValuesMap.put(Long.valueOf(key), values);
        }

        int paramCount = parameterToValuesMap.size();
        List<String[]> result = new LinkedList<>();

        if (paramCount > 0) {
            int iterations = parameterToValuesMap.get(parameterToValuesMap.keySet().iterator().next()).length;

            for (int i = 0; i < iterations; i++) {
                List<String> parameters = new LinkedList<>();

                for (Map.Entry<Long, String[]> entry : parameterToValuesMap.entrySet()) {
                    parameters.add(entry.getValue()[i]);
                }
                result.add(parameters.toArray(new String[parameters.size()]));
            }
        }
        return result;
    }

    /* Helper to extract a mapping of values having a common key prefix. */
    private Map<String, String> extractMapping(Properties properties, String identifier) {
        Map<String, String> result = new LinkedHashMap<>();

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = entry.getKey().toString();

            if (key.startsWith(identifier + ".")) {
                result.put(key.replaceAll("^" + identifier + "\\.", ""), entry.getValue().toString());
            }
        }
        return result;
    }

    boolean isAnnotationCollection() {
        return Boolean.parseBoolean(getProperty(Parameter.ANNOTATION_COLLECTION));
    }

    //~ Enums ****************************************************************************************************************************************

    /**
     * The list of supported parameters not including <tt>param.*</tt>, <tt>metric.*</tt> and <tt>tag.*</tt>
     *
     * @author  Tom Valine (tvaline@salesforce.com)
     */
    public enum Parameter {

        /** The Splunk host name. Defaults to 'splunk-api.crz.salesforce.com'. */
        HOST("localhost"),
        /** The Splunk port number. Defaults to '8214'. */
        PORT("8214"),
        /** The user name with which to log into Splunk with. No default value. */
        USERNAME(""),
        /** The password with which to log into Splunk with. No default value. */
        PASSWORD(""),
        /** The timeout in seconds after which Splunk queries will be abandoned. Defaults to '10000'. */
        TIMEOUT_SEC("10000"),
        /** The number of worker threads to use. Defaults to '3'. */
        WORKER_COUNT("3"),
        /** Indicates the collection is an annotation collection. Defaults to false. */
        ANNOTATION_COLLECTION("false"),
        /** Label used to indicate the type of the annotation being collected. No default. */
        ANNOTATION_TYPE(""),
        /** Indicates the metric name on which the annotation should be stored. Defaults to 'global.annotations'. */
        ANNOTATION_METRICNAME("global.annotations"),
        /** Indicates the field that contains the data source specific annotation ID. Defaults to 'id'. */
        ANNOTATION_ID_FIELD("id"),
        /** The scope of the collection. Can contain string literal, parameter substitution or key substitution place holders. */
        SCOPE(""),
        /** Indicates the timestamp field. Defaults to 'time'. */
        TIMESTAMP("time"),
        /**
         * The query to execute. May have <tt>MessageFormat</tt> style substitution place holders to be populated by parameters specified in the
         * configuration.
         */
        QUERY("");

        /** The scope formatting pattern. No default value. */
        private final String defaultValue;

        private Parameter(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
