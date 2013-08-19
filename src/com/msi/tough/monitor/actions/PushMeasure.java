package com.msi.tough.monitor.actions;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;

import com.msi.tough.query.AbstractAction;
import com.msi.tough.query.MarshallStruct;

public class PushMeasure extends AbstractAction<Object> {
	//private final static Logger logger = Appctx.getLogger(PushMeasure.class
	//		.getName());

	@Override
	public String marshall(MarshallStruct<Object> input,
			HttpServletResponse resp) throws Exception {
		return null;
	}

	@Override
	public Object process0(Session session, HttpServletRequest req,
			HttpServletResponse resp, Map<String, String[]> map)
			throws Exception {
		/*
		 * final MonitorQueryImpl impl = Appctx.getBean("monitorQuery");
		 * req.setAttribute("Action", "PutMetricData"); impl.process(req, resp);
		 * PutMetricData pmr = new PutMetricData(); pmr.process0(session, req,
		 * resp, map);
		 */
		return null;
	}

	public void unmarshall(HttpServletRequest req) {

	}

}
