/**
 * Transcend Computing, Inc.
 * Confidential and Proprietary
 * Copyright (c) Transcend Computing, Inc. 2012
 * All Rights Reserved.
 */
package com.msi.tough.monitor.connector.receive;

import java.util.List;
import java.util.Map;

/**
 * Generic interface for receiving monitoring metrics from a virtual machine agents.
 *
 * @author jgardner
 */
public interface AgentReceiver {

    /**
     * Count of messages received.
     *
     * @return count
     */
    public int getReceiveCount();

    public void onAgentMessage(List<Map<String, Object>> messages)
            throws Exception;

}
