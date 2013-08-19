package com.msi.tough.monitor.actions;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;

import com.generationjava.io.xml.XMLNode;
import com.msi.tough.core.Appctx;
import com.msi.tough.query.AbstractAction;
import com.msi.tough.query.MarshallStruct;

public class DeleteHypervisorConfig extends AbstractAction<Object> {
	private final static Logger logger = Appctx
			.getLogger(DeleteHypervisorConfig.class.getName());

	private String host;

	@Override
	public String marshall(MarshallStruct<Object> input,
			HttpServletResponse resp) throws Exception {
		logger.debug("Marshalling the response into the xml format...");
		XMLNode aimr = new XMLNode("DeleteHypervisorConfigResponse");
		XMLNode resMeta = new XMLNode("ResponseMetadata");
		aimr.addNode(resMeta);
		XMLNode rId = new XMLNode("RequestId");
		resMeta.addNode(rId);
		XMLNode rId0 = new XMLNode();
		rId0.setPlaintext(getRequestId());
		rId.addNode(rId0);
		return aimr.toString();
	}

	@Override
	public Object process0(Session session, HttpServletRequest req,
			HttpServletResponse resp, Map<String, String[]> map)
			throws Exception {
		unmarshall(req);

		logger.debug("Deleting HypervisorConfig...");

		String hql = "delete from HypervisorConfigBean where host = '" + host
				+ "'";
		Query query = session.createQuery(hql);
		int row = query.executeUpdate();

		logger.debug(row + " HypervisorConfig deleted");
		return null;
	}

	public void unmarshall(HttpServletRequest req) {
		logger.debug("Unmarshalling the DeleteHypervisorConfig request...");

		host = req.getParameter("Host");

	}

}
