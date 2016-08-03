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
package com.salesforce.dva.orchestra.domain;

import com.salesforce.dva.orchestra.argus.entity.Annotation;
import com.salesforce.dva.orchestra.argus.entity.Metric;
import java.util.Queue;

/**
 * Domain specific reader for querying time series data from a legacy system for uploading into TSDB. Implementations of this interface are used for
 * pulling data into TSDB. Classes implementing this interface are responsible for configuring and/or initializing themselves as required during
 * invocation.
 *
 * <p>All domain reader implementations must provide a public default constructor.</p>
 *
 * @author  tvaline
 */
public interface DomainReader {

    //~ Methods **************************************************************************************************************************************

    /**
     * Indicates the reader has completed its collection and queued all the collected data points.
     *
     * @return  true If the reader has completed its collection and queued all the collected data points.
     */
    boolean isMetricCollectionDone();

    /**
     * Indicates the reader has completed its collection and queued all the collected data points.
     *
     * @return  true If the reader has completed its collection and queued all the collected data points.
     */
    boolean isAnnotationCollectionDone();

    /**
     * Queries time series data from a legacy system for uploading into TSDB.
     *
     * @param  metricQueue      The queue into which the reader will write collected metrics. Will never be null.
     * @param  annotationQueue  The queue into which the reader will write collected annotations. Will never be null.
     */
    void invokeCollection(Queue<Metric> metricQueue, Queue<Annotation> annotationQueue);

    /**
     * Returns the descriptive name of the data source this reader supports.
     *
     * @return  The name of the data source supported by this reader.
     */
    String getDatasource();
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
