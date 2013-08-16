/**
 * Transcend Computing, Inc.
 * Confidential and Proprietary
 * Copyright (c) Transcend Computing, Inc. 2012
 * All Rights Reserved.
 */
package com.msi.tough.monitor.connector.receive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.msi.tough.model.monitor.DimensionBean;
import com.msi.tough.model.monitor.MeasureBean;
import com.msi.tough.monitor.common.model.helper.DimensionHelper;
import com.msi.tough.utils.Constants;

/**
 * @author jgardner
 *
 */
public abstract class AbstractAgentMessage {

    protected String host;
    protected String plugin;
    protected String plugin_instance;
    String type;
    String type_instance;
    String[] dstypes;
    String[] dsnames;
    Object[] values;
    Double time;
    Double interval;

    private boolean complete;

    private final String namespace = "MSI/EC2";

    private DimensionHelper dimensionHelper = null;

    /**
     *
     */
    public AbstractAgentMessage() {
        super();
    }

    /**
     * Set up message for the given time window.
     * @param time
     * @param interval
     */
    public void initMessage(String host, double time, double interval) {
        this.host = host;
        this.time = time;
        this.interval = interval;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public Object[] getValues() {
        return values;
    }

    public void setValues(Object[] values) {
        this.values = values;
    }

    public Double getDoubleValue(int index) {
        if (values[index] == null) {
            return 0d;
        }
        if (values[index] instanceof Double) {
            return (Double) values[index];
        }
        if (values[index] instanceof Integer) {
            return 0d + (Integer) values[index];
        }
        if (values[index] instanceof Long) {
            return 0d + (Long) values[index];
        }
        return 0d;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public String getPluginInstance() {
        return plugin_instance;
    }

    public void setPluginInstance(String plugin_instance) {
        this.plugin_instance = plugin_instance;
    }

    public String getTypeInstance() {
        return type_instance;
    }

    public void setTypeInstance(String type_instance) {
        this.type_instance = type_instance;
    }

    public String[] getDstypes() {
        return dstypes;
    }

    public void setDstypes(String[] dstypes) {
        this.dstypes = dstypes;
    }

    public String[] getDsnames() {
        return dsnames;
    }

    public void setDsnames(String[] dsnames) {
        this.dsnames = dsnames;
    }

    public void addDsnames(String[] dsnames) {
        if (this.dsnames == null) {
            this.dsnames = dsnames;
        } else {
            ArrayList<String> dsnamesList =
                    new ArrayList<String>(Arrays.asList(this.dsnames));
            dsnamesList.addAll(Arrays.asList(this.dsnames));
            this.dsnames = dsnamesList.toArray(new String[] {});
        }
    }

    public double getInterval() {
        return interval;
    }

    public void setInterval(double interval) {
        this.interval = interval;
    }

    /**
     * @return true if the message is complete (ready to generate measures)
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * @param complete value for completeness
     */
    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public List<MeasureBean> toMeasure() {
        List<MeasureBean> measures = new ArrayList<MeasureBean>();
        while (hasMoreMeasures()) {
            MeasureBean measure = new MeasureBean();
            fillMeasure(measure);
            measures.add(measure);
        }
        return measures;
    }

    /**
     * Should return true as long as more measures can be created from this
     * message.
     * @return true if more measures can be generated.
     */
    public abstract boolean hasMoreMeasures();

    protected void fillMeasure(MeasureBean measure) {
        final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
        dimensions.add(dimensionHelper.getDimensionBean(Constants.INSTANCEID, getHost(), true));
        measure.setDimensions(dimensions);
        measure.setNamespace(namespace);
        // CollectD time is in seconds, x1000 for millis since epoch.
        measure.setTimestamp(new Date(1000 * time.longValue()));
    }

    public DimensionHelper getDimensionHelper() {
        return dimensionHelper;
    }

    public void setDimensionHelper(DimensionHelper dimensionHelper) {
        this.dimensionHelper = dimensionHelper;
    }
}