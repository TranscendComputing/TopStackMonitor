package com.msi.tough.monitor.connector.collect.platform;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockStats;
import org.libvirt.DomainInterfaceStats;
import org.libvirt.LibvirtException;
import org.libvirt.VcpuInfo;
import org.slf4j.Logger;

import com.msi.tough.core.Appctx;
import com.msi.tough.monitor.common.model.exception.MSIMonitorException;
import com.msi.tough.monitor.connector.HypervisorConnector;
import com.msi.tough.monitor.connector.collect.MonitorGatherer;

/**
 * The basic Libvirt Gatherer for Monitoring information. options that can be
 * passed to the constructor to override functionality: 'driveLabel' => 'sd',
 * 'netlabel' => 'vif' Note: both of these are used to discover drives if they
 * cannot be detected by libvirt.
 *
 * @author heathm
 */
public class XenGatherer implements MonitorGatherer {
	private static final Logger logger = Appctx.getLogger(XenGatherer.class
			.getName());

	public final static long CPU_DIV_EST = 1000000000L;
	// millis to perform a measure
	private static long cpu_sleep;

	static {
		cpu_sleep = Long.parseLong((String) Appctx
				.getConfigurationBean("CPUSampleTime"));
	}

	/**
	 * Get the Domain object from its domainId (either name or id)
	 *
	 * @param xConn
	 * @param domainId
	 * @return
	 * @throws LibvirtException
	 */
	public static Domain getDomain(final Connect xConn, final String domainId)
			throws LibvirtException {
		int id = -1;
		try {
			id = Integer.parseInt(domainId);
		} catch (final NumberFormatException nfe) {
		}
		return id == -1 ? xConn.domainLookupByName(domainId) : xConn
				.domainLookupByID(id);
	}

	//
	// public static void setCPUSleep(final long slp) {
	// cpu_sleep = slp;
	// }

	private final Map<String, String> options = new HashMap<String, String>();

	public XenGatherer(final Map<String, String> options) {
		this.options.putAll(options);
	}

	/**
	 * Get the CPU Utilization for a domain. This is a rolled up estimated
	 * percentage based on a time sample and the following formula: 100 x
	 * (cputime_now - cputime_then) / (then x numcpus x 10^9) This is the same
	 * formula used by virttop to calculate this metric.
	 *
	 * @param conn
	 * @param domainId
	 * @return Estimated percentage of CPU Utilized.
	 * @throws MSIMonitorException
	 */
	@Override
	public double getCPUUtilization(final HypervisorConnector conn,
			final String domainId) throws MSIMonitorException {
		final Connect xConn = (Connect) conn.getConnection();
		try {
			final Domain dom = getDomain(xConn, domainId);
			// long beginTime = DateUtil.getCurrentUnixTimeStamp(new Date());
			final long beginTime = Calendar.getInstance(
					TimeZone.getTimeZone("GMT")).getTimeInMillis();
			final VcpuInfo[] cpuinfo1 = dom.getVcpusInfo();
			try {
				Thread.sleep(cpu_sleep);
			} catch (final InterruptedException ie) {
			}
			double cpu1Tally = 0;
			double cpu2Tally = 0;
			final long diffTime = Calendar.getInstance(
					TimeZone.getTimeZone("GMT")).getTimeInMillis()
					- beginTime;
			final VcpuInfo[] cpuinfo2 = dom.getVcpusInfo();

			for (final VcpuInfo element : cpuinfo1) {
				cpu1Tally += element.cpuTime;
			}
			for (final VcpuInfo element : cpuinfo2) {
				cpu2Tally += element.cpuTime;
			}

			final double diff_interval = diffTime;

			// calculate cpu percentage based on our sampling.
			logger.debug("cpu2Tally precalculated [" + cpu2Tally
					+ "] cpu1Tally pre calculated [" + cpu1Tally + "]");
			final double numerator = 100 * java.lang.Math.abs(cpu2Tally
					- cpu1Tally);
			logger.debug("numerator is [" + numerator + "] cpu length is ["
					+ cpuinfo1.length + "]");
			// double denominator = diff_interval * (xConn.nodeInfo().nodes *
			// xConn.nodeInfo().sockets * xConn.nodeInfo().cores *
			// xConn.nodeInfo().threads) * CPU_DIV_EST;
			final double denominator = diff_interval / 1000 * dom.getMaxVcpus()
					* CPU_DIV_EST;
			logger.debug("denominator [" + denominator + "]");
			final double retVal = numerator / denominator;

			return retVal;
		} catch (final LibvirtException le) {
			throw new MSIMonitorException("", le);
		}
	}

