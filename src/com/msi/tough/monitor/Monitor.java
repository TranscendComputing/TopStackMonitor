package com.msi.tough.monitor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;

import com.msi.tough.core.HibernateUtil;
import com.msi.tough.model.monitor.MeasureBean;
import com.msi.tough.monitor.common.manager.MeasureHandler;
import com.msi.tough.monitor.common.model.VirtualMachineInstance;
import com.msi.tough.monitor.common.model.exception.MSIMonitorException;
import com.msi.tough.monitor.connector.HypervisorConnector;
import com.msi.tough.monitor.connector.collect.MonitorGatherer;

/**
 * This is an agentless Monitor for Hypervisors.
 *
 * @author heathm
 */
public class Monitor {
	public static final int DEFAULT_TIME_TO_COLLECT = 60; // in seconds
	private int collectTime = DEFAULT_TIME_TO_COLLECT;
	private final List<VirtualMachineInstance> instances;
	private HypervisorConnector conn;
	private MeasureHandler mHandler;

	public Monitor(final HypervisorConnector conn) {
		this.conn = conn;
		instances = new ArrayList<VirtualMachineInstance>();
	}

	public Monitor(final HypervisorConnector conn,
			final List<VirtualMachineInstance> instances) {
		this.conn = conn;
		this.instances = instances;
	}

	/**
	 * Add a VirtualMachineInstance to the list of monitored instances.
	 *
	 * @param vm
	 */
	public void addInstance(final VirtualMachineInstance vm) {
		instances.add(vm);
	}

	/**
	 * Gathers AWS style measures aggregates them based on the measureCollector.
	 * and stores them via the measureHandler.store() mechanism defined.
	 *
	 * @param checkAlarm
	 *
	 * @throws MSIMonitorException
	 */
	public long gather() throws MSIMonitorException {
		try {
			if (!conn.isConnected()) {
				conn.connect();
			}
			final long b = System.currentTimeMillis();
			final MonitorGatherer gatherer = conn.getGatherer();

			if (instances == null || instances.size() == 0) {
				return 0;
			}

			HibernateUtil.withNewSession(new HibernateUtil.Operation<Object>() {

				@Override
				public Object ex(final Session session, final Object... args)
						throws Exception {
					final List<MeasureBean> measures = new ArrayList<MeasureBean>();

					// Gather metrics
					for (final VirtualMachineInstance vm : instances) {
						getCPUMeasurements(session, gatherer, measures, vm);
						vm.setDriveIds(gatherer.getDriveIds(conn, vm.getName()));
						getDriveMeasurements(session, gatherer, measures, vm);
						vm.setNetworkIds(gatherer.getNetworkIds(conn,
								vm.getName()));
						getNetworkMeasurements(session, gatherer, measures, vm);
					}
					// handle Measures
					for (final MeasureBean measure : measures) {
						mHandler.store(session, measure);
					}
					return null;
				}
			});
			final long e = Calendar.getInstance().getTimeInMillis();
			return e - b;
		} catch (final Exception e) {
			return 0;
		}
	}

