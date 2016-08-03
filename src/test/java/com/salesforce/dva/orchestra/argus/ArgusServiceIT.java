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

import com.salesforce.dva.orchestra.OrchestraException;
import com.salesforce.dva.orchestra.argus.entity.Annotation;
import com.salesforce.dva.orchestra.argus.entity.Metric;
import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

public class ArgusServiceIT extends ArgusAbstractTest {

    @Test
    public void testPreviewMode() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream sout = System.out;

        System.setOut(new PrintStream(baos));
        try {
            ArgusService service = ArgusService.getInstance(getEndpoint(), 2, true);

            service.login(getUsername(), getPassword());

            List<Metric> data = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
                data.add(createData());
            }
            service.put(data);
            service.logout();

            String json = baos.toString();

            assertNotNull(json);
        } finally {
            System.setOut(sout);
        }
    }

    @Test
    public void testMetricSubmission() {
        ArgusService service = ArgusService.getInstance(getEndpoint(), 2, false);

        service.login(getUsername(), getPassword());

        List<Metric> data = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            data.add(createData());
        }
        service.put(data);
        service.logout();
    }

    @Test
    public void testAnnotationSubmission() {
        ArgusService service = ArgusService.getInstance(getEndpoint(), 2, false);

        service.login(getUsername(), getPassword());

        List<Annotation> data = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            data.add(createAnnotationData());
        }
        service.putAnnotations(data);
        service.logout();
    }

    @Test(expected = OrchestraException.class)
    public void testUnauthenticated() {
        ArgusService service = ArgusService.getInstance(getEndpoint(), 2, false);
        List<Metric> data = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            data.add(createData());
        }
        service.put(data);
    }

    @Test(expected = OrchestraException.class)
    public void testBadEndpoint() {
        ArgusService service = ArgusService.getInstance("http://localhost:1024/foo", 2, false);
        List<Metric> data = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            data.add(createData());
        }
        service.put(data);
    }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
