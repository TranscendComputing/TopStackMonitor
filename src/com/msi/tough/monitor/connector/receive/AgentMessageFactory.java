/**
 * Transcend Computing, Inc.
 * Confidential and Proprietary
 * Copyright (c) Transcend Computing, Inc. 2012
 * All Rights Reserved.
 */
package com.msi.tough.monitor.connector.receive;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;

import com.msi.tough.core.Appctx;
import com.msi.tough.monitor.common.model.helper.DimensionHelper;

/**
 * Utility to generate agent messages correctly from raw data.
 *
 * Some agent messages really need to be combined together to be useful, so
 * this merges messages into a common ancestor.
 *
 * @author jgardner
 *
 */
public class AgentMessageFactory extends AbstractAgentMessage {

    private static final Logger logger = Appctx
            .getLogger(CollectDAgentReceiver.class.getName());


    /**
     * Maintain generated messages
     */
    private Map<String, AgentMessage> existingMessages;

    private double lastTimeSeen = 0;

    /**
     *
     */
    public AgentMessageFactory() {
        existingMessages = new HashMap<String, AgentMessage>();
    }

    @Resource
    public void setDimensionHelper(DimensionHelper dimensionHelper) {
        super.setDimensionHelper(dimensionHelper);
    }

    /**
     * Done with a batch of messages, reset to be ready for next.
     */
    public void reset() {
    }

    public String makeMessageKey() {
        return getHost() + ":" + getPlugin();
    }

    public AgentMessage makeMessage() {
        if (lastTimeSeen != this.time) {
            reset();
            lastTimeSeen = this.time;
        }
        AgentMessage message = null;
        if ("cpu".equals(getPlugin())) {
            CpuAgentMessage cpuMessage = (CpuAgentMessage)
                    existingMessages.get(makeMessageKey());
            cpuMessage = new CpuAgentMessage(cpuMessage);
            message = cpuMessage;
        } else if ("disk".equals(getPlugin())) {
            message = new DiskAgentMessage();
        } else if ("interface".equals(getPlugin())) {
            message = new NetIoAgentMessage();
        } else {
            logger.debug("Cannot make message for type "+getPlugin());
            return null;
        }
        copyCommon(message);
        message.setDimensionHelper(getDimensionHelper());
        existingMessages.put(makeMessageKey(), message);
        clearMessage();
        return message;
    }

    private void copyCommon(AgentMessage message) {
        message.initMessage(this.host, this.time, this.interval);
        message.plugin = this.plugin;
        message.plugin_instance = this.plugin_instance;
        message.type = this.type;
        message.type_instance = this.type_instance;
        message.dsnames = this.dsnames;
        message.dstypes = this.dstypes;
        message.setValues(this.values);
    }

    private void clearMessage() {
        this.host = null;
        this.plugin = null;
        this.plugin_instance = null;
        this.type = null;
        this.type_instance = null;
        this.time = 0d;
        this.interval = 0d;
        this.dsnames = null;
        this.dstypes = null;
    }

    /* (non-Javadoc)
     * @see com.msi.tough.monitor.connector.receive.AbstractAgentMessage#hasMoreMeasures()
     */
    @Override
    public boolean hasMoreMeasures() {
        // Factory doesn't actually make measures directly.
        return false;
    }
}
