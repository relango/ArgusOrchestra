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
package com.salesforce.dva.orchestra.domain.internal;

import com.salesforce.dva.orchestra.argus.entity.Annotation;
import com.salesforce.dva.orchestra.argus.entity.Metric;
import com.salesforce.dva.orchestra.domain.AbstractDomainReader;
import org.slf4j.LoggerFactory;
import java.security.SecureRandom;
import java.util.*;

/**
 * Domain reader to facilitate unit testing.
 *
 * @author  Tom Valine (tvaline@salesforce.com)
 */
public class UnitTestReader extends AbstractDomainReader {

    //~ Static fields/initializers *******************************************************************************************************************

    private static final SecureRandom RANDOM = new SecureRandom();

    //~ Instance fields ******************************************************************************************************************************

    private boolean done = false;

    //~ Constructors *********************************************************************************************************************************

    /** Creates a new instance of UnitTestReader. */
    public UnitTestReader() {
        super();
    }

    //~ Methods **************************************************************************************************************************************

    /**
     * Utility method to create a pseudo-random data point.
     *
     * @param   time  The timestamp of the data point.
     *
     * @return  A pseudo-random data point.
     */
    public static Metric createData(long time) {
        String scope = UnitTestReader.class.getName() + Long.toHexString(System.currentTimeMillis()) + Long.toHexString(RANDOM.nextLong());
        String metric = UnitTestReader.class.getName() + Long.toHexString(System.currentTimeMillis()) + Long.toHexString(RANDOM.nextLong());
        Map<String, String> tags = new TreeMap<>();

        tags.put("host", "localhost");

        Metric result = new Metric(scope, metric);

        result.setTags(tags);

        Map<Long, String> dps = new TreeMap<>();

        for (int i = 0; i < 10; i++) {
            dps.put((time - (i * 60000L)), String.valueOf(RANDOM.nextInt(100)));
        }
        result.setDatapoints(dps);
        return result;
    }

    /**
     * Utility method to create a pseudo-random data point.
     *
     * @param   time  The timestamp of the data point.
     *
     * @return  A pseudo-random data point.
     */
    public static Annotation createAnnotationData(long time) {
        String source = "unittest";
        String type = "test";
        String scope = UnitTestReader.class.getName() + Long.toHexString(System.currentTimeMillis()) + Long.toHexString(RANDOM.nextLong());
        String metric = UnitTestReader.class.getName() + Long.toHexString(System.currentTimeMillis()) + Long.toHexString(RANDOM.nextLong());
        Map<String, String> tags = new TreeMap<>();

        tags.put("host", "localhost");

        Annotation result = new Annotation(source, metric, type, scope, metric, time);

        result.setTags(tags);
        return result;
    }

    //~ Methods **************************************************************************************************************************************

    /**
     * Returns 10 sequential test data points.
     *
     * @param  metricQueue      The queue into which the reader will write collected metrics. Will never be null.
     * @param  annotationQueue  The queue into which the reader will write collected annotations. Will never be null.
     */
    @Override
    public void invokeCollection(Queue<Metric> metricQueue, Queue<Annotation> annotationQueue) {
        LoggerFactory.getLogger(getClass()).info("Generating test data points.");

        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 10; i++) {
            cal.add(Calendar.MINUTE, -i);
            metricQueue.add(createData(cal.getTimeInMillis()));
            try {
                Thread.sleep(250L);
            } catch (InterruptedException ex) {
                break;
            }
        }
        done = true;
    }

    @Override
    public boolean isMetricCollectionDone() {
        return done;
    }

    @Override
    public String getDatasource() {
        return "UNITTEST";
    }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
