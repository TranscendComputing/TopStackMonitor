package com.msi.tough.monitor.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.slf4j.Logger;

import com.msi.tough.core.Appctx;
import com.msi.tough.model.elasticache.CacheClusterBean;
import com.msi.tough.model.monitor.DimensionBean;
import com.msi.tough.query.UnsecuredAction;
import com.msi.tough.utils.CWUtil;

public class GatherECache extends UnsecuredAction {
	private static Logger logger = Appctx.getLogger(GatherECache.class
			.getName());

	/**
     *
     */
    public GatherECache() {
        super();
        setLessVerbose(true); // Avoid lots of logging; this is called on timer.
    }

    // memcached =========
    /*
	private static String[] memstrs = new String[] { "BytesUsedForCacheItems",
			"bytes", "BytesReadIntoMemcached", "bytes_read", "CasBadval",
			"cas_badval", "CasHits", "cas_hits", "CasMisses", "cas_misses",
			"CmdFlush", "cmd_flush", "CmdGet", "cmd_get", "CmdSet", "cmd_set",
			"CPUUtilization", "rusage_system", "CurrConnections",
			"curr_connections", "CurrItems", "curr_items", "DecrHits",
			"decr_hits", "DecrMisses", "decr_misses", "DeleteHits",
			"delete_hits", "DeleteMisses", "delete_misses", "Evictions",
			"evictions", "GetHits", "get_hits", "GetMisses", "get_misses",
			"IncrHits", "incr_hits", "IncrMisses", "incr_misses", "Reclaimed",
			"reclaimed" };
    */

	@SuppressWarnings({ "unchecked" })
	private void gather(final Session s) throws Exception {
		//final MeasureHandler mHandler = Appctx.getBean("measurehandler");
		final List<CacheClusterBean> cache = s.createQuery(
				"from CacheClusterBean").list();
		for (final CacheClusterBean cluster : cache) {
			// if (!cluster.getLbStatus().endsWith("ready")) {
			// logger.debug("Ignoring AC " + cluster.getUserId() + " LB "
			// + cluster.getLoadBalancerName() + " Status "
			// + cluster.getLbStatus());
			// continue;
			// }
			final long acid = cluster.getAcid();
			logger.debug("Gather AC " + acid + " Cluster " + cluster.getName());
			//final AccountBean acb = AccountUtil.readAccount(s, acid);
			//final AccountType ac = AccountUtil.toAccount(acb);

			final DimensionBean dim = CWUtil.getDimensionBean(s, acid,
					"CacheClusterId", cluster.getName(), true);
			final Set<DimensionBean> dims = new HashSet<DimensionBean>();
			dims.add(dim);
			//final List<String> al = Arrays.asList(memstrs);
			// for (final CacheNodeBean cn : cluster.getCacheNodes()) {
			// final MemcachedClient client = new MemcachedClient(
			// new InetSocketAddress(cn.getAddress(), cn.getPort()));
			//
			// final Map<SocketAddress, Map<String, String>> stats = client
			// .getStats();
			// for (final SocketAddress sa : stats.keySet()) {
			// final Map<String, String> statList = stats.get(sa);
			// for (final Entry<String, String> stat : statList.entrySet()) {
			// final int idx = al.indexOf(stat.getKey());
			// if (idx == -1) {
			// continue;
			// }
			// final MeasureBean measure = new MeasureBean();
			// measure.setDimensions(dims);
			// measure.setName(al.get(idx - 1));
			// measure.setNamespace("AWS/ElastiCache");
			// measure.setTimestamp(new Date());
			// measure.setUnit("Count");
			// final double value = Double
			// .parseDouble(stat.getValue());
			// measure.setValue(value);
			// mHandler.store(s, measure);
			// }
			// }
			// client.shutdown(10, TimeUnit.SECONDS);
			// }
		}

		// http://docs.amazonwebservices.com/AmazonElastiCache/latest/UserGuide/CacheMetrics.html
		// Instance =====
		// CPUUtilization
		// SwapUsage
		// FreeableMemory
		// NetworkBytesIn
		// NetworkBytesOut
		// calculated ===============
		// NewConnections delta STAT total_connections 4
		// NewItems delta STAT total_items 0
		// UnusedMemory STAT limit_maxbytes 7444889600 - STAT bytes 0
	}

	@Override
	public String process0(final Session s, final HttpServletRequest req,
			final HttpServletResponse resp, final Map<String, String[]> map)
			throws Exception {
		gather(s);
		return "DONE";
	}
}
