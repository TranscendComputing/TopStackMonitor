package com.msi.tough.monitor.connector.receive;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

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
import com.msi.tough.model.monitor.MeasureBean;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/test-monitor-context.xml" })
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = true)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
public class CpuAgentMessageTest {

    private static final Logger logger = Appctx
            .getLogger(CpuAgentMessageTest.class.getName());

    private static final Map<Integer[], Double> expected = new HashMap<Integer[], Double>();
    static {
        // Expected values are a number of jiffies; idle is first in this set.
        // the utilization is the percent represented by non-idle jiffies.
        expected.put(new Integer[]{5,5,5,5,0}, 75d);
        expected.put(new Integer[]{90,5,5,0,0}, 10d);
    }

    private static String[] cpuTypes = {"idle", "system", "user", "wait", "interrupt"};

    private static String host = "host1";
    private static Double time = 0d;
    private static Double interval = 0d;
    private static String plugin = "cpu";
    private static String plugin_instance = "";
    private static String type = "";
    private static String[] dsnames = new String[] {"irrelevant"};
    private static String[] dstypes = new String[] {"irrelevant"};

    @Resource
    private AgentMessageFactory agentMessageFactory = null;

	@Test
	@Transactional
	public void testMeasureComputation() throws Exception {
	    for (Integer[] values: expected.keySet()) {
	        CpuAgentMessage message = null;
	        for (int i = 0; i < cpuTypes.length; i++) {
	            message = new CpuAgentMessage(message);
	            message.setDimensionHelper(agentMessageFactory.getDimensionHelper());
	            message.initMessage(host, time, interval);
	            message.plugin = plugin;
	            message.plugin_instance = plugin_instance;
	            message.type = type;
	            message.type_instance = cpuTypes[i];
	            message.dsnames = dsnames;
	            message.dstypes = dstypes;
	            message.setValues(new Integer[] {values[i]});
	            if (message.isComplete()) {
	                List<MeasureBean> measures = message.toMeasure();
	                assertEquals("Unexpected value.", expected.get(values),
	                        measures.get(0).getValue());
	                logger.debug("Got value:" + measures.get(0).getValue());
	            }
	        }

	    }
	}
}
