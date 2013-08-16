package com.msi.ec2.monitor.manager;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionConfiguration;

import com.msi.tough.model.monitor.HypervisorConfigBean;
import com.msi.tough.monitor.Monitor;
import com.msi.tough.monitor.common.model.VirtualMachineInstance;
import com.msi.tough.monitor.common.model.exception.MSIMonitorException;
import com.msi.tough.monitor.connector.HypervisorConnectionFactory;
import com.msi.tough.monitor.connector.HypervisorConnector;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/test-monitor-context.xml" })
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = false)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
public class MeasureGathererTest
{
    public MeasureGathererTest() {

    }
    @Autowired
    private String testXenHypervisor = null;

    @Resource(name="connectionOptions")
    private Map<String, String> connectionOptions = null;

    @Test
    public void testGather() throws MSIMonitorException
    {
        // HypervisorConnector conn =
        // HypervisorConnetionFactory.getConnector("xen", "ssh", "root", "",
        // "192.168.1.3", opts);
        HypervisorConfigBean conf = new HypervisorConfigBean();
        conf.setHost(testXenHypervisor);
        conf.setUsername("root");
        conf.setPassword("");
        conf.setProto("ssh");
        conf.setType("xen");
        conf.setOptions(connectionOptions);
        HypervisorConnector conn =
            HypervisorConnectionFactory.getConnector(conf);
        assertTrue(conn != null);
        assertTrue(conn.connect());
        List<VirtualMachineInstance> instances =
            new ArrayList<VirtualMachineInstance>();
        VirtualMachineInstance efrontend = new VirtualMachineInstance();
        VirtualMachineInstance ccaustin = new VirtualMachineInstance();
        efrontend.setId("10");
        efrontend.setName("EFrontEnd");
        ccaustin.setId("8");
        ccaustin.setName("CCAustin");
        instances.add(efrontend);
        instances.add(ccaustin);
        @SuppressWarnings("unused")
        Monitor mon = new Monitor(conn, instances); // monitor object per
                                                    // physical box. Can always

        // add more monitors.
        /*
        mon.setMeasureHandler(new MeasureHandler()
        {
            @Override
            public void store(Object session, MeasureBean measure)
                    throws MSIMonitorException {
                System.out.println(measure);
            }

            @Override
            public void storeAll(Object session, List<MeasureBean> measures)
                    throws MSIMonitorException {
                System.out.println(measures);
            }
        });
        mon.gather(); // monitor
        */
    }
}
