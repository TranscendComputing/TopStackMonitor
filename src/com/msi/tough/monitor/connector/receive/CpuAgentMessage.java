/**
 * Transcend Computing, Inc.
 * Confidential and Proprietary
 * Copyright (c) Transcend Computing, Inc. 2012
 * All Rights Reserved.
 */
package com.msi.tough.monitor.connector.receive;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.msi.tough.core.Appctx;
import com.msi.tough.model.monitor.MeasureBean;
import com.msi.tough.monitor.common.MonitorConstants;

/**
 * CPU Utilization message from an agent.
 *
 * @author jgardner
 *
 */
public class CpuAgentMessage extends AgentMessage {
    private static final Logger logger = Appctx
            .getLogger(CpuAgentMessage.class.getName());

    private Map<String, Integer> cpuValues = new HashMap<String, Integer>();

    private boolean consumed = false;

    private CpuAgentMessage priorMessage;

    /**
     *
     */
    public CpuAgentMessage() {
    }

    /**
     * Construct message, with prior message of same type.
     * @param priorMessage
     */
    public CpuAgentMessage(CpuAgentMessage priorMessage) {
        this.priorMessage = priorMessage;
    }

    /**
     * Set up message for the given time window.
     * @param host
     * @param time
     * @param interval
     */
    public void initMessage(String host, double time, double interval) {
        super.initMessage(host, time, interval);
        // If prior message is incomplete, and within .1 of a second,
        // combine with this counter.
        if (priorMessage != null && !priorMessage.isComplete() &&
                this.time+0.05 > priorMessage.getTime() &&
                this.time-0.05 < priorMessage.getTime() &&
                this.host.equals(priorMessage.getHost())) {
            // This message should be aggregated with prior, they are for the
            // same timestamp (i.e., different cpu states, "idle", "user").
            cpuValues = priorMessage.cpuValues;
            for (String key: cpuValues.keySet()) {
                if (cpuValues.get(key) == null) {
                    cpuValues.remove(key);
                }
            }
            consumed = priorMessage.consumed;
            priorMessage = null;
        }
    }

    public void setCpuType(String typeInstance, Object[] values) {
        Integer value = getDoubleValue(0).intValue();
        cpuValues.put(typeInstance, value);
    }

    @Override
    public boolean isComplete() {
        // We might have missed messages; can't calculate CPU usage without
        // at least the primary cpu usage types.
        // Excluding these; may not always be present,
        // small contributers to utilization
        // "softirq", "steal"
        if (cpuValues.containsKey("idle") &&
                cpuValues.containsKey("user") &&
                cpuValues.containsKey("wait") &&
                cpuValues.containsKey("interrupt") &&
                cpuValues.containsKey("system")) {
            setComplete(true);
            return true;
        }
        return false;
    }

    @Override
    public void setValues(Object[] values) {
        super.setValues(values);
        setCpuType(getTypeInstance(), values);
    }

    @Override
    protected void fillMeasure(MeasureBean measure) {
        super.fillMeasure(measure);
        measure.setName(MonitorConstants.CPU_UTILIZATION_COMMAND);
        measure.setUnit(MonitorConstants.PERCENT_UNIT);

        double cpuTotal = 0;
        double cpuIdle = 0;
        StringBuffer sb = new StringBuffer("{");
        for (Map.Entry<String, Integer> cpuStateCounter : cpuValues.entrySet()) {
            String state = cpuStateCounter.getKey();
            int counter = cpuStateCounter.getValue();
            int lastCounter = 0;
            sb.append(state + ":" + (counter - lastCounter) + ",");
            cpuTotal += counter - lastCounter;
            if (state.equals("idle")) {
                cpuIdle += counter - lastCounter;
            }
        }
        sb.append("}");
        double cpuIdlePercent = cpuIdle / cpuTotal;
        double value = 100 * (1 - cpuIdlePercent);
        BigDecimal rounded;
        try {
            rounded = new BigDecimal(Double.toString(value));
        } catch (NumberFormatException nfe) {
            logger.warn("Failed to parse " + value + " from " + getHost());
            rounded = new BigDecimal(0);
        }
        rounded = rounded.setScale(2, BigDecimal.ROUND_HALF_UP);
        measure.setValue(rounded.doubleValue());
        if (logger.isDebugEnabled()) {
            logger.debug(getHost()+"CPU: " + measure.getValue().intValue() + "%: " + sb);
        }
        consumed = true;
    }

    /* (non-Javadoc)
     * @see com.msi.tough.monitor.connector.receive.AbstractAgentMessage#hasMoreMeasures()
     */
    @Override
    public boolean hasMoreMeasures() {
        if (consumed) {
            return false;
        }
        return true;
    }


}
