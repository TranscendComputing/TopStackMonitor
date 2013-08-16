package com.msi.tough.monitor;

/**
 * This class represents a single hypervisors monitor (one hypervisor per
 * thread).
 * 
 * @author heathm
 */

import java.util.List;

import org.slf4j.Logger;

import com.msi.tough.core.Appctx;
import com.msi.tough.model.monitor.HypervisorConfigBean;
import com.msi.tough.monitor.common.manager.MeasureHandler;
import com.msi.tough.monitor.common.model.VirtualMachineInstance;
import com.msi.tough.monitor.common.model.exception.MSIMonitorException;
import com.msi.tough.monitor.connector.HypervisorConnectionFactory;
import com.msi.tough.monitor.connector.HypervisorConnector;

public class HypervisorMonitor implements Runnable {
	private static final Logger logger = Appctx
			.getLogger(HypervisorMonitor.class.getName());

	private Thread runner;
	private HypervisorConnector conn;
	private final MeasureHandler mHandler;
	private final long gather_sleep;

	public HypervisorMonitor(final MeasureHandler mHandler,
			final HypervisorConnector conn, final long sleep)
			throws MSIMonitorException {
		runner = new Thread(this);
		this.conn = conn;
		this.mHandler = mHandler;
		gather_sleep = sleep;
		conn.getConnection();
		runner.start();
	}

	public void configConn(final HypervisorConfigBean config) {
		final HypervisorConnector conn = HypervisorConnectionFactory
				.getConnector(config);
		this.conn = conn;
		try {
			conn.getConnection();
		} catch (final MSIMonitorException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			logger.error("A general MSIMonitorException occurred at startup - Monitor may not work correctly!");
		}
	}

	@Override
	public void run() {
		while (runner != null) {
			try {
				logger.debug("Collecting data");
				final List<VirtualMachineInstance> instances = conn
						.getGuestDomains();
				final Monitor mon = new Monitor(conn, instances);
				mon.setMeasureHandler(mHandler);
				Thread.sleep(gather_sleep);
			} catch (final InterruptedException ie) {
				runner = null;
			} catch (final MSIMonitorException me) {
				me.printStackTrace();
			}
		}
	}

	/**
	 * Terminates this hypervisor monitor (interrupts the thread).
	 */
	public void terminate() {
		runner.interrupt();
	}
}