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

import com.salesforce.dva.orchestra.argus.entity.Annotation;
import com.salesforce.dva.orchestra.argus.entity.Metric;
import org.junit.Before;
import org.junit.Test;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.assertTrue;

public class SplunkNativeReaderAnnotationIT {

    @Before
    public void setProps() throws IOException {
        Properties props = System.getProperties();
        File tempFile = File.createTempFile("test", ".properties");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile)); BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("SplunkNativeReaderAnnotationTest.properties")))) {
            String line;
            while ((line = br.readLine()) != null) {
                bw.append(line).append("\n");
            }   bw.append(SplunkConfiguration.Parameter.TIMEOUT_SEC.name().toLowerCase() + "=" + 600).append("\n");
        }
        tempFile.deleteOnExit();
        props.setProperty(SplunkNativeReader.class.getName() + ".configuration", tempFile.getAbsolutePath());
        System.setProperties(props);
    }

    @Test(timeout = 1000000)
    public void testMetricQuery() {
        Queue<Metric> metricQueue = new LinkedBlockingQueue<>();
        Queue<Annotation> annotationQueue = new LinkedBlockingQueue<>();
        SplunkNativeReader reader = new SplunkNativeReader();

        reader.invokeCollection(metricQueue, annotationQueue);
        assertTrue(annotationQueue.size() > 0);
    }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
