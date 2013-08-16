package com.msi.tough.monitor.actions;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;

import com.msi.tough.query.UnsecuredAction;

public class GatherRDS extends UnsecuredAction {
	//private static Logger logger = Appctx.getLogger(GatherRDS.class.getName());

	private void gather(final Session s) throws Exception {
		//final MeasureHandler mHandler = Appctx.getBean("measurehandler");
		//final List<CacheClusterBean> cache = s.createQuery(
		//		"from CacheClusterBean").list();
		//for (final CacheClusterBean cluster : cache) {
			// if (!cluster.getLbStatus().endsWith("ready")) {
			// logger.debug("Ignoring AC " + cluster.getUserId() + " LB "
			// + cluster.getLoadBalancerName() + " Status "
			// + cluster.getLbStatus());
			// continue;
			// }
			// final long acid = cluster.getAccount().getId();
			// logger.debug("Gather AC " + acid + " Cluster " +
			// cluster.getName());
			// final AccountBean acb = cluster.getAccount();
			// final AccountType ac = AccountUtil.toAccount(acb);
			//
			// final DimensionBean dim = CWUtil.getDimensionBean(s, acb.getId(),
			// "CacheClusterId", cluster.getName(), true);
			// final Set<DimensionBean> dims = new HashSet<DimensionBean>();
			// dims.add(dim);
			// BinLogDiskUsage Bytes
			// CPUUtilization Percent
			// DatabaseConnections Count
			// FreeableMemory Bytes
			// FreeStorageSpace Bytes
			// ReplicaLag Seconds
			// SwapUsage Bytes
			// ReadIOPS Count/Second
			// WriteIOPS Count/Second
			// ReadLatency Seconds
			// WriteLatency Seconds
			// ReadThroughput Bytes/Second
			// WriteThroughput Bytes/Second

		//}
	}

	@Override
	public String process0(final Session s, final HttpServletRequest req,
			final HttpServletResponse resp, final Map<String, String[]> map)
			throws Exception {
		gather(s);
		return "DONE";
	}
}
