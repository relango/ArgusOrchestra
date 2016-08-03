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

import com.salesforce.dva.orchestra.OrchestraException;
import com.salesforce.dva.orchestra.argus.entity.Annotation;
import com.salesforce.dva.orchestra.argus.entity.Metric;
import com.salesforce.dva.orchestra.domain.AbstractDomainReader;
import com.salesforce.dva.orchestra.domain.splunk.SplunkConfiguration.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.salesforce.dva.orchestra.util.Assert.requireArgument;

/**
 * A reader that queries information from Splunk.
 *
 * @author  Anand Subramanian (a.subramanian@salesforce.com)
 * @author  Tom Valine (tvaline@salesforce.com)
 * @see     SplunkConfiguration
 */
public class SplunkNativeReader extends AbstractDomainReader {

    //~ Instance fields ******************************************************************************************************************************

    Logger _logger = LoggerFactory.getLogger(SplunkNativeReader.class);
    private final AtomicBoolean done = new AtomicBoolean(false);
    private final SplunkConfiguration config;
    private final String host;
    private final String username;
    private final String password;
    private final int workerCount;
    private final int port;
    private final long timeout;

    //~ Constructors *********************************************************************************************************************************

    /** Creates a new SplunkNativeReader object. */
    public SplunkNativeReader() {
        super();
        config = new SplunkConfiguration(super.configuration);
        host = config.getProperty(Parameter.HOST);
        username = config.getProperty(Parameter.USERNAME);
        password = config.getProperty(Parameter.PASSWORD);
        workerCount = Integer.parseInt(config.getProperty(Parameter.WORKER_COUNT));
        port = Integer.parseInt(config.getProperty(Parameter.PORT));
        timeout = Long.parseLong(config.getProperty(Parameter.TIMEOUT_SEC));
        requireArgument(!StringUtils.isBlank(host), "Parameter, \"" + Parameter.HOST + "\", cannot be blank. Please check the properties file.");
        requireArgument(!StringUtils.isBlank(username),
            "Parameter, \"" + Parameter.USERNAME + "\", cannot be blank. Please check the properties file.");
        requireArgument(!StringUtils.isBlank(password),
            "Parameter, \"" + Parameter.PASSWORD + "\", cannot be blank. Please check the properties file.");
    }

    //~ Methods **************************************************************************************************************************************

    @Override
    public void invokeCollection(Queue<Metric> metricQueue, Queue<Annotation> annotationQueue) {
        Properties properties = new Properties();

        properties.putAll(configuration);
        requireArgument(host != null && !host.isEmpty(), "Host cannot be null or empty");
        requireArgument(username != null && !username.isEmpty(), "User name cannot be null or empty.");
        requireArgument(password != null && !password.isEmpty(), "Password cannot br null or empty.");
        if (config.isAnnotationCollection()) {
            String annotationType = config.getProperty(Parameter.ANNOTATION_TYPE);

            requireArgument(annotationType != null && !annotationType.isEmpty(), "Annotation type cannot be null or empty.");
        }

        SplunkService service = new SplunkService(username, password, host, port, timeout * 1000);

        _logger.info("Starting Splunk collection.");

        ExecutorService executor = null;

        try {
            executor = Executors.newFixedThreadPool(workerCount, new ThreadFactory() {

                    private long worker = 0;

                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread result = new Thread(runnable, "splunknativereader-" + worker++);

                        result.setDaemon(false);
                        return result;
                    }
                });

            boolean annotations = Boolean.valueOf(config.getProperty(Parameter.ANNOTATION_COLLECTION));
            Collection<SplunkWorker<? extends Object>> workers;

            if (annotations) {
                workers = getWorkers(service, annotationQueue, config, new SplunkAnnotationParser(config));
            } else {
                workers = getWorkers(service, metricQueue, config, new SplunkMetricParser(config));
            }
            executor.invokeAll(workers);
            executor.shutdown();
            executor.awaitTermination(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            if (!Thread.currentThread().isInterrupted()) {
                throw new OrchestraException(e);
            } else {
                _logger.warn("Splunk collection timed out.");
            }
        } finally {
            closeResources(service, executor);
            done.set(true);
            _logger.info("Splunk reader finished.");
        }
    }

    @Override
    public String getDatasource() {
        return "SPLUNK";
    }

    private <T> Collection<SplunkWorker<? extends Object>> getWorkers(SplunkService service, Queue<T> queue, SplunkConfiguration config,
        SplunkParser<T> parser) {
        Collection<SplunkWorker<? extends Object>> workers = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : config.getQueries().entrySet()) {
            workers.add(new SplunkWorker<>(service, queue, entry.getKey(), entry.getValue(), parser));
        }
        return workers;
    }

    private void closeResources(SplunkService service, ExecutorService executor) {
        if ((executor != null) && !executor.isTerminated()) {
            _logger.warn("Cleaning up collection workers.");
            executor.shutdownNow();
            try {
                executor.awaitTermination(20, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                _logger.warn("Forching shutdown of collection workers.");
            }
        }
        if (service != null) {
            try {
                service.close();
            } catch (Exception ex) {
                _logger.warn("Failed to close Splunk service.", ex);
                assert (false) : "This should never happen.";
            }
        }
    }

    @Override
    public boolean isMetricCollectionDone() {
        return done.get();
    }

    @Override
    public boolean isAnnotationCollectionDone() {
        return done.get();
    }

    @Override
    protected boolean isConfigurationRequired() {
        return true;
    }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
