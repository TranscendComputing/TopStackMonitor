package com.msi.tough.monitor.actions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;

import com.msi.tough.core.Appctx;
import com.msi.tough.model.monitor.DimensionBean;
import com.msi.tough.model.monitor.HypervisorConfigBean;
import com.msi.tough.model.monitor.MeasureBean;
import com.msi.tough.monitor.common.MonitorConstants;
import com.msi.tough.monitor.common.manager.MeasureHandler;
import com.msi.tough.monitor.common.model.VirtualMachineInstance;
import com.msi.tough.monitor.common.model.exception.MSIMonitorException;
import com.msi.tough.monitor.connector.HypervisorConnectionFactory;
import com.msi.tough.monitor.connector.HypervisorConnector;
import com.msi.tough.monitor.connector.collect.MonitorGatherer;
import com.msi.tough.query.UnsecuredAction;
import com.msi.tough.utils.CWUtil;
import com.msi.tough.utils.Constants;

public class GatherRDBMS extends UnsecuredAction {
	private static Logger logger = Appctx
			.getLogger(GatherRDBMS.class.getName());

	private final String namespace = "MSI/EC2";

	private String convertId(final String in) {
		final int i = in.indexOf('-');
		if (i == -1) {
			return in;
		}
		return "i-" + in.substring(i + 1);
	}

	/**
	 * Gathers AWS style measures aggregates them based on the measureCollector.
	 * and stores them via the measureHandler.store() mechanism defined.
	 *
	 * @param checkAlarm
	 *
	 * @throws MSIMonitorException
	 */
	@SuppressWarnings("unchecked")
	private void gather(final Session session) throws MSIMonitorException {
		final MeasureHandler mHandler = Appctx.getBean("measurehandler");
		final Query query = session
				.createQuery("from HypervisorConfigBean where enable='Y'");
		final List<HypervisorConfigBean> hvlist = query.list();
		for (final HypervisorConfigBean config : hvlist) {
			final HypervisorConnector conn = HypervisorConnectionFactory
					.getConnector(config);
			Integer acid = config.getAccountId();
			if (acid == null) {
			    acid = 0; // Avoid NPE, null implies no ID
			}
			logger.debug("Collecting " + config.getHost());
			try {
				if (!conn.isConnected()) {
					conn.connect();
				}
				final MonitorGatherer gatherer = conn.getGatherer();
				final List<VirtualMachineInstance> instances = conn
						.getGuestDomains();

				if (instances == null || instances.size() == 0) {
					continue;
				}

				final List<MeasureBean> measures = new ArrayList<MeasureBean>();

				// Gather metrics
				for (final VirtualMachineInstance vm : instances) {
					// check if instance in our db
					// final String vmid = convertId(vm.getName());
					// final String vmuid = vm.getUuid().toString();
					// logger.debug("id " + vmid + " uuid " + vmuid);
					// if (InstanceUtil.getInstance(session, 0, vmid) == null) {
					// final InstanceBean ib = InstanceUtil.createNewInstance(
					// session, 0L, vmid, null, null, null, vmuid);
					// session.save(ib);
					// logger.debug("adding instance id " + vmid + " uuid "
					// + vmuid);
					// }

					logger.debug("Collecting for instance " + vm.getId());
					getCPUMeasurements(session, acid, conn, gatherer, measures,
							vm);
					vm.setDriveIds(gatherer.getDriveIds(conn, vm.getName()));
					getDriveMeasurements(session, acid, conn, gatherer,
							measures, vm);
					vm.setNetworkIds(gatherer.getNetworkIds(conn, vm.getName()));
					getNetworkMeasurements(session, acid, conn, gatherer,
							measures, vm);
				}
				// handle Measures
				for (final MeasureBean measure : measures) {
					mHandler.store(session, measure);
				}
			} catch (final Exception e) {
				e.printStackTrace();
			} finally {
				conn.logout();
			}
		}
	}

	/**
	 * Grab CPUUtilization information from the virtualMachineInstance.
	 *
	 * @param conn
	 *
	 * @param gatherer
	 * @param measures
	 * @param vm
	 * @param acid
	 * @param dims
	 * @throws Exception
	 */
	private void getCPUMeasurements(final Session session, final long acid,
			final HypervisorConnector conn, final MonitorGatherer gatherer,
			final List<MeasureBean> measures, final VirtualMachineInstance vm)
			throws Exception {
		final double cpu = gatherer.getCPUUtilization(conn, vm.getName());
		final MeasureBean b = new MeasureBean();
		final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
		dimensions.add(CWUtil.getDimensionBean(session, acid,
				Constants.INSTANCEID, convertId(vm.getName()), true));
		b.setDimensions(dimensions);
		b.setName(MonitorConstants.CPU_UTILIZATION_COMMAND);
		b.setNamespace(namespace);
		b.setTimestamp(new Date());
		b.setUnit(MonitorConstants.PERCENT_UNIT);
		b.setValue(cpu);
		measures.add(b);
	}