	/**
	 * Get the bytes read by the drive specified.
	 *
	 * @param conn
	 * @param domainId
	 * @param diskId
	 * @return bytes
	 * @throws MSIMonitorException
	 */
	@Override
	public long getDiskReadBytes(final HypervisorConnector conn,
			final String domainId, final String diskId)
			throws MSIMonitorException {
		final Connect xConn = (Connect) conn.getConnection();
		try {
			final Domain dom = getDomain(xConn, domainId);
			final DomainBlockStats bStats = dom.blockStats(diskId);
			if (bStats != null) {
				return bStats.rd_bytes;
			}
		} catch (final LibvirtException le) {
			throw new MSIMonitorException("", le);
		}
		return 0;
	}

	/**
	 * Get the requested number of read operations from the drive specified.
	 *
	 * @param conn
	 * @param domainId
	 * @param diskId
	 * @return number of operations
	 * @throws MSIMonitorException
	 */
	@Override
	public long getDiskReadOps(final HypervisorConnector conn,
			final String domainId, final String diskId)
			throws MSIMonitorException {
		final Connect xConn = (Connect) conn.getConnection();
		try {
			final Domain dom = getDomain(xConn, domainId);
			final DomainBlockStats bStats = dom.blockStats(diskId);
			if (bStats != null) {
				return bStats.rd_req;
			}
		} catch (final LibvirtException le) {
			throw new MSIMonitorException("", le);
		}
		return 0;
	}

	/**
	 * Get the bytes written to the drive specified.
	 *
	 * @param conn
	 * @param domainId
	 * @param diskId
	 * @return bytes
	 * @throws MSIMonitorException
	 */
	@Override
	public long getDiskWriteBytes(final HypervisorConnector conn,
			final String domainId, final String diskId)
			throws MSIMonitorException {
		final Connect xConn = (Connect) conn.getConnection();
		try {
			final Domain dom = getDomain(xConn, domainId);
			final DomainBlockStats bStats = dom.blockStats(diskId);
			if (bStats != null) {
				return bStats.wr_bytes;
			}
		} catch (final LibvirtException le) {
			throw new MSIMonitorException(le.getMessage(), le);
		}
		return 0;
	}

	/**
	 * Get the requested number of write operations from the drive specified.
	 *
	 * @param conn
	 * @param domainId
	 * @param diskId
	 * @return number of operations
	 * @throws MSIMonitorException
	 */
	@Override
	public long getDiskWriteOps(final HypervisorConnector conn,
			final String domainId, final String diskId)
			throws MSIMonitorException {
		final Connect xConn = (Connect) conn.getConnection();
		try {
			final Domain dom = getDomain(xConn, domainId);

			final DomainBlockStats bStats = dom.blockStats(diskId);
			if (bStats != null) {
				return bStats.wr_req;
			}
		} catch (final LibvirtException le) {
			throw new MSIMonitorException("", le);
		}
		return 0;
	}