	public void gatherAS(final String accessKey, final String secretKey)
			throws MSIMonitorException {
		// final AWSCredentials creds = new BasicAWSCredentials(accessKey,
		// secretKey);
		//
		// final AmazonAutoScaling aas = new AmazonAutoScalingClient(creds);
		// aas.setEndpoint("http://172.17.1.100:8080/AutoScaleQuery");
		// final DescribeAutoScalingGroupsResult dagr = aas
		// .describeAutoScalingGroups();
		//
		// final List<AutoScalingGroup> asgroups = dagr.getAutoScalingGroups();
		// final List<MeasureBean> measures = new ArrayList<MeasureBean>();
		//
		// for (final AutoScalingGroup as : asgroups) {
		// cal = Calendar.getInstance();
		// // final List<DimensionBean> dims = new ArrayList<DimensionBean>();
		// // dims.add(new DimensionBean(DimensionType.GROUP, as
		// // .getAutoScalingGroupName()));
		//
		// measures.add(new MeasureBean(as.getAutoScalingGroupName(),
		// "GroupMinSize", "MSI/AutoScaling", MonitorConstants.COUNT_UNIT,
		// String.valueOf(as.getMinSize()), getUTCtime()));
		// measures.add(new MeasureBean(as.getAutoScalingGroupName(),
		// "GroupMaxSize", "MSI/AutoScaling", MonitorConstants.COUNT_UNIT,
		// String.valueOf(as.getMaxSize()), getUTCtime()));
		// measures.add(new MeasureBean(as.getAutoScalingGroupName(),
		// "GroupDesiredCapacity", "MSI/AutoScaling",
		// MonitorConstants.COUNT_UNIT, String.valueOf(as
		// .getDesiredCapacity()), getUTCtime()));
		//
		// final List<Instance> instList = as.getInstances();
		//
		// measures.add(new MeasureBean(as.getAutoScalingGroupName(),
		// "GroupTotalInstances", "MSI/AutoScaling",
		// MonitorConstants.COUNT_UNIT, String.valueOf(instList.size()),
		// getUTCtime()));
		// int runningInstances = 0;
		// int pendingInstances = 0;
		// int terminatingInstances = 0;
		// for (final Instance inst : instList) {
		// final String state = inst.getLifecycleState();
		// if (state.equalsIgnoreCase("running")) {
		// runningInstances++;
		// }
		// if (state.equalsIgnoreCase("pending")) {
		// pendingInstances++;
		// }
		// if (state.equalsIgnoreCase("shutting-down")) {
		// terminatingInstances++;
		// }
		// }
		// measures.add(new MeasureBean(as.getAutoScalingGroupName(),
		// "GroupInServiceInstances", "MSI/AutoScaling",
		// MonitorConstants.COUNT_UNIT, String.valueOf(runningInstances),
		// getUTCtime()));
		//
		// measures.add(new MeasureBean(as.getAutoScalingGroupName(),
		// "GroupPendingInstances", "MSI/AutoScaling",
		// MonitorConstants.COUNT_UNIT, String.valueOf(pendingInstances),
		// getUTCtime()));
		//
		// measures.add(new MeasureBean(as.getAutoScalingGroupName(),
		// "GroupTerminatingInstances", "MSI/AutoScaling",
		// MonitorConstants.COUNT_UNIT, String.valueOf(terminatingInstances),
		// getUTCtime()));
		// }
		// // handle Measures
		// for (final MeasureBean measure : measures) {
		// mHandler.store(null, measure);
		// }
	}

	public void gatherSQS(final String accessKey, final String secretKey)
			throws MSIMonitorException {
		// final AWSCredentials creds = new BasicAWSCredentials(accessKey,
		// secretKey);
		//
		// final AmazonSQS sqs = new AmazonSQSClient(creds);
		// sqs.setEndpoint("http://172.17.1.100:8080/SQSQuery");
		// final ListQueuesResult lqr = sqs.listQueues();
		// final List<MeasureBean> measures = new ArrayList<MeasureBean>();
		// final List<String> queueUrls = lqr.getQueueUrls();
		//
		// logger.info("QueueUrls SIZE: " + queueUrls.size());
		// for (int i = 0; i < queueUrls.size(); i++) {
		// logger.info(i + ". Url: " + queueUrls.get(i));
		// final GetQueueAttributesRequest request = new
		// GetQueueAttributesRequest(
		// queueUrls.get(i));
		// final GetQueueAttributesResult result = sqs
		// .getQueueAttributes(request);
		// final Map<String, String> attrs = result.getAttributes();
		//
		// // final List<DimensionBean> dims = new ArrayList<DimensionBean>();
		// // dims.add(new DimensionBean(DimensionType.GROUP, attrs
		// // .get("QueueArn")));
		//
		// if (attrs.containsKey("ApproximateNumberOfMessages")) {
		// measures.add(new MeasureBean(attrs.get("QueueArn"),
		// "ApproximateNumberOfMessagesVisible", "MSI/SQS",
		// MonitorConstants.COUNT_UNIT, attrs
		// .get("ApproximateNumberOfMessages"),
		// getUTCtime()));
		// }
		// if (attrs.containsKey("ApproximateNumberOfMessagesNotVisible")) {
		// measures.add(new MeasureBean(attrs.get("QueueArn"),
		// "ApproximateNumberOfMessagesNotVisible", "MSI/SQS",
		// MonitorConstants.COUNT_UNIT, attrs
		// .get("ApproximateNumberOfMessagesNotVisible"),
		// getUTCtime()));
		// }
		// }
		// // handle Measures
		// for (final MeasureBean measure : measures) {
		// mHandler.store(null, measure);
		// }

	}

	/**
	 * Get the last aggregation time.
	 *
	 * @return
	 */
	public int getCollectTime() {
		return collectTime;
	}

	/**
	 * Get a hypervisorConnector object
	 */
	public HypervisorConnector getConn() {
		return conn;
	}

