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

import com.splunk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;

import static com.salesforce.dva.orchestra.util.Assert.requireArgument;

/**
 * Encapsulates basic Splunk API functionality.
 *
 * @author  Anand Subramanian (a.subramanian@salesforce.com)
 * @author  Tom Valine (tvaline@salesforce.com)
 */
public class SplunkService {

    //~ Static fields/initializers *******************************************************************************************************************

    private static final Logger LOGGER = LoggerFactory.getLogger(SplunkService.class);
    private static final long POLL_TIME_MS = 30000;

    //~ Instance fields ******************************************************************************************************************************

    private final int port;
    private final long queryTimeout;
    private final String userName;
    private final String password;
    private final String host;
    private Service splunkService;

    //~ Constructors *********************************************************************************************************************************

    /**
     * Creates a new SplunkService object.
     *
     * @param  userName      The user name used to access Splunk. Cannot be null.
     * @param  password      The password used to access Splunk. Cannot be null.
     * @param  host          The host name of the Splunk endpoint. Cannot be null.
     * @param  port          The port number of the Splunk endpoint. Must be positive.
     * @param  queryTimeout  The Splunk query timeout in milliseconds.
     */
    public SplunkService(String userName, String password, String host, int port, long queryTimeout) {
        requireArgument((this.userName = userName) != null, "Username cannot be null.");
        requireArgument((this.password = password) != null, "Password cannot be null.");
        requireArgument((this.host = host) != null, "Hostname cannot be null.");
        requireArgument((this.port = port) >= 0, "Illegal port specified.");
        requireArgument((this.queryTimeout = queryTimeout) > 0, "Query timeout must be greated than 0.");
        login();
    }

    //~ Methods **************************************************************************************************************************************

    /** Logs out of the Splunk service and terminates the connections. */
    public void close() {
        splunkService.logout();
    }

    /**
     * Executes a Splunk query. This method returns a reader object which must be disposed of by calling code when it is no longer needed.
     *
     * @param   query  The query to execute. Cannot be null.
     * @param   label  The query label used for informational purposes.
     *
     * @return  The results XML. Cannot be null.
     *
     * @throws  IOException  If an error reading the results occurs.
     */
    public ResultsReader querySplunkForEvents(String query, String label) throws IOException {
        assert (queryTimeout > 0) : "Timeout should not be less than or equal to 0.";

        JobArgs jobArgs = new JobArgs();
        JobResultsArgs resultArgs = new JobResultsArgs();

        jobArgs.setExecutionMode(JobArgs.ExecutionMode.NORMAL);
        resultArgs.setCount(0);

        Job job = splunkService.getJobs().create(query, jobArgs);
        long timeout = queryTimeout;
        boolean terminated = false;

        while (!(job.isReady() && job.isDone()) && !terminated) {
            if (timeout <= 0) {
                LOGGER.warn(MessageFormat.format("Query for {0} timed out after {1,number,0.00}s", label, getRunDuration(job)));
                job = terminateJob(job);
                terminated = true;
            } else {
                try {
                    Thread.sleep(POLL_TIME_MS);
                    LOGGER.info(MessageFormat.format("Awaiting results for {0}.  Query has been running for {1,number,0.00}s.", label,
                            getRunDuration(job)));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    LOGGER.warn(MessageFormat.format("Received a request to interrupt and terminate the query for {0}.", label));
                    job = terminateJob(job);
                    terminated = true;
                }
            }
            timeout -= POLL_TIME_MS;
        }
        LOGGER.debug(MessageFormat.format("Query for {0} has completed.", label));
        return terminated ? getNullResults() : new ResultsReaderXml(job.getResults(resultArgs));
    }

    private Job terminateJob(Job job) {
        try {
            return job.finish();
        } catch (HttpException ex) {
            LOGGER.debug("Failed to terminate job.", ex);
            return job;
        }
    }

    /* Helper to get the run duration considering unqueued jobs throw an exception. */
    private float getRunDuration(Job job) {
        try {
            return job.getRunDuration();
        } catch (Exception ex) {
            return 0.0f;
        }
    }

    /* Helper method to login. */
    private void login() {
        ServiceArgs loginArgs = new ServiceArgs();

        Service.setSslSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
        loginArgs.setUsername(userName);
        loginArgs.setPassword(password);
        loginArgs.setHost(host);
        loginArgs.setPort(port);
        splunkService = Service.connect(loginArgs);
    }

    private ResultsReader getNullResults() {
        ByteArrayInputStream bais = new ByteArrayInputStream("job,interrupted".getBytes(Charset.forName("UTF-8")));

        try {
            return new ResultsReaderCsv(bais);
        } catch (IOException ex) {
            assert false : "This should never happen.";
            throw new IllegalStateException(ex);
        }
    }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
