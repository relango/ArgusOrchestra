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

import com.splunk.ResultsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Callable;

import static com.salesforce.dva.orchestra.util.Assert.requireArgument;

/**
 * Performs the actual Splunk query for a pod.
 *
 * @param   <T>  The specific type of data this worker will process.
 *
 * @author  Anand Subramanian (a.subramanian@salesforce.com)
 * @author  Tom Valine (tvaline@salesforce.com)
 */
public class SplunkWorker<T> implements Callable<Boolean> {

    //~ Static fields/initializers *******************************************************************************************************************

    private static final Logger LOGGER = LoggerFactory.getLogger(SplunkService.class);

    //~ Instance fields ******************************************************************************************************************************

    private final Queue<T> queue;
    private final SplunkService service;
    private final String query;
    private final List<String> queryParams;
    private final SplunkParser parser;

    //~ Constructors *********************************************************************************************************************************

    /**
     * Creates a new Splunk worker.
     *
     * @param  service          The Splunk service instance to use. Cannot be null.
     * @param  queue            The queue in which to collect data points.
     * @param  query            The resolved query to execute.
     * @param  queryParameters  The list of parameters used to resolve the query being executed.
     * @param  parser           The Splunk configuration to use.
     */
    SplunkWorker(SplunkService service, Queue<T> queue, String query, List<String> queryParameters, SplunkParser<T> parser) {
        requireArgument((this.queue = queue) != null, "The queue cannot be null.");
        requireArgument((this.service = service) != null, "The Splunk service cannot be null.");
        requireArgument((this.query = query) != null, "The query cannot be null.");
        requireArgument((this.queryParams = queryParameters) != null, "The query parameters cannot be null.");
        requireArgument((this.parser = parser) != null, "The parser cannot be null.");
    }

    //~ Methods **************************************************************************************************************************************

    /**
     * Performs the query.
     *
     * @return  The results of the query. Will not be null, but may be empty.
     */
    @Override
    public Boolean call() {
        String params = Arrays.toString(queryParams.toArray(new String[queryParams.size()]));

        try {
            LOGGER.info("Dispatching query using: {}.", params);
            queue.addAll(querySplunk());
            return true;
        } catch (IOException ex) {
            LOGGER.warn(MessageFormat.format("An error occurred reading the result for {0}.  Aborting attempt.", params), ex);
            assert (false) : "This should never happen.";
        } // end try-catch
        return false;
    }

    private List<T> querySplunk() throws IOException {
        List<T> results = new ArrayList<>();
        ResultsReader resultSet = null;

        try {
            resultSet = service.querySplunkForEvents(query, Arrays.toString(queryParams.toArray(new String[queryParams.size()])));
            results.addAll(parser.parse(resultSet, queryParams));
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (IOException ex) {
                    LOGGER.warn("Failed to close results reader.", ex);
                    assert (false) : "This should never happen.";
                }
            }
        }
        return results;
    }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