	/**
	 * Grab CPUUtilization information from the virtualMachineInstance.
	 *
	 * @param gatherer
	 * @param measures
	 * @param vm
	 * @param dims
	 * @throws MSIMonitorException
	 */
	private void getCPUMeasurements(final Session session,
			final MonitorGatherer gatherer, final List<MeasureBean> measures,
			final VirtualMachineInstance vm) throws MSIMonitorException {
		// final double cpu = gatherer.getCPUUtilization(conn, vm.getName());
		// final MeasureBean b = new MeasureBean();
		// final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
		// dimensions.add(CWUtil.getDimensionBean(session, Constants.INSTANCEID,
		// convertId(vm.getName())));
		// b.setDimensions(dimensions);
		// b.setName(MonitorConstants.CPU_UTILIZATION_COMMAND);
		// b.setNamespace(namespace);
		// b.setTimestamp(new Date());
		// b.setUnit(MonitorConstants.PERCENT_UNIT);
		// b.setValue(cpu);
		// measures.add(b);
	}

	/**
	 * Grab Disk I/O information from the VirtualMachineInstance.
	 *
	 * @param gatherer
	 * @param measures
	 * @param vm
	 * @param dims
	 * @throws MSIMonitorException
	 */
	private void getDriveMeasurements(final Session session,
			final MonitorGatherer gatherer, final List<MeasureBean> measures,
			final VirtualMachineInstance vm) throws MSIMonitorException {
		// final List<String> drives = vm.getDriveIds();
		// if (drives.size() == 0) {
		// logger.info("No Drives found for [" + vm.getName() + "]");
		// }
		// for (final String driveId : drives) {
		// if (driveId.equals("hd")) {
		// continue;
		// }
		// logger.debug("Monitoring drive " + vm.getName() + " " + driveId);
		// final String id = convertId(vm.getName());
		// {
		// final double readByte = gatherer.getDiskReadBytes(conn,
		// vm.getName(), driveId);
		// final MeasureBean b = new MeasureBean();
		// final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
		// dimensions.add(CWUtil.getDimensionBean(session,
		// Constants.INSTANCEID, id));
		// b.setDimensions(dimensions);
		// b.setName(MonitorConstants.DISK_READ_BYTES_COMMAND);
		// b.setNamespace(namespace);
		// b.setTimestamp(new Date());
		// b.setUnit(MonitorConstants.BYTE_UNIT);
		// b.setValue(readByte);
		// measures.add(b);
		// }
		// {
		// final double writeByte = gatherer.getDiskWriteBytes(conn,
		// vm.getName(), driveId);
		// final MeasureBean b = new MeasureBean();
		// final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
		// dimensions.add(CWUtil.getDimensionBean(session,
		// Constants.INSTANCEID, id));
		// b.setDimensions(dimensions);
		// b.setName(MonitorConstants.DISK_WRITE_BYTES_COMMAND);
		// b.setNamespace(namespace);
		// b.setTimestamp(new Date());
		// b.setUnit(MonitorConstants.BYTE_UNIT);
		// b.setValue(writeByte);
		// measures.add(b);
		// }
		// {
		// final double diskRead = gatherer.getDiskReadOps(conn,
		// vm.getName(), driveId);
		// final MeasureBean b = new MeasureBean();
		// final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
		// dimensions.add(CWUtil.getDimensionBean(session,
		// Constants.INSTANCEID, id));
		// b.setDimensions(dimensions);
		// b.setName(MonitorConstants.DISK_READ_OPS_COMMAND);
		// b.setNamespace(namespace);
		// b.setTimestamp(new Date());
		// b.setUnit(MonitorConstants.COUNT_PER_SECOND_UNIT);
		// b.setValue(diskRead);
		// measures.add(b);
		// }
		// {
		// final double diskWrite = gatherer.getDiskWriteOps(conn,
		// vm.getName(), driveId);
		// final MeasureBean b = new MeasureBean();
		// final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
		// dimensions.add(CWUtil.getDimensionBean(session,
		// Constants.INSTANCEID, id));
		// b.setDimensions(dimensions);
		// b.setName(MonitorConstants.DISK_WRITE_OPS_COMMAND);
		// b.setNamespace(namespace);
		// b.setTimestamp(new Date());
		// b.setUnit(MonitorConstants.COUNT_PER_SECOND_UNIT);
		// b.setValue(diskWrite);
		// measures.add(b);
		// }
		// }
	}

	/**
	 * Get a VirtualMachineInstance from the name of the guest instance.
	 *
	 * @param name
	 * @return
	 */
	public VirtualMachineInstance getInstance(final String name) {
		for (final VirtualMachineInstance vm : instances) {
			if (vm.getName() != null && vm.getName().equals(name)) {
				return vm;
			}
		}
		return null;
	}

