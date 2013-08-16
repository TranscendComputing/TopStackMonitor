package com.msi.ec2.monitor.connector.collect.platform;

import java.util.Map;

import javax.annotation.Resource;

import static junit.framework.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionConfiguration;

import com.msi.tough.model.monitor.HypervisorConfigBean;
import com.msi.tough.monitor.common.model.exception.MSIMonitorException;
import com.msi.tough.monitor.connector.HypervisorConnectionFactory;
import com.msi.tough.monitor.connector.HypervisorConnector;

@Ignore("Seems to leak connections, eventually crashes the Xen gatherer.")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/test-monitor-context.xml" })
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = false)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
public class XenGetDomainXMLTest {

    @Autowired
    private String testXenHypervisor = null;

    @Resource(name="connectionOptions")
    private Map<String, String> connectionOptions = null;

	@Test
	public void testDumpXML() {

	    HypervisorConfigBean conf = new HypervisorConfigBean();
        conf.setHost(testXenHypervisor);
        conf.setUsername("root");
        conf.setPassword("doesnt-matter-use-key");
        conf.setProto("ssh");
        conf.setType("qemu");
        conf.setOptions(connectionOptions);
        HypervisorConnector connector = HypervisorConnectionFactory.getConnector(conf);

        Connect conn = null;
		try {
	        conn = (Connect) connector.getConnection();
			//conn = new Connect("xen+ssh://root@msicloud2.momentumsoftware.com/", true);
		} catch (MSIMonitorException e) {
			System.out.println("exception while connecting: " + e.getMessage());
			System.out.println(e);
		}
		try {
			Domain testDomain = conn.domainLookupByName("i-5596093C");
			System.out.println("Domain: " +testDomain.getName() + ":" + testDomain.getID() + ":" + testDomain.getOSType());
			System.out.println("Attempting the big one.");
			System.out.println("Capabilities: " + conn.getCapabilities());
			System.out.println("XMLDescription: " + testDomain.getXMLDesc(0));
		} catch (LibvirtException e) {
			System.out.println("exception caught: " + e.getMessage());
			System.out.println(e.getError());
		}
		assertTrue(connector.logout());

	}


}
