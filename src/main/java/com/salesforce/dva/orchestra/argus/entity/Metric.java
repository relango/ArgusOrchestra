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
package com.salesforce.dva.orchestra.argus.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static com.salesforce.dva.orchestra.util.Assert.requireArgument;

/**
 * Time series metric entity object. This entity encapsulates all the information needed to represent a time series for a metric within a single
 * scope. The following tag names are reserved. Any methods that set tags, which use these reserved tag names, will throw a runtime exception.
 *
 * <ul>
 *   <li>metric</li>
 *   <li>displayName</li>
 *   <li>units</li>
 * </ul>
 *
 * @author  Tom Valine (tvaline@salesforce.com), Bhinav Sura (bhinav.sura@salesforce.com)
 */
@SuppressWarnings("serial")
@JsonInclude(Include.NON_NULL)
public class Metric extends TSDBEntity implements Serializable {

    //~ Instance fields ******************************************************************************************************************************

    private String _displayName;
    private String _units;
    private final Map<Long, String> _datapoints;

    //~ Constructors *********************************************************************************************************************************

    /**
     * Creates a new Metric object.
     *
     * @param  scope   The reverse dotted name of the collection scope. Cannot be null or empty.
     * @param  metric  The name of the metric. Cannot be null or empty.
     */
    public Metric(String scope, String metric) {
        this();
        setScope(scope);
        setMetric(metric);
    }

    /** Creates a new Metric object. */
    protected Metric() {
        super(null, null);
        _datapoints = new TreeMap<>();
    }

    //~ Methods **************************************************************************************************************************************

    @Override
    protected final void setScope(String scope) {
        requireArgument(scope != null && !scope.trim().isEmpty(), "Scope cannot be null or empty.");
        super.setScope(scope);
    }

    @Override
    protected final void setMetric(String metric) {
        requireArgument(metric != null && !metric.trim().isEmpty(), "Metric cannot be null or empty.");
        super.setMetric(metric);
    }

    /**
     * Returns an unmodifiable map of time series data points which is backed by the entity objects internal data.
     *
     * @return  The map of time series data points. Will never be null, but may be empty.
     */
    public Map<Long, String> getDatapoints() {
        return Collections.unmodifiableMap(_datapoints);
    }

    /**
     * Deletes the current set of data points and replaces them with a new set.
     *
     * @param  datapoints  The new set of data points. If null or empty, only the deletion of the current set of data points is performed.
     */
    public void setDatapoints(Map<Long, String> datapoints) {
        _datapoints.clear();
        if (datapoints != null) {
            _datapoints.putAll(datapoints);
        }
    }

    /**
     * Sets the display name for the metric.
     *
     * @param  displayName  The display name for the metric. Can be null or empty.
     */
    public void setDisplayName(String displayName) {
        _displayName = displayName;
    }

    /**
     * Returns the display name for the metric.
     *
     * @return  The display name for the metric. Can be null or empty.
     */
    public String getDisplayName() {
        return _displayName;
    }

    /**
     * Sets the units of the time series data point values.
     *
     * @param  units  The units of the time series data point values. Can be null or empty.
     */
    public void setUnits(String units) {
        _units = units;
    }

    /**
     * Returns the units of the time series data point values.
     *
     * @return  The units of the time series data point values. Can be null or empty.
     */
    public String getUnits() {
        return _units;
    }

    @Override
    public String toString() {
        Object[] params = { getScope(), getMetric(), getTags(), getDatapoints() };
        String format = "scope=>{0}, metric=>{1}, tags=>{2}, datapoints=>{3}";

        return MessageFormat.format(format, params);
    }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
