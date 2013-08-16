package com.msi.ec2.monitor.connector;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockStats;
import org.libvirt.DomainInfo;
import org.libvirt.DomainInterfaceStats;
import org.libvirt.LibvirtException;
import org.libvirt.VcpuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionConfiguration;

import com.msi.tough.model.monitor.HypervisorConfigBean;
import com.msi.tough.monitor.connector.HypervisorConnectionFactory;
import com.msi.tough.monitor.connector.HypervisorConnector;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/test-monitor-context.xml" })
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = false)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
public class HypervisorConnectionFactoryTest {

    @Autowired
    private String testHypervisor = null;

    @Resource(name="connectionOptions")
    private Map<String, String> connectionOptions = null;//= new HashMap<String, String>();

	@Test
	public void testCapabilities() throws Exception {
		HypervisorConfigBean conf = new HypervisorConfigBean();
		conf.setHost(testHypervisor);
		conf.setUsername("root");
		conf.setPassword("doesnt-matter-use-key");
		conf.setProto("ssh");
		conf.setType("qemu");
		conf.setOptions(connectionOptions);

		HypervisorConnector conn = HypervisorConnectionFactory.getConnector(conf);
		assertTrue(conn != null);
		assertTrue(conn.connect());
		Connect hConn = (Connect) conn.getConnection();
		System.out.println("*******************************");
		System.out.println("HOST INFO");
		System.out.println("*******************************");
		System.out.println("capabilities: " + hConn.getCapabilities());
		System.out.println("host: " + hConn.getHostName());
		System.out.println("Type: " + hConn.getType());
		System.out.println("URI: " + hConn.getURI());
		System.out.println("Version: " + hConn.getVersion());
		System.out.println("Memory: " + hConn.getFreeMemory());
		System.out.println("*******************************");
		System.out.println("Domain INFO");
		System.out.println("*******************************");

		String[] namedDomains = hConn.listDefinedDomains();
		System.out.println("**** LIST DEFINED DOMAINS ****");
		if (namedDomains != null) {
			for (String dom : namedDomains) {
				System.out.println("\t[" + dom + "]");
			}
		}
		System.out.println("******************************");
		int[] idDoms = hConn.listDomains();
		for (int i = 0; i < idDoms.length; i++) {
			System.out.println("idDoms[" + i + "] : " + idDoms[i]);
			Domain inst = hConn.domainLookupByID(idDoms[i]);
			DomainInfo inf = inst.getInfo();
			System.out.println("***** Domain " + inst.getName() + " *****");
			System.out.println("CPU Time: " + inf.cpuTime);
			VcpuInfo[] cpuInfos = inst.getVcpusInfo();
			for (int j = 0; j < cpuInfos.length; j++) {
				System.out.println("vcpusInfo [" + j + "] : cpu ["
						+ cpuInfos[j].cpu + "] cpuTime [" + cpuInfos[j].cpuTime
						+ "] number [" + cpuInfos[j].number + "]");
			}
			// System.out.println("Current Snapshot: " +
			// inst.snapshotCurrent());
			System.out.println("OS : " + inst.getOSType());
			System.out.println("Max Memory: (from DomainInfo) " + inf.maxMem
					+ " (from Domain) " + inst.getMaxMemory());
			System.out.println("Memory: (from DomainInfo) " + inf.memory);

			// Unknown procedure: 194 on older Xend we have. This is due to out
			// of date Xend... upgrade at some point!
			// DomainBlockInfo bInfo = inst.blockInfo("");

			String nickName = "vif" + idDoms[i] + ".0";
			DomainInterfaceStats iStats = null;
			try {
			    iStats = inst.interfaceStats(nickName);
			} catch (LibvirtException lve) {
                System.out.println("Couldn't read " + nickName + " stats: " + lve.getMessage());
			}
			if (iStats == null) {
				System.out.println("OOPS!  iStats is null");
			} else {
				System.out.println("Network IN: " + iStats.rx_bytes);
				System.out.println("Network OUT: " + iStats.tx_bytes);
			}
			if (i > 0) {
			    try {
			        DomainBlockStats bStats = inst.blockStats("xvda");
			        System.out.println("DiskReadBytes : " + bStats.rd_bytes);
			        System.out.println("DiskReadOps : " + bStats.rd_req);
			        System.out.println("DiskWriteBytes : " + bStats.wr_bytes);
			        System.out.println("DiskWriteOps : " + bStats.wr_req);
			    } catch (LibvirtException lve) {
                    System.out.println("Couldn't get block stats for xvda:" + lve.getMessage());
			    }
			}

			System.out.println("nrVirtCpu?: " + inf.nrVirtCpu);
			System.out.println("State: " + inf.state.toString());
			try {
			    System.out.println("XML : " + inst.getXMLDesc(idDoms[i]));
			} catch (LibvirtException lve) {
                System.out.println("XML : can't read XML: " + lve.getMessage());
			}
		}
		System.out.println("*******************************");
	}

	// public void testMetric() throws LibvirtException, MSIMonitorException {
	// HashMap<String, String> opts = new HashMap<String, String>();
	// HypervisorConnector conn = HypervisorConnetionFactory.getConnector("xen",
	// "ssh", "root", "", "192.168.1.210", opts);
	// assertTrue(conn != null);
	// assertTrue(conn.connect());
	//
	// // get a Metric for CPUUtilization
	//
	//
	// conn.logout();
	// }

	@Ignore
	@Test
	public void testGetConnector() {
		HashMap<String, String> opts = new HashMap<String, String>();
		HypervisorConfigBean conf = new HypervisorConfigBean();
		conf.setHost("msicloud4.momentumsoftware.com");
		conf.setOptions(opts);
		conf.setPassword("");
		conf.setProto("ssh");
		conf.setType("qemu");
		conf.setUsername("root");

		HypervisorConnector conn = HypervisorConnectionFactory.getConnector(conf);
		assertTrue(conn != null);
		assertTrue(conn.connect());
		conn.logout();
	}

}
