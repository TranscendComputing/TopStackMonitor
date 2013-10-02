/**
 * Transcend Computing, Inc.
 * Confidential and Proprietary
 * Copyright (c) Transcend Computing, Inc. 2012
 * All Rights Reserved.
 */
package com.msi.tough.monitor.connector.receive;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.msi.tough.core.Appctx;
import com.msi.tough.model.monitor.MeasureBean;
import com.msi.tough.monitor.common.manager.MeasureHandler;
import com.msi.tough.monitor.common.model.exception.MSIMonitorException;
import com.msi.tough.utils.InstanceUtil;
import com.transcend.compute.client.util.RunningInstanceUtil;

/**
 * Message driven bean to receive statistics from an agent.
 *
 * @author jgardner
 *
 */
@Component
public class CollectDAgentReceiver implements AgentReceiver {

    private static final Logger logger = Appctx
            .getLogger(CollectDAgentReceiver.class.getName());

    @Autowired
    private MeasureHandler measureHandler = null;

    @Autowired
    private SessionFactory sessionFactory = null;

    @Autowired
    private AgentMessageFactory factory = null;

    @Autowired
    private RunningInstanceUtil runningInstanceUtil = null;

    private int messagesReceived = 0;
    private int measuresCreated = 0;
    private Map<Class<? extends AgentMessage>, Integer> countByType =
            new HashMap<Class<? extends AgentMessage>, Integer>();

    /**
     *
     */
    public CollectDAgentReceiver() {
    }


    @Transactional
    public void onAgentMessage(List<Map<String, Object>> messages) throws Exception {
        Session session = sessionFactory.getCurrentSession();
        try {
            for (Map<String, Object> value_map : messages) {
                if (logger.isDebugEnabled()) {
                    for (Entry<String, Object> e : value_map.entrySet()) {
                        logger.debug("E:" + e.getKey() + " = " + e.getValue() + " "
                                + e.getValue().getClass());
                    }
                }
                AgentMessage agentMessage = null;
                try {
                    agentMessage = hydrateMessage(value_map);
                } catch (Exception e) {
                    logger.error("Unable to parse monitor message.", e);
                }

                if (agentMessage != null) {
                    persistMessage(session, agentMessage);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to consume message.", e);
            //throw e;
        } finally {
        }
    }


    /**
     * Convert message from map of primitives to object type.
     *
     * @param value_map
     * @return
     */
    private AgentMessage hydrateMessage(Map<String, Object> value_map) {
        String host = value_map.get("host").toString();
        Session session = sessionFactory.getCurrentSession();
        host = runningInstanceUtil.normalizeInstanceId(session, host, true);
        factory.host = host;

        factory.plugin = value_map.get("plugin").toString();
        factory.plugin_instance = value_map.get("plugin_instance").toString();
        factory.type = value_map.get("type").toString();
        factory.type_instance = value_map.get("type_instance").toString();
        factory.time = (Double) value_map.get("time");
        factory.interval = (Double) value_map.get("interval");
        @SuppressWarnings("unchecked")
        List<Integer> values = (List<Integer>) value_map.get("values");
        factory.values = values.toArray(new Object[] {});
        @SuppressWarnings("unchecked")
        List<String> dstypes = (List<String>) value_map.get("dstypes");
        factory.dstypes = dstypes.toArray(new String[] {});
        @SuppressWarnings("unchecked")
        List<String> dsnames = (List<String>) value_map.get("dsnames");
        factory.dsnames = dsnames.toArray(new String[] {});
        return factory.makeMessage();
    }


    private Collection<MeasureBean> persistMessage(Session session,
            AgentMessage message) {
        List<MeasureBean> measures = null;
        if (message.isComplete()) {
            try {
                measures = message.toMeasure();
                measureHandler.storeAll(session, measures);
                for (MeasureBean m: measures) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("M: "+m.getName()+" "+m.getValue());
                    }
                }
                onMessageSave(message, measures);
            } catch (MSIMonitorException mse) {
                logger.error("Unable to save measure", mse);
            }
        } else {
            logger.debug("Not persisting message: " + message.getPlugin() +":"+
                    message.getTypeInstance() + message.getTime());
        }
        return measures;
    }

    private void onMessageSave(AgentMessage message, List<MeasureBean> measures) {
        messagesReceived += 1;
        measuresCreated += measures.size();
        Integer count = countByType.get(message.getClass());
        if (count == null) {
            count = 0;
        }
        countByType.put(message.getClass(), count+1);
        if (messagesReceived % 5 == 0) {
            logger.info("Received " + messagesReceived +
                    " agent messages for monitoring.");
            logger.info("Created " + measuresCreated +
                    " metric values.");
            for (Class<?> key: countByType.keySet() ) {
                logger.info("Saved " + countByType.get(key) +
                        " messages of type " + key.getSimpleName());
            }
        }
    }

    @Override
    public int getReceiveCount() {
        return messagesReceived;
    }
}
