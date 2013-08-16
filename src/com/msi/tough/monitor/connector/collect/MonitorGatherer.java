/**
 * 
 */
package com.msi.tough.monitor.connector.collect;

import java.util.List;

import com.msi.tough.monitor.common.model.exception.MSIMonitorException;
import com.msi.tough.monitor.connector.HypervisorConnector;

/**
 * The generic interface for gathering monitoring metrics from a hypervisor.
 * 
 * @author heathm
 */
public interface MonitorGatherer {
	/*
	 * This method should gathers CPU Utilization percentages from the
	 * hypervisor about the guest domain instance
	 */
	public abstract double getCPUUtilization(HypervisorConnector conn,
			String domainId) throws MSIMonitorException;

	/*
	 * Gather Reads from the disk in bytes from the hypervisor about the guest
	 * domain instance
	 */
	public abstract long getDiskReadBytes(HypervisorConnector conn,
			String domainId, String diskId) throws MSIMonitorException;

	/*
	 * Gather disk read request operations for the specified disk id from the
	 * hypervisor about the guest domain instance
	 */
	public abstract long getDiskReadOps(HypervisorConnector conn,
			String domainId, String diskId) throws MSIMonitorException;

	/*
	 * Gather Writes to the disk in bytes from the hypervisor about the guest
	 * domain instance
	 */
	public abstract long getDiskWriteBytes(HypervisorConnector conn,
			String domainId, String diskId) throws MSIMonitorException;

	/*
	 * Gather disk write request operations to the specified disk id from the
	 * hypervisor about the guest domain instance
	 */
	public abstract long getDiskWriteOps(HypervisorConnector conn,
			String domainId, String diskId) throws MSIMonitorException;

	/*
	 * Grab the Drive identifiers for each drive on the target guest domain.
	 */
	public abstract List<String> getDriveIds(HypervisorConnector conn,
			String domainId) throws MSIMonitorException;

	/*
	 * Grab the network identifiers for each network device on the target guest
	 * domain.
	 */
	public abstract List<String> getNetworkIds(HypervisorConnector conn,
			String domainId) throws MSIMonitorException;

	/*
	 * Gather inbound network bytes read from the specified interface from the
	 * hypervisor about the guest domain instance
	 */
	public abstract long getNetworkIn(HypervisorConnector conn,
			String domainId, String ifaceId) throws MSIMonitorException;

	/*
	 * Gather outbound writes to the specified interface from the hypervisor
	 * about the guest domain instance
	 */
	public abstract long getNetworkOut(HypervisorConnector conn,
			String domainId, String ifaceId) throws MSIMonitorException;;
}
