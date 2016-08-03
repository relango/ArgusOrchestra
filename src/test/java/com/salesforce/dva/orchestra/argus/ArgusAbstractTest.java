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
import com.salesforce.dva.orchestra.domain.internal.UnitTestReader;
import com.salesforce.dva.orchestra.util.Configuration;
import org.junit.Ignore;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Ignore
public abstract class ArgusAbstractTest {

    private static final long HBASE_LATENCY = 5000;

    protected ArgusAbstractTest() {
        Properties testConfiguration = new Properties();

        try(InputStream stream = getClass().getResourceAsStream("/orchestra.properties")) {
            testConfiguration.load(stream);
            Configuration.load(testConfiguration);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    protected static String getUsername() {
        return Configuration.getParameter(Configuration.Parameter.ARGUSWS_USERNAME);
    }

    protected static String getPassword() {
        return Configuration.getParameter(Configuration.Parameter.ARGUSWS_PASSWORD);
    }

    protected static String getEndpoint() {
        return Configuration.getParameter(Configuration.Parameter.ARGUSWS_ENDPOINT);
    }

    protected Metric createData() {
        long seed = System.currentTimeMillis();
        long time = seed - (1800000);

        return UnitTestReader.createData(time);
    }

    protected Annotation createAnnotationData() {
        long seed = System.currentTimeMillis();
        long time = seed - (1800000);

        return UnitTestReader.createAnnotationData(time);
    }

    protected void waitForCommit() {
        try {
            Thread.sleep(HBASE_LATENCY);
        } catch (InterruptedException ex) {
            assert (false) : "This should never occur";
        }
    }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
