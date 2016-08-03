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

import com.salesforce.dva.orchestra.OrchestraException;
import com.salesforce.dva.orchestra.argus.entity.Annotation;
import com.salesforce.dva.orchestra.argus.entity.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.Queue;

/**
 * Abstract base class for domain reader implementations. This class provides a default implementation for reading configuration from a properties
 * file whose location is specified by the the system property named '<tt>&lt;full_qualified_class_name&gt;.configuration</tt>'.
 *
 * @author  Tom Valine (tvaline@salesforce.com)
 */
public abstract class AbstractDomainReader implements DomainReader {

    //~ Static fields/initializers *******************************************************************************************************************

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDomainReader.class);

    //~ Instance fields ******************************************************************************************************************************

    protected final Properties configuration;

    //~ Constructors *********************************************************************************************************************************

    /** Creates a new AbstractDomainReader object. */
    public AbstractDomainReader() {
        configuration = new Properties(getDefaultConfig());
        if (isConfigurationRequired()) {
            String keyPrefix = getClass().getName();

            loadConfiguration(keyPrefix + ".configuration");
            try {
                loadConfiguration(getClass().getName() + ".override.configuration");
            } catch (OrchestraException e) {
                // It is fine to not find an override, just ignore this.
            }

            Properties props = System.getProperties();
            String key, val;

            for (Object tmp : props.keySet()) {
                key = tmp.toString();
                val = props.getProperty(key);
                if (key.startsWith(keyPrefix)) {
                    key = key.substring(keyPrefix.length() + 1);
                    if (!(key.equals("configuration") || key.equals("override.configuration"))) {
                        LOGGER.info("Adding override key={} value={}", key, (key.equals("password") ? "***" : val));
                        configuration.setProperty(key, val);
                    }
                }
            }
        }
    }

    //~ Methods **************************************************************************************************************************************

    private void loadConfiguration(String keyName) {
        InputStream is = null;

        try {
            is = new FileInputStream(System.getProperty(keyName));
            configuration.load(is);
        } catch (Exception ex) {
            String msg = "Could not load configuration.  Please specify the configuration file location using -D{0}=<path>.";

            throw new OrchestraException(MessageFormat.format(msg, keyName), ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    assert (false) : "This should never happen";
                }
            }
        }
    }

    /**
     * Default implementation of the default configuration getter.
     *
     * @return  An empty properties collection.
     */
    protected Properties getDefaultConfig() {
        return new Properties();
    }

    /**
     * Used to determine if configuration should be read from a property file. Sub-class implementations may override this method to indicate that
     * configuration loading should be performed.
     *
     * @return  false
     */
    protected boolean isConfigurationRequired() {
        return false;
    }

    @Override
    public boolean isMetricCollectionDone() {
        return true;
    }

    @Override
    public boolean isAnnotationCollectionDone() {
        return true;
    }

    @Override
    public void invokeCollection(Queue<Metric> metricQueue, Queue<Annotation> annotationQueue) { }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
