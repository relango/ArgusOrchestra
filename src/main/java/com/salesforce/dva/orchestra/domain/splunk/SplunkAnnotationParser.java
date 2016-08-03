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
import com.splunk.Event;
import com.splunk.ResultsReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of the Splunk parser for annotation collection.
 *
 * @author  Tom Valine (tvaline@salesforce.com)
 */
class SplunkAnnotationParser extends SplunkParser<Annotation> {

    //~ Constructors *********************************************************************************************************************************

    /**
     * Creates a new SplunkAnnotationParser object.
     *
     * @param  configuration  The Splunk configuration to use. Cannot be null.
     */
    SplunkAnnotationParser(SplunkConfiguration configuration) {
        super(configuration);
    }

    //~ Methods **************************************************************************************************************************************

    @Override
    List<Annotation> parse(ResultsReader reader, List<String> queryParams) {
        List<Annotation> result = new LinkedList<>();

        if (reader != null) {
            Iterator<Event> iterator = reader.iterator();

            try {
                while (iterator.hasNext()) {
                    Event event = iterator.next();

                    result.add(parseAnnotation(event, queryParams));
                }
            } catch (Exception ex) {
                LOGGER.warn("Failed to parse annotation.", ex);
            } finally {
                try {
                    reader.close();
                } catch (IOException ex) {
                    LOGGER.warn("Failed to close result set.");
                    assert (false) : "This should never happen.";
                }
            }
        }
        return result;
    }

    private Annotation parseAnnotation(Event event, List<String> queryParams) {
        long timeStamp = parseTimestamp(event);
        String scope = parseScope(event, queryParams);
        String id = event.get(getAnnotationIdField());
        String type = getAnnotationType();
        String metric = getAnnotationMetric();
        String source = "splunk";
        Annotation result = new Annotation(source, id, type, scope, metric, timeStamp);

        result.setTags(parseTags(event));
        result.setFields(parseMetrics(event));
        LOGGER.debug("Parsed annotation: {}.", result);
        return result;
    }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
