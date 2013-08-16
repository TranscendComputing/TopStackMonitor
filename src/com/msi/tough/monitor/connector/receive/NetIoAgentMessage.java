/**
 * Transcend Computing, Inc.
 * Confidential and Proprietary
 * Copyright (c) Transcend Computing, Inc. 2012
 * All Rights Reserved.
 */
package com.msi.tough.monitor.connector.receive;

import org.slf4j.Logger;

import com.msi.tough.core.Appctx;
import com.msi.tough.model.monitor.MeasureBean;
import com.msi.tough.monitor.common.MonitorConstants;

/**
 * Network IO message from an agent.
 *
 * @author jgardner
 *
 */
public class NetIoAgentMessage extends AgentMessage {
    private static final Logger logger = Appctx
            .getLogger(NetIoAgentMessage.class.getName());

    boolean consumed = false;

    /**
     *
     */
    public NetIoAgentMessage() {
    }

    @Override
    public boolean isComplete() {
        return true;
    }


    @Override
    public void setValues(Object[] values) {
        super.setValues(values);
    }

    @Override
    protected void fillMeasure(MeasureBean measure) {
        super.fillMeasure(measure);
        for (int i = 0; i < dsnames.length; i++) {
            if (dsnames[i].equals("rx") && "if_octets".equals(type)) {
                measure.setName(MonitorConstants.NETWORK_IN_COMMAND);
                measure.setUnit(MonitorConstants.BYTE_UNIT);
                measure.setValue(getDoubleValue(i));
                dsnames[i] = "";
                break;
            } else if (dsnames[i].equals("tx") && "if_octets".equals(type)) {
                measure.setName(MonitorConstants.NETWORK_OUT_COMMAND);
                measure.setUnit(MonitorConstants.BYTE_UNIT);
                measure.setValue(getDoubleValue(i));
                dsnames[i] = "";
                break;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Network: " + measure.getName() + "=" + measure.getValue().intValue());
        }
    }

    /* (non-Javadoc)
     * @see com.msi.tough.monitor.connector.receive.AbstractAgentMessage#hasMoreMeasures()
     */
    @Override
    public boolean hasMoreMeasures() {
        if (! "if_octets".equals(type) ) {
            // Not a handled type.
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