	/**
	 * Grab Disk I/O information from the VirtualMachineInstance.
	 *
	 * @param conn
	 *
	 * @param gatherer
	 * @param measures
	 * @param vm
	 * @param dims
	 * @throws Exception
	 */
	private void getDriveMeasurements(final Session session, final long acid,
			final HypervisorConnector conn, final MonitorGatherer gatherer,
			final List<MeasureBean> measures, final VirtualMachineInstance vm)
			throws Exception {
		final List<String> drives = vm.getDriveIds();
		if (drives.size() == 0) {
			logger.info("No Drives found for [" + vm.getName() + "]");
		}
		for (final String driveId : drives) {
			if (driveId.equals("hd")) {
				continue;
			}
			logger.debug("Monitoring drive " + vm.getName() + " " + driveId);
			final String id = convertId(vm.getName());
			{
				final double readByte = gatherer.getDiskReadBytes(conn,
						vm.getName(), driveId);
				final MeasureBean b = new MeasureBean();
				final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
				dimensions.add(CWUtil.getDimensionBean(session, acid,
						Constants.INSTANCEID, id, true));
				b.setDimensions(dimensions);
				b.setName(MonitorConstants.DISK_READ_BYTES_COMMAND);
				b.setNamespace(namespace);
				b.setTimestamp(new Date());
				b.setUnit(MonitorConstants.BYTE_UNIT);
				b.setValue(readByte);
				measures.add(b);
			}
			{
				final double writeByte = gatherer.getDiskWriteBytes(conn,
						vm.getName(), driveId);
				final MeasureBean b = new MeasureBean();
				final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
				dimensions.add(CWUtil.getDimensionBean(session, acid,
						Constants.INSTANCEID, id, true));
				b.setDimensions(dimensions);
				b.setName(MonitorConstants.DISK_WRITE_BYTES_COMMAND);
				b.setNamespace(namespace);
				b.setTimestamp(new Date());
				b.setUnit(MonitorConstants.BYTE_UNIT);
				b.setValue(writeByte);
				measures.add(b);
			}
			{
				final double diskRead = gatherer.getDiskReadOps(conn,
						vm.getName(), driveId);
				final MeasureBean b = new MeasureBean();
				final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
				dimensions.add(CWUtil.getDimensionBean(session, acid,
						Constants.INSTANCEID, id, true));
				b.setDimensions(dimensions);
				b.setName(MonitorConstants.DISK_READ_OPS_COMMAND);
				b.setNamespace(namespace);
				b.setTimestamp(new Date());
				b.setUnit(MonitorConstants.COUNT_PER_SECOND_UNIT);
				b.setValue(diskRead);
				measures.add(b);
			}
			{
				final double diskWrite = gatherer.getDiskWriteOps(conn,
						vm.getName(), driveId);
				final MeasureBean b = new MeasureBean();
				final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
				dimensions.add(CWUtil.getDimensionBean(session, acid,
						Constants.INSTANCEID, id, true));
				b.setDimensions(dimensions);
				b.setName(MonitorConstants.DISK_WRITE_OPS_COMMAND);
				b.setNamespace(namespace);
				b.setTimestamp(new Date());
				b.setUnit(MonitorConstants.COUNT_PER_SECOND_UNIT);
				b.setValue(diskWrite);
				measures.add(b);
			}
		}
	}

	/**
	 * Get Network bytes writen/read for the VirtualMachineInstance.
	 *
	 * @param conn
	 *
	 * @param gatherer
	 * @param measures
	 * @param vm
	 * @param dims
	 * @throws Exception
	 */
	private void getNetworkMeasurements(final Session session, final long acid,
			final HypervisorConnector conn, final MonitorGatherer gatherer,
			final List<MeasureBean> measures, final VirtualMachineInstance vm)
			throws Exception {

		final List<String> networks = vm.getNetworkIds();
		if (networks.size() == 0) {
			logger.info("No Networks found for [" + vm.getName() + "]");
		}
		for (final String ethId : networks) {
			if (ethId == null || ethId.isEmpty()) {
				continue;
			}
			final String id = convertId(vm.getName());
			{
				final double netIn = gatherer.getNetworkIn(conn, vm.getName(),
						ethId);
				final MeasureBean b = new MeasureBean();
				final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
				dimensions.add(CWUtil.getDimensionBean(session, acid,
						Constants.INSTANCEID, id, true));
				b.setDimensions(dimensions);
				b.setName(MonitorConstants.NETWORK_IN_COMMAND);
				b.setNamespace(namespace);
				b.setTimestamp(new Date());
				b.setUnit(MonitorConstants.BITS_PER_SECOND_UNIT);
				b.setValue(netIn);
				measures.add(b);
			}
			{
				final double netOut = gatherer.getNetworkOut(conn,
						vm.getName(), ethId);
				final MeasureBean b = new MeasureBean();
				final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
				dimensions.add(CWUtil.getDimensionBean(session, acid,
						Constants.INSTANCEID, id, true));
				b.setDimensions(dimensions);
				b.setName(MonitorConstants.NETWORK_OUT_COMMAND);
				b.setNamespace(namespace);
				b.setTimestamp(new Date());
				b.setUnit(MonitorConstants.BITS_PER_SECOND_UNIT);
				b.setValue(netOut);
				measures.add(b);
			}
		}
	}

	@Override
	public String process0(final Session s, final HttpServletRequest req,
			final HttpServletResponse resp, final Map<String, String[]> map)
			throws Exception {
		gather(s);
		return "DONE";
	}
}
