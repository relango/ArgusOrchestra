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
package com.salesforce.dva.orchestra;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.salesforce.dva.orchestra.argus.ArgusService;
import com.salesforce.dva.orchestra.argus.entity.Annotation;
import com.salesforce.dva.orchestra.argus.entity.Metric;
import com.salesforce.dva.orchestra.domain.DomainReader;
import com.salesforce.dva.orchestra.domain.internal.UnitTestReader;
import com.salesforce.dva.orchestra.domain.splunk.SplunkNativeReader;
import com.salesforce.dva.orchestra.util.Configuration;
import com.salesforce.dva.orchestra.util.Option;
import org.slf4j.LoggerFactory;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.salesforce.dva.orchestra.argus.ArgusService.getInstance;
import static com.salesforce.dva.orchestra.util.Assert.requireArgument;
import static com.salesforce.dva.orchestra.util.Configuration.Parameter.ARGUSWS_ENDPOINT;
import static com.salesforce.dva.orchestra.util.Option.findOption;

/**
 * Populates metric data using a specified data source domain type.
 *
 * @author  Tom Valine (tvaline@salesforce.com)
 */
public class Collector {

    //~ Static fields/initializers *******************************************************************************************************************

    private static final Option HELP_OPTION;
    private static final Logger LOGGER;
    private static final Option TYPE_OPTION;
    private static final Option LEVEL_OPTION;
    private static final Option TIMEOUT_OPTION;
    private static final Option PREVIEW_OPTION;
    private static final Option[] TEMPLATES;
    private static final Map<String, Class<? extends DomainReader>> READERS;

    /* This class should not do any logging.  If there are any errors, it should throw a runtime exception or display the usage. */
    static {
        HELP_OPTION = Option.createFlag("-h", "Display the usage and available collector types.");
        TYPE_OPTION = Option.createOption("-t", "Mandatory option indicating the name of the collector type to invoke.");
        TIMEOUT_OPTION = Option.createOption("-s", "Optional timeout in seconds.  Defaults to 3600.");
        LEVEL_OPTION = Option.createOption("-l", "Optional log level.  Defaults to INFO.");
        PREVIEW_OPTION = Option.createFlag("-n", "Preview mode.  Writes output to stdout.");
        TEMPLATES = new Option[] { HELP_OPTION, TYPE_OPTION, LEVEL_OPTION, TIMEOUT_OPTION, PREVIEW_OPTION };
        READERS = new TreeMap<>();
        READERS.put("UNITTEST", UnitTestReader.class);
        READERS.put("SPLUNKNATIVE", SplunkNativeReader.class);
        LOGGER = (Logger) LoggerFactory.getLogger(Collector.class.getPackage().getName());
    }

    private static final long TIMEOUT_INTERVAL_MS = 14400000;
    private static final long POLL_INTERVAL_MS = 500;
    private static final int CHUNK_SIZE = 1000;
    private static final AtomicInteger ID = new AtomicInteger(1);

    //~ Instance fields ******************************************************************************************************************************

    private final String type;
    private final long timeoutMillis;
    private ArgusService service;

    //~ Constructors *********************************************************************************************************************************

    /**
     * Creates a new Collector object.
     *
     * @param  username       The username used to login to the Argus web services.
     * @param  password       The password used to login to the Argus web services.
     * @param  type           The type of the collector. Cannot be null and must be a supported collector.
     * @param  timeoutMillis  The collector timeout in milliseconds. Must be greater than zero.
     */
    public Collector(String username, String password, String type, long timeoutMillis) {
        this(username, password, type, timeoutMillis, false);
    }

    /**
     * Creates a new Collector object.
     *
     * @param   username       The username used to login to the Argus web services.
     * @param   password       The password used to login to the Argus web services.
     * @param   type           The type of the collector. Cannot be null and must be a supported collector.
     * @param   timeoutMillis  The collector timeout in milliseconds. Must be greater than zero.
     * @param   preview        Set to <tt>true</tt> to indicate the collector should run in preview mode and not actually submit results to Argus.
     *
     * @throws  OrchestraException  If an error occurs.
     */
    public Collector(String username, String password, String type, long timeoutMillis, boolean preview) {
        try {
            requireArgument((this.type = type) != null && READERS.keySet().contains(type), "Invalid reader type.");
            requireArgument((this.timeoutMillis = timeoutMillis) > 0, "The timeout in seconds must be greater than zero.");
            service = getInstance(Configuration.getParameter(ARGUSWS_ENDPOINT), 10, preview);
            service.login(username, password);
        } catch (Exception ex) {
            throw new OrchestraException(MessageFormat.format("Could not create a {0} collector.", type), ex);
        }
    }

    //~ Methods **************************************************************************************************************************************

