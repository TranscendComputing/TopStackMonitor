package com.msi.tough.monitor.actions;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonNode;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.msi.tough.cf.AccountType;
import com.msi.tough.core.Appctx;
import com.msi.tough.core.CommaObject;
import com.msi.tough.core.JsonUtil;
import com.msi.tough.core.StringHelper;
import com.msi.tough.model.AccountBean;
import com.msi.tough.model.LoadBalancerBean;
import com.msi.tough.model.monitor.DimensionBean;
import com.msi.tough.model.monitor.MeasureBean;
import com.msi.tough.monitor.common.manager.MeasureHandler;
import com.msi.tough.query.UnsecuredAction;
import com.msi.tough.utils.AccountUtil;
import com.msi.tough.utils.CFUtil;
import com.msi.tough.utils.CWUtil;
import com.msi.tough.utils.ChefUtil;

public class GatherLoadBalancer extends UnsecuredAction {

    private static Logger logger = Appctx.getLogger(GatherLoadBalancer.class
            .getName());

    /**
     * Default constructor.
     */
    public GatherLoadBalancer() {
        super();
        setLessVerbose(true); // Avoid lots of logging; this is called on timer.
        setContextSession(true);
        setManagedTx(true);
    }

    @Override
    @Transactional
    public void process(final HttpServletRequest req,
            final HttpServletResponse resp) throws Exception {
        super.process(req, resp);
    }

	@SuppressWarnings({ "unchecked" })
	private void gather(final Session s) throws Exception {
		final MeasureHandler mHandler = Appctx.getBean("measurehandler");
		final List<LoadBalancerBean> lbl = s.createQuery(
				"from LoadBalancerBean").list();
		for (final LoadBalancerBean lb : lbl) {
			if (StringHelper.isBlank(lb.getLbStatus()) ||
			        !lb.getLbStatus().endsWith("ready")) {
				logger.debug("Ignoring AC " + lb.getUserId() + " LB "
						+ lb.getLoadBalancerName() + " Status "
						+ lb.getLbStatus());
				continue;
			}
			logger.debug("Gather AC " + lb.getUserId() + " LB "
					+ lb.getLoadBalancerName());
			final AccountBean acb = AccountUtil.readAccount(s, lb.getUserId());
			final AccountType ac = AccountUtil.toAccount(acb);

			final Map<String, Object> cmd = new HashMap<String, Object>();
			cmd.put("Command", "MonitorData");
			cmd.put("Period", "60");
			ChefUtil.deleteDatabagItem(lb.getDatabag(), "response");
			ChefUtil.createDatabagItem(lb.getDatabag(), "command",
					JsonUtil.toJsonString(cmd));

			final CommaObject cin = new CommaObject(lb.getLbInstances());
			CFUtil.runChefClient(s, ac, cin.toList());

			String resp = null;
			// for (int i = 0; i < 10; i++) {
			// Thread.sleep(1000);
			resp = ChefUtil.getDatabagItem(lb.getDatabag(), "response");
			// if (resp != null) {
			// break;
			// }
			// }
			if (resp == null) {
				logger.debug("Response not found " + lb.getLoadBalancerName());
				continue;
			}
			logger.debug("Response " + lb.getLoadBalancerName() + " " + resp);
			ChefUtil.deleteDatabagItem(lb.getDatabag(), "command");
			ChefUtil.deleteDatabagItem(lb.getDatabag(), "response");
			final JsonNode json = JsonUtil.load(resp);
			final Map<String, Object> jm = JsonUtil.toMap(json);
			final DimensionBean dim = CWUtil.getDimensionBean(s, ac.getId(),
					"LoadBalancer", lb.getLoadBalancerName(), true);
			final Set<DimensionBean> dims = new HashSet<DimensionBean>();
			dims.add(dim);

			for (final String k : new String[] { "Latency", "RequestCount",
					"UnHealthyHostCount", "HealthyHostCount",
					"HTTPCode_Backend_2XX", "HTTPCode_Backend_3XX",
					"HTTPCode_Backend_4XX", "HTTPCode_Backend_5XX",
					"HTTPCode_ELB_4XX"

			}) {
				if (jm.containsKey(k)) {
					final MeasureBean measure = new MeasureBean();
					measure.setDimensions(dims);
					measure.setName(k);
					measure.setNamespace("MSI/EC2");
					measure.setTimestamp(new Date());
					measure.setUnit(k.equals("Latency") ? "Seconds" : "Count");
					final double value = Double.parseDouble((String) jm.get(k));
					measure.setValue(value);
					mHandler.store(s, measure);
				}
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
