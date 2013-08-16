/**
 *
 */
package com.msi.tough.monitor.connector.collect.platform;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.msi.tough.core.Appctx;
import com.msi.tough.monitor.common.model.exception.MSIMonitorException;
import com.msi.tough.monitor.connector.HypervisorConnector;
import com.msi.tough.monitor.connector.collect.MonitorGatherer;
import com.vmware.apputils.AppUtil;
import com.vmware.vim.ManagedObjectReference;
import com.vmware.vim.PerfCounterInfo;
import com.vmware.vim.PerfEntityMetric;
import com.vmware.vim.PerfEntityMetricBase;
import com.vmware.vim.PerfMetricId;
import com.vmware.vim.PerfMetricIntSeries;
import com.vmware.vim.PerfMetricSeries;
import com.vmware.vim.PerfQuerySpec;
import com.vmware.vim.PerfSampleInfo;
import com.vmware.vim.RuntimeFault;

/**
 * VMWare gatherer implementation. Gathers metrics for monitoring from the
 * vcenter SOAP Service. options that can be passed to the constructor to
 * override functionality: 'driveLabel' => 'disk', 'netlabel' => 'net'
 *
 * @author heathm
 */
public class VmwareGatherer implements MonitorGatherer {

	private static final Logger logger = Appctx.getLogger(VmwareGatherer.class
			.getName());
	private static final int CPU_INDEX = 1;
	private static final int NET_IN_INDEX = 5;
	private static final int NET_OUT_INDEX = 6;
	private static final int DISK_WOPS_INDEX = 6;
	private static final int DISK_ROPS_INDEX = 5;
	private static final int DISK_WBYTES_INDEX = 8;
	private static final int DISK_RBYTES_INDEX = 7;

	private static final int INTERVAL = 20;
	private final Map<String, String> options = new HashMap<String, String>();

	public VmwareGatherer(final Map<String, String> options) {
		this.options.putAll(options);
	}

	private long[] gatherMetric(final ManagedObjectReference pmRef,
			final ManagedObjectReference vmRef,
			final ArrayList<PerfMetricId> mMetrics,
			final Map<Integer, PerfCounterInfo> counters, final AppUtil xConn)
			throws RuntimeFault, RemoteException {
		final PerfMetricId[] metricIds = mMetrics.toArray(new PerfMetricId[0]);
		final PerfQuerySpec qSpec = new PerfQuerySpec();
		qSpec.setEntity(vmRef);
		qSpec.setMaxSample(new Integer(1));
		qSpec.setMetricId(metricIds);
		qSpec.setIntervalId(INTERVAL);

		final PerfQuerySpec[] qSpecs = new PerfQuerySpec[] { qSpec };
		final PerfEntityMetricBase[] pValues = xConn.getConnection()
				.getService().queryPerf(pmRef, qSpecs);
		long[] returnVals = {};

		if (pValues != null) {
			for (int i = 0; i < pValues.length; ++i) {
				final PerfMetricSeries[] vals = ((PerfEntityMetric) pValues[i])
						.getValue();
				@SuppressWarnings("unused")
                final PerfSampleInfo[] infos = ((PerfEntityMetric) pValues[i])
						.getSampleInfo();
				for (int vi = 0; vi < vals.length; ++vi) {
					@SuppressWarnings("unused")
                    final PerfCounterInfo pci = counters.get(new Integer(
							vals[vi].getId().getCounterId()));
					if (vals[vi] instanceof PerfMetricIntSeries) {
						final PerfMetricIntSeries val = (PerfMetricIntSeries) vals[vi];
						returnVals = val.getValue();
					}
				}
			}
		}

		if (returnVals == null) {
			//returnVals[0] = 0;
		}
		return returnVals;

	}

	@Override
	public double getCPUUtilization(final HypervisorConnector conn,
			final String domainId) throws MSIMonitorException {
		final long[] polledCPUs = pollForMetric(conn, domainId, "cpu",
				CPU_INDEX);
		long perc = 0;
		for (final long polledCPU : polledCPUs) {
			perc += polledCPU;
		}
		if (polledCPUs.length > 0) {
			perc /= polledCPUs.length;
		}
		return perc * 0.01;
	}

	@Override
	public long getDiskReadBytes(final HypervisorConnector conn,
			final String domainId, final String diskId)
			throws MSIMonitorException {
		final long[] polledReadBytes = pollForMetric(conn, domainId, "disk",
				DISK_RBYTES_INDEX);
		long rValue = 0;
		for (final long polledReadByte : polledReadBytes) {
			rValue += polledReadByte;
		}
		return rValue;
	}

	@Override
	public long getDiskReadOps(final HypervisorConnector conn,
			final String domainId, final String diskId)
			throws MSIMonitorException {
		final long[] polledReadOps = pollForMetric(conn, domainId, "disk",
				DISK_ROPS_INDEX);
		long rValue = 0;
		for (final long polledReadOp : polledReadOps) {
			rValue += polledReadOp;
		}
		return rValue;
	}

