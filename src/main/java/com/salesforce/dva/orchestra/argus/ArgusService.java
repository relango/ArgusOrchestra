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
package com.salesforce.dva.orchestra.argus;

import com.salesforce.dva.orchestra.argus.entity.Annotation;
import com.salesforce.dva.orchestra.argus.entity.Metric;
import org.apache.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

import static com.salesforce.dva.orchestra.util.Assert.requireArgument;

/**
 * Provides read and write access to Argus. All annotations must be attached to a metric. Global annotations must be emulated by attaching them to a
 * dummy metric. This is to prevent the overpopulation of global metrics which will result in poor global annotation performance.
 *
 * @author  Tom Valine (tvaline@salesforce.com)
 * @author  Bhagyashree Shekhawat (bbhati@salesforce.com)
 */
public class ArgusService {

    //~ Instance fields ******************************************************************************************************************************

    private final ArgusHttpClient client;

    //~ Constructors *********************************************************************************************************************************

    private ArgusService(String tsdReadEndpoint, int maxConn, int connTimeout, int connRequestTimeout, boolean isPreview) {
        client = new ArgusHttpClient(tsdReadEndpoint, maxConn, connTimeout, connRequestTimeout, isPreview);
    }

    //~ Methods **************************************************************************************************************************************

    /**
     * Returns a new instance of the Argus service configured with 10 second timeouts.
     *
     * @param   endpoint   The HTTP endpoint for Argus.
     * @param   maxConn    The number of maximum connections. Must be greater than 0.
     * @param   isPreview  Set to true if the collector should skip actually submitting the data.
     *
     * @return  A new instance of the Argus service.
     */
    public static ArgusService getInstance(String endpoint, int maxConn, boolean isPreview) {
        return getInstance(endpoint, maxConn, 10000, 10000, isPreview);
    }

    /**
     * Returns a new instance of the Argus service.
     *
     * @param   endpoint            The HTTP endpoint for Argus.
     * @param   maxConn             The number of maximum connections. Must be greater than 0.
     * @param   connTimeout         The connection timeout in milliseconds. Must be greater than 0.
     * @param   connRequestTimeout  The connection request timeout in milliseconds. Must be greater than 0.
     * @param   isPreview           Set to true if the collector should skip actually submitting the data.
     *
     * @return  A new instance of the Argus service.
     */
    public static ArgusService getInstance(String endpoint, int maxConn, int connTimeout, int connRequestTimeout, boolean isPreview) {
        return new ArgusService(endpoint, maxConn, connTimeout, connRequestTimeout, isPreview);
    }

    //~ Methods **************************************************************************************************************************************

    /**
     * Closes the service connections and prepares the service for garbage collection. This method may be invoked on a service which has already been
     * disposed.
     */
    public void dispose() {
        client.dispose();
    }

    /**
     * Writes a metric datum.
     *
     * @param  metric  The metric datum to write. May not be null.
     */
    public void put(Metric metric) {
        requireArgument(metric != null, "Data point cannot be null.");
        put(Arrays.asList(new Metric[] { metric }));
    }

    /**
     * Writes metric data.
     *
     * @param  data  The metric data to write.
     */
    public void put(List<Metric> data) {
        requireArgument(data != null && !data.isEmpty(), "Data cannot be null or empty.");
        client.putMetricData(data);
    }

    /**
     * Create or update an annotation.
     *
     * @param  annotation  The annotation to add. Cannot be null.
     */
    public void putAnnotation(Annotation annotation) {
        requireArgument(annotation != null, "Annotation cannot be null.");
        putAnnotations(Arrays.asList(new Annotation[] { annotation }));
    }

    /**
     * Create or update global annotations.
     *
     * @param  annotations  The annotations to add. Cannot be null.
     */
    public void putAnnotations(List<Annotation> annotations) {
        requireArgument(annotations != null && !annotations.isEmpty(), "Data cannot be null or empty.");
        client.putAnnotationData(annotations);
    }

    /**
     * Logs into the web services.
     *
     * @param  username  The username.
     * @param  password  The password.
     */
    public void login(String username, String password) {
        client.login(username, password);
    }

    /** Logs out of the web services. */
    public void logout() {
        client.logout();
    }

    /**
     * A utility for retrieving metrics via an expression argument.
     *
     * @param   expression  the metrics expression
     *
     * @return  the raw HttpResponse
     *
     * @throws  Exception  when things go wrong
     */
    public HttpResponse queryMetrics(String expression) throws Exception {
        return client.getMetricData(expression);
    }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
