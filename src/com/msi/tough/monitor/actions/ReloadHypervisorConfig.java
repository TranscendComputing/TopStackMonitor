package com.msi.tough.monitor.actions;

import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;

import com.generationjava.io.xml.XMLNode;
import com.msi.tough.core.Appctx;
import com.msi.tough.model.monitor.HypervisorConfigBean;
import com.msi.tough.monitor.HypervisorMonitor;
import com.msi.tough.monitor.common.manager.MeasureHandler;
import com.msi.tough.monitor.common.manager.RDBMSMeasureHandler;
import com.msi.tough.monitor.connector.HypervisorConnectionFactory;
import com.msi.tough.monitor.connector.HypervisorConnector;
import com.msi.tough.query.AbstractAction;
import com.msi.tough.query.MarshallStruct;

public class ReloadHypervisorConfig extends AbstractAction<Object> {
	private final static Logger logger = Appctx
			.getLogger(ReloadHypervisorConfig.class.getName());

	@Override
	public String marshall(final MarshallStruct<Object> input,
			final HttpServletResponse resp) throws Exception {
		logger.debug("Marshalling the response into the xml format...");
		final XMLNode aimr = new XMLNode("ReloadHypervisorConfigResponse");
		final XMLNode resMeta = new XMLNode("ResponseMetadata");
		aimr.addNode(resMeta);
		final XMLNode rId = new XMLNode("RequestId");
		resMeta.addNode(rId);
		final XMLNode rId0 = new XMLNode();
		rId0.setPlaintext(getRequestId());
		rId.addNode(rId0);
		return aimr.toString();
	}

	@Override
	public Object process0(final Session session, final HttpServletRequest req,
			final HttpServletResponse resp, final Map<String, String[]> map)
			throws Exception {
		// Get all application-scoped attributes
		final ServletContext sc = req.getSession().getServletContext();

		final String hql = "from com.msi.tough.model.monitor.HypervisorConfigBean";
		final Query query = session.createQuery(hql);

		@SuppressWarnings("unchecked")
        final List<HypervisorConfigBean> hvlist = query.list();

		@SuppressWarnings("unchecked")
        final List<HypervisorMonitor> hvmlist = (List<HypervisorMonitor>) sc
				.getAttribute("monitor.hypervisormonitor.list");
		final long gather_sleep = (Long) sc
				.getAttribute("monitor.hypervisormonitor.sleep");

		if (hvlist.size() < hvmlist.size()) {
			for (int i = 0; i < hvlist.size(); i++) {
				final HypervisorMonitor hvm = hvmlist.get(i);
				hvm.configConn(hvlist.get(i));
			}

			final int i = hvlist.size();
			while (hvmlist.size() > hvlist.size()) {
				hvmlist.get(i).terminate();
				hvmlist.remove(i);
			}
		} else {
			for (int i = 0; i < hvmlist.size(); i++) {
				final HypervisorMonitor hvm = hvmlist.get(i);
				hvm.configConn(hvlist.get(i));
			}
			final MeasureHandler mHandler = new RDBMSMeasureHandler();
			for (int i = 0; i < hvlist.size(); i++) {
				final HypervisorConfigBean config = hvlist.get(i);

				final HypervisorConnector conn = HypervisorConnectionFactory
						.getConnector(config);
				final HypervisorMonitor hvm = new HypervisorMonitor(mHandler,
						conn, gather_sleep);
				hvmlist.add(hvm);
			}
		}

		return null;
	}

}
