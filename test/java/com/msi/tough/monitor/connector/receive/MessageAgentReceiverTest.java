package com.msi.tough.monitor.connector.receive;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.msi.tough.core.Appctx;

//@Ignore("This test consumes real message, need a source of messages.")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/test-monitor-context.xml" })
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = false)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
public class MessageAgentReceiverTest {

    private static final Logger logger = Appctx
            .getLogger(MessageAgentReceiverTest.class.getName());

    @Resource(name="collectDAgentReceiver")
    AgentReceiver receiver;

	@Test
	@Ignore
	public void testMessageConsumption() throws Exception {
	    int startCount = receiver.getReceiveCount();
	    boolean received = false;
	    for (int i = 0; i < 60; i++) {
	        if (receiver.getReceiveCount() > startCount) {
	            received = true;
	            break;
	        }
	        logger.debug("Waiting for message: " + i + " sec.");
	        Thread.sleep(1000);
	    }
	    if (! received) {
	        fail("No messages received after 1 minute.");
	    }
	}

	@Test
	@Transactional
    public void testMessageCreation() throws Exception {
        List<Map<String, Object>> messages = new ArrayList<Map<String,Object>>();
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("host", "i-00000b72");
        message.put("plugin", "interface");
        message.put("plugin_instance", "");
        message.put("type", "if_octets");
        message.put("type_instance", "");
        message.put("time", new Double(new Date().getTime()/1000));
        message.put("interval", new Double(0));
        message.put("values", Arrays.asList(new Integer[]{1}));
        message.put("dstypes", Arrays.asList(new String[]{""}));
        message.put("dsnames", Arrays.asList(new String[]{"rx"}));

        messages.add(message);
        try {
            receiver.onAgentMessage(messages);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