	/**
	 * Get Network bytes writen/read for the VirtualMachineInstance.
	 *
	 * @param gatherer
	 * @param measures
	 * @param vm
	 * @param dims
	 * @throws MSIMonitorException
	 */
	private void getNetworkMeasurements(final Session session,
			final MonitorGatherer gatherer, final List<MeasureBean> measures,
			final VirtualMachineInstance vm) throws MSIMonitorException {
		//
		// final List<String> networks = vm.getNetworkIds();
		// if (networks.size() == 0) {
		// logger.info("No Networks found for [" + vm.getName() + "]");
		// }
		// for (final String ethId : networks) {
		// if (ethId == null || ethId.isEmpty()) {
		// continue;
		// }
		// final String id = convertId(vm.getName());
		// {
		// final double netIn = gatherer.getNetworkIn(conn, vm.getName(),
		// ethId);
		// final MeasureBean b = new MeasureBean();
		// final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
		// dimensions.add(CWUtil.getDimensionBean(session,
		// Constants.INSTANCEID, id));
		// b.setDimensions(dimensions);
		// b.setName(MonitorConstants.NETWORK_IN_COMMAND);
		// b.setNamespace(namespace);
		// b.setTimestamp(new Date());
		// b.setUnit(MonitorConstants.BITS_PER_SECOND_UNIT);
		// b.setValue(netIn);
		// measures.add(b);
		// }
		// {
		// final double netOut = gatherer.getNetworkOut(conn,
		// vm.getName(), ethId);
		// final MeasureBean b = new MeasureBean();
		// final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
		// dimensions.add(CWUtil.getDimensionBean(session,
		// Constants.INSTANCEID, id));
		// b.setDimensions(dimensions);
		// b.setName(MonitorConstants.NETWORK_OUT_COMMAND);
		// b.setNamespace(namespace);
		// b.setTimestamp(new Date());
		// b.setUnit(MonitorConstants.BITS_PER_SECOND_UNIT);
		// b.setValue(netOut);
		// measures.add(b);
		// }
		// }
	}

	/**
	 * Remove the VirtualMachineInstance from the list of gathered monitoring
	 * info.
	 *
	 * @param vm
	 */
	public void removeInstance(final VirtualMachineInstance vm) {
		instances.remove(vm);
	}

	/**
	 * Sets the last aggregation time to the given value. (unix timestamp)
	 *
	 * @param collectTime
	 */
	public void setCollectTime(final int collectTime) {
		this.collectTime = collectTime;
	}

	/**
	 * Set the HypervisorConnector to use for this Monitor. Currently this is a
	 * one to one mapping of monitor to Hypervisor.
	 *
	 * @param conn
	 */
	public void setConn(final HypervisorConnector conn) {
		this.conn = conn;
	}

	public void setInstances(final List<VirtualMachineInstance> vms) {
		if (vms != null) {
			instances.clear();
			instances.addAll(vms);
		}
	}

	/**
	 * Sets the Object that handles storage of Measure data / Metric Summary
	 * data / retrieval of Metric Dimensions and Namespace information.
	 *
	 * @param mHandler
	 */
	public void setMeasureHandler(final MeasureHandler mHandler) {
		this.mHandler = mHandler;
	}
	//
	// public void storeMetrics(final Session session, final String mName,
	// final DimensionBean db) {
	// final Criteria crit = session.createCriteria(UserStatisticBean.class);
	// crit.add(Restrictions.eq("metricName", mName));
	// crit.add(Restrictions.eq("namespace", namespace));
	// crit.createCriteria("dimensions").createCriteria("dimensions")
	// .add(Restrictions.eq("value", db.getValue()));
	// //
	// System.out.println("DB: "+db.getValue()+" S1: "+crit.list().size()+" S2: "+crit2.list().size()+" mName "+mName);
	// final List<DimensionBean> dbList = new ArrayList<DimensionBean>();
	// final DimensionBean duplicate_db = new DimensionBean(db.getType(),
	// db.getValue());
	// dbList.add(duplicate_db);
	// if (crit.list().size() == 0) {
	// // session.save(new UserStatisticBean(mName, this.namespace,
	// // dbList));
	// final UserStatisticBean usb = new UserStatisticBean(mName,
	// namespace, dbList);
	// usb.save(session);
	// // System.out.println(usb);
	// }
	// /*
	// * else{ System.out.println("Match!"); for(UserStatisticBean usb :
	// * (List<UserStatisticBean>) crit.list()){ System.out.println(usb); } }
	// */
	// }
}
