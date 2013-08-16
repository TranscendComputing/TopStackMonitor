/**
 * Transcend Computing, Inc.
 * Confidential and Proprietary
 * Copyright (c) Transcend Computing, Inc. 2012
 * All Rights Reserved.
 */
package com.msi.tough.monitor.connector.receive;

import java.math.BigDecimal;

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
public class DiskAgentMessage extends AgentMessage {
    private static final Logger logger = Appctx
            .getLogger(DiskAgentMessage.class.getName());

    boolean consumed = false;

    /**
     *
     */
    public DiskAgentMessage() {
    }

    @Override
    public boolean isComplete() {
        return true;
    }


    @Override
    public void setValues(Object[] values) {
        super.setValues(values);
    }

    private Double scale(Double value) {
        value = value * 100;
        BigDecimal rounded = new BigDecimal(Double.toString(value));
        rounded = rounded.setScale(0, BigDecimal.ROUND_HALF_UP);
        return rounded.doubleValue();
    }

    @Override
    protected void fillMeasure(MeasureBean measure) {
        super.fillMeasure(measure);
        for (int i = 0; i < dsnames.length; i++) {
            if (dsnames[i].equals("read") && "disk_octets".equals(type)) {
                measure.setName(MonitorConstants.DISK_READ_BYTES_COMMAND);
                measure.setUnit(MonitorConstants.BYTE_UNIT);
                measure.setValue(getDoubleValue(i));
                dsnames[i] = "";
                break;
            } else if (dsnames[i].equals("write") && "disk_octets".equals(type)) {
                measure.setName(MonitorConstants.DISK_WRITE_BYTES_COMMAND);
                measure.setUnit(MonitorConstants.BYTE_UNIT);
                measure.setValue(getDoubleValue(i));
                dsnames[i] = "";
                break;
            } else if (dsnames[i].equals("read") && "disk_ops".equals(type)) {
                measure.setName(MonitorConstants.DISK_READ_OPS_COMMAND);
                measure.setUnit(MonitorConstants.COUNT_UNIT);
                measure.setValue(scale(getDoubleValue(i)));
                dsnames[i] = "";
                break;
            } else if (dsnames[i].equals("write") && "disk_ops".equals(type)) {
                measure.setName(MonitorConstants.DISK_WRITE_OPS_COMMAND);
                measure.setUnit(MonitorConstants.COUNT_UNIT);
                measure.setValue(scale(getDoubleValue(i)));
                dsnames[i] = "";
                break;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Disk: " + measure.getName() + "=" + measure.getValue().intValue());
        }
    }

    /* (non-Javadoc)
     * @see com.msi.tough.monitor.connector.receive.AbstractAgentMessage#hasMoreMeasures()
     */
    @Override
    public boolean hasMoreMeasures() {
        if (! "disk_ops".equals(type) && !"disk_octets".equals(type) ) {
            // Not a handled type.
            return false;
        }
        // Exclude partitions; we're looking for a full disk
        if (plugin_instance != null &&
            Character.isDigit(plugin_instance.charAt(plugin_instance.length()-1)) ) {
            return false;
        }
        for (String dsname: dsnames) {
            if (! "".equals(dsname)) {
                return true;
            }
        }
        return false;
    }

}