    /**
     * Runs the collector with the specified command line options.
     *
     * @param   args  The command line options used to configure the collector.
     *
     * @throws  OrchestraException  If an error occurs.
     */
    public static void main(String[] args) {
        try {
            Option[] options = Option.parseCLArgs(args, TEMPLATES);
            Option level = (options == null) ? null : findOption(LEVEL_OPTION.getName(), options);
            Option help = (options == null) ? null : findOption(HELP_OPTION.getName(), options);
            Option type = (options == null) ? null : findOption(TYPE_OPTION.getName(), options);
            Option timeout = (options == null) ? null : findOption(TIMEOUT_OPTION.getName(), options);
            Option preview = (options == null) ? null : findOption(PREVIEW_OPTION.getName(), options);
            String username = Configuration.getParameter(Configuration.Parameter.ARGUSWS_USERNAME);
            String password = Configuration.getParameter(Configuration.Parameter.ARGUSWS_PASSWORD);

            configureLogging((level == null) ? "INFO" : level.getValue());

            long timeoutMillis = timeout == null ? 3600000 : Long.parseLong(timeout.getValue()) * 1000;

            if ((help == null) && (type != null)) {
                new Collector(username, password, type.getValue(), timeoutMillis, preview != null).invoke();
            } else {
                System.out.println(usage()); // NOSONAR
            }
        } catch (IllegalArgumentException ex) {
            System.out.println(usage()); // NOSONAR
        } catch (Exception e) {
            LOGGER.error("Error running orchestra collector: ", e);
            throw new OrchestraException(e);
        }
    }

    private static void configureLogging(String level) {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.DEBUG);
        try {
            LOGGER.setLevel(Level.toLevel(level));
        } catch (Exception ex) {
            LOGGER.setLevel(Level.INFO);
        }
    }

    private static String usage() {
        StringBuilder sb = new StringBuilder("Usage:\n");

        for (Option option : TEMPLATES) {
            sb.append(String.format("\t%1$-3s%2$s", option.getName(), option.getDescription())).append("\n");
        }
        sb.append("\nAvailable types (Refer to type specific API documentation for configuration options):\n");
        for (String type : READERS.keySet()) {
            if ("UNITTEST".equals(type)) {
                continue;
            }
            sb.append(String.format("\t%s%n", type));
        }
        return sb.toString();
    }

    //~ Methods **************************************************************************************************************************************

    /**
     * Invokes the domain specific collector.
     *
     * @throws  OrchestraException  If an error occurs.
     */
    public void invoke() {
        try {
            final DomainReader reader = _getDomainReader(type);

            LOGGER.info("Invoking reader: " + reader.getDatasource());

            final BlockingQueue<Metric> metricQueue = new LinkedBlockingQueue<>();
            final BlockingQueue<Annotation> annotationQueue = new LinkedBlockingQueue<>();
            long timeout = System.currentTimeMillis() + timeoutMillis;
            Thread invoker = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        reader.invokeCollection(metricQueue, annotationQueue);
                    }
                }, "collectclient-invoker-" + ID.getAndIncrement());

            invoker.start();

            List<Metric> metricChunk = new ArrayList<>(CHUNK_SIZE);
            List<Annotation> annotationChunk = new ArrayList<>(CHUNK_SIZE);

            while (System.currentTimeMillis() < timeout) {
                boolean metricCollectionDone = metricQueue.isEmpty() && reader.isMetricCollectionDone();
                boolean annotationCollectionDone = annotationQueue.isEmpty() && reader.isAnnotationCollectionDone();

                if (Thread.currentThread().isInterrupted() || (metricCollectionDone && annotationCollectionDone)) {
                    break;
                }
                metricQueue.drainTo(metricChunk, CHUNK_SIZE);
                annotationQueue.drainTo(annotationChunk, CHUNK_SIZE);
                if (!metricChunk.isEmpty()) {
                    try {
                        service.put(metricChunk);
                        LOGGER.debug("metric chunk sent to service");
                    } finally {
                        metricChunk.clear();
                    }
                }
                if (!annotationChunk.isEmpty()) {
                    try {
                        service.putAnnotations(annotationChunk);
                        LOGGER.debug("annotation chunk sent to service");
                    } finally {
                        annotationChunk.clear();
                    }
                }
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException ex) {
                    LOGGER.info("Execution was interrupted.");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (invoker.isAlive()) {
                invoker.interrupt();
                invoker.join(TIMEOUT_INTERVAL_MS);
            }
        } catch (InterruptedException ex) {
            LOGGER.info("Execution was interrupted.");
        } catch (RuntimeException ex) {
            throw new OrchestraException("There was a problem invoking the reader.", ex);
        } finally {
            LOGGER.info("Finished");
            if (service != null) {
                service.dispose();
            }
        } // end try-catch-finally
    }

    private DomainReader _getDomainReader(String type) {
        assert (type != null) : "Reader type cannot be null.";
        try {
            if (READERS.containsKey(type)) {
                return READERS.get(type).newInstance();
            } else {
                throw new IllegalArgumentException("Unsupported reader type: " + type);
            }
        } catch (InstantiationException | IllegalAccessException | RuntimeException ex) {
            throw new OrchestraException(ex);
        }
    }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