	/**
	 * Retrieve the disk Ids for the given domain.
	 *
	 * @param conn
	 * @param domainId
	 * @return A List of the domains named drive Ids
	 * @throws MSIMonitorException
	 */
	@Override
	public List<String> getDriveIds(final HypervisorConnector conn,
			final String domainId) throws MSIMonitorException {
		final List<String> drives = new ArrayList<String>();
		final Connect xConn = (Connect) conn.getConnection();
		int dId = -1;
		try {
			dId = Integer.parseInt(domainId);
		} catch (final NumberFormatException nfe) {
		}
		final Pattern pat = Pattern.compile(".*dev='(\\S*)'.*"); // default
		try {
			final Domain dom = getDomain(xConn, domainId);
			if (dId < 0) {
				dId = dom.getID();
			}
			final String xmlDesc = dom.getXMLDesc(0);
			final Matcher match = pat.matcher(xmlDesc);
			while (match.find()) {
				final String found = match.group(1);
				if (found.startsWith("xvd") || found.startsWith("sd")
						|| found.startsWith("hd") || found.startsWith("vd")) {
					drives.add(new String(found));
				}
			}
		} catch (final LibvirtException le) {
			logger.warn("Unable to acquire Drive Ids for domain id ["
					+ domainId + "] : " + le.getMessage());
			logger.info("Removing any disk operation gathering for ["
					+ domainId + "] from monitoring.");
			return drives;
		}
		return drives;
	}

	/**
	 * Retrieve a list of the domains network interface Ids.
	 *
	 * @param conn
	 * @param domainId
	 * @return List of network interface ids.
	 * @throws MSIMonitorException
	 */
	@Override
	public List<String> getNetworkIds(final HypervisorConnector conn,
			final String domainId) throws MSIMonitorException {
		final List<String> eths = new ArrayList<String>();
		final Connect xConn = (Connect) conn.getConnection();
		int dId = -1;
		try {
			dId = Integer.parseInt(domainId);
		} catch (final NumberFormatException nfe) {
		}
		// Pattern pat = Pattern.compile(".*dev='(vif" + domainId +
		// "\\.\\d+)'.*");
		final Pattern pat = Pattern.compile(".*dev='((vif|vnet)\\S*)'.*");
		try {
			final Domain dom = getDomain(xConn, domainId);
			if (dId < 0) {
				dId = dom.getID();
			}
			final String xmlDesc = dom.getXMLDesc(0);
			final Matcher match = pat.matcher(xmlDesc);
			while (match.find()) {
				eths.add(new String(match.group(1)));
			}
		} catch (final LibvirtException le) {
			logger.warn("Unable to acquire Network Ids for domain id ["
					+ domainId + "] : " + le.getMessage());
			logger.info("Removing any network metrics gathering for ["
					+ domainId + "] from monitoring.");
			return eths;
		}
		return eths;
	}

	/**
	 * Get the number of bytes read from the specified network interface.
	 *
	 * @param conn
	 * @param domainId
	 * @param ifaceId
	 * @return bytes
	 * @throws MSIMonitorException
	 */
	@Override
	public long getNetworkIn(final HypervisorConnector conn,
			final String domainId, final String ifaceId)
			throws MSIMonitorException {
		final Connect xConn = (Connect) conn.getConnection();
		try {
			final Domain dom = getDomain(xConn, domainId);
			final DomainInterfaceStats iStats = dom.interfaceStats(ifaceId);
			if (iStats != null) {
				return iStats.rx_packets;
			}
		} catch (final LibvirtException le) {
			throw new MSIMonitorException("", le);
		}
		return 0;
	}

	/**
	 * Get the number of bytes written to the specified network interface.
	 *
	 * @param conn
	 * @param domainId
	 * @param ifaceId
	 * @return bytes
	 * @throws MSIMonitorException
	 */
	@Override
	public long getNetworkOut(final HypervisorConnector conn,
			final String domainId, final String ifaceId)
			throws MSIMonitorException {
		final Connect xConn = (Connect) conn.getConnection();
		try {
			final Domain dom = getDomain(xConn, domainId);
			final DomainInterfaceStats iStats = dom.interfaceStats(ifaceId);
			if (iStats != null) {
				return iStats.tx_packets;
			}
		} catch (final LibvirtException le) {
			throw new MSIMonitorException("", le);
		}
		return 0;
	}

}