	@Override
	public long getDiskWriteBytes(final HypervisorConnector conn,
			final String domainId, final String diskId)
			throws MSIMonitorException {
		final long[] polledWriteBytes = pollForMetric(conn, domainId, "disk",
				DISK_WBYTES_INDEX);
		long rValue = 0;
		for (final long polledWriteByte : polledWriteBytes) {
			rValue += polledWriteByte;
		}
		return rValue;
	}

	@Override
	public long getDiskWriteOps(final HypervisorConnector conn,
			final String domainId, final String diskId)
			throws MSIMonitorException {
		final long[] polledWriteOps = pollForMetric(conn, domainId, "disk",
				DISK_WOPS_INDEX);
		long rValue = 0;
		for (final long polledWriteOp : polledWriteOps) {
			rValue += polledWriteOp;
		}
		return rValue;
	}

	@Override
	public List<String> getDriveIds(final HypervisorConnector conn,
			final String domainId) throws MSIMonitorException {
		String propertySet = options.get("diskLabel");
		propertySet = propertySet == null || propertySet.isEmpty() ? "disk"
				: propertySet;
		final List<String> drives = new ArrayList<String>();
		drives.add(propertySet);
		return drives;
	}

	@Override
	public List<String> getNetworkIds(final HypervisorConnector conn,
			final String domainId) throws MSIMonitorException {
		String propertySet = options.get("netLabel");
		propertySet = propertySet == null || propertySet.isEmpty() ? "net"
				: propertySet;
		final List<String> nets = new ArrayList<String>();
		nets.add(propertySet);
		return nets;
	}

	@Override
	public long getNetworkIn(final HypervisorConnector conn,
			final String domainId, final String ifaceId)
			throws MSIMonitorException {
		final long[] polledNetIn = pollForMetric(conn, domainId, "net",
				NET_IN_INDEX);
		long rValue = 0;
		for (final long element : polledNetIn) {
			rValue += element;
		}
		return rValue;
	}

	@Override
	public long getNetworkOut(final HypervisorConnector conn,
			final String domainId, final String ifaceId)
			throws MSIMonitorException {
		final long[] polledNetOut = pollForMetric(conn, domainId, "net",
				NET_OUT_INDEX);
		long rValue = 0;
		for (final long element : polledNetOut) {
			rValue += element;
		}
		return rValue;
	}

	/**
	 * @param cInfo
	 * @return
	 */
	private List<PerfCounterInfo> getPerformanceCounters(
			final PerfCounterInfo[] cInfo, final String type) {

		final List<PerfCounterInfo> vmCpuCounters = new ArrayList<PerfCounterInfo>();
		for (int i = 0; i < cInfo.length; ++i) {
			if (type.equalsIgnoreCase(cInfo[i].getGroupInfo().getKey())) {
				vmCpuCounters.add(cInfo[i]);
			}
		}
		return vmCpuCounters;
	}

	public long[] pollForMetric(final HypervisorConnector conn,
			final String domainId, final String metricName,
			final int metricIndex) throws MSIMonitorException {

		final AppUtil xConn = (AppUtil) conn.getConnection();
		try {
			final ManagedObjectReference vmmor = xConn.getServiceUtil()
					.getDecendentMoRef(null, "VirtualMachine", domainId);
			if (vmmor == null) {
				logger.warn("Virtual Machine " + xConn.get_option("vmname")
						+ " not found");
				final long[] x = { 0 };
				return x;
			}

			final Map<Integer, PerfCounterInfo> counters = new HashMap<Integer, PerfCounterInfo>();
			final ManagedObjectReference pmRef = xConn.getConnection()
					.getServiceContent().getPerfManager();
			final PerfCounterInfo[] cInfo = (PerfCounterInfo[]) xConn
					.getServiceUtil().getDynamicProperty(pmRef, "perfCounter");

			final List<PerfCounterInfo> vmCpuCounters = getPerformanceCounters(
					cInfo, metricName);
			final PerfCounterInfo pcInfo = vmCpuCounters.get(metricIndex);

			counters.put(new Integer(pcInfo.getKey()), pcInfo);
			final PerfMetricId[] aMetrics = xConn
					.getConnection()
					.getService()
					.queryAvailablePerfMetric(pmRef, vmmor, null, null,
							INTERVAL);
			final ArrayList<PerfMetricId> mMetrics = new ArrayList<PerfMetricId>();
			if (aMetrics != null) {
				for (int index = 0; index < aMetrics.length; ++index) {
					if (counters.containsKey(new Integer(aMetrics[index]
							.getCounterId()))) {
						mMetrics.add(aMetrics[index]);
					}
				}
			}
			return gatherMetric(pmRef, vmmor, mMetrics, counters, xConn);

		} catch (final Exception e) {
			throw new MSIMonitorException(e.getMessage(), e);
		}
	}

}
