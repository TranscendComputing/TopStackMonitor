package com.msi.tough.monitor.connector;

import java.util.List;

import com.msi.tough.monitor.common.model.VirtualMachineInstance;
import com.msi.tough.monitor.common.model.exception.MSIMonitorException;
import com.msi.tough.monitor.connector.collect.MonitorGatherer;

public interface HypervisorConnector {
	public boolean connect();

	/*
	 * get the Connection
	 */
	public Object getConnection() throws MSIMonitorException;

	/*
	 * Get the Domain Id for the specified domain (represented as a
	 * VirtualMachineInstance object)
	 */
	public String getDomainId(VirtualMachineInstance domain)
			throws MSIMonitorException;

	/*
	 * Get a Platform specific Metric Gatherer used to collect monitoring
	 * information with.
	 */
	public MonitorGatherer getGatherer();

	/*
	 * Lists all guest domain instances for the current hypervisor connection.
	 */
	public List<VirtualMachineInstance> getGuestDomains()
			throws MSIMonitorException;

	public boolean isConnected();

	public boolean logout();

	/*
	 * Set the Platform specific Metric Gatherer.
	 */
	public void setGatherer(MonitorGatherer gatherer);
}
