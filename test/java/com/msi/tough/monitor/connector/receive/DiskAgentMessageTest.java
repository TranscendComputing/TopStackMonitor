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
public class DiskAgentMessageTest {

    private static final Logger logger = Appctx
            .getLogger(DiskAgentMessageTest.class.getName());

    private static final Map<Double[], Double[]> expected = new HashMap<Double[], Double[]>();
    static {
        expected.put(new Double[]{0d, 0d}, new Double[]{0d, 0d});
        expected.put(new Double[]{0.1d, 0d}, new Double[]{10d, 0d});
        expected.put(new Double[]{371.66d, 32.8666d}, new Double[]{37166d, 3287d});
        expected.put(new Double[]{0.30905704340659d, 0.128199106292094}, new Double[]{31d, 13d});
    }

    private static String host = "host1";
    private static Double time = 0d;
    private static Double interval = 0d;
    private static String plugin = "disk";
    private static String plugin_instance = "vda";
    private static String[] dsnames = new String[] {"read", "write"};
    private static String[] dstypes = new String[] {"derive", "derive"};

    @Resource
    private AgentMessageFactory agentMessageFactory = null;

	@Test
	@Transactional
	public void testMeasureComputation() throws Exception {
	    for (Double[] values: expected.keySet()) {
	        DiskAgentMessage message = null;
	        message = new DiskAgentMessage();
            message.setDimensionHelper(agentMessageFactory.getDimensionHelper());
	        message.initMessage(host, time, interval);
	        message.plugin = plugin;
	        message.plugin_instance = plugin_instance;
	        message.type = "disk_ops";
	        message.type_instance = "";
	        message.dsnames = dsnames.clone();
	        message.dstypes = dstypes;
	        message.setValues(values);
	        if (message.isComplete()) {
	            List<MeasureBean> measures = message.toMeasure();
	            assertEquals("Unexpected value.", expected.get(values)[0],
	                    measures.get(0).getValue());
	            logger.debug("Got value:" + measures.get(0).getValue());
                assertEquals("Unexpected value.", expected.get(values)[1],
                        measures.get(1).getValue());
                logger.debug("Got value:" + measures.get(1).getValue());
	        }

	    }
	}
}
