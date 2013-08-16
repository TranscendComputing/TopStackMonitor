package com.msi.tough.monitor.actions;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.slf4j.Logger;

import com.generationjava.io.xml.XMLNode;
import com.msi.tough.core.Appctx;
import com.msi.tough.model.monitor.HypervisorConfigBean;
import com.msi.tough.query.AbstractAction;
import com.msi.tough.query.MarshallStruct;

public class AddHypervisorConfig extends AbstractAction<Object> {
	private final static Logger logger = Appctx
			.getLogger(AddHypervisorConfig.class.getName());

	private String username;
	private String type;
	private String proto;
	private String password;
	private String host;
	private final Map<String, String> options = new HashMap<String, String>();

	@Override
	public String marshall(MarshallStruct<Object> input,
			HttpServletResponse resp) throws Exception {
		logger.debug("Marshalling the response into the xml format...");
		XMLNode aimr = new XMLNode("AddHypervisorConfigResponse");
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
			HttpServletResponse resp, Map<String, String[]> map) throws Exception {
		unmarshall(req);

		logger.debug("Adding HypervisorConfig...");

		HypervisorConfigBean add = new HypervisorConfigBean(username, type,
				proto, password, host, options);
		session.save(add);

		return null;
	}

	public void unmarshall(HttpServletRequest req) {
		logger.debug("Unmarshalling the AddHypervisorConfig request...");

		username = req.getParameter("Username");
		type = req.getParameter("Type");
		proto = req.getParameter("Protocol");
		password = req.getParameter("Password");
		host = req.getParameter("Host");

		int i = 0;
		while (true) {
			i++;
			String name = req.getParameter("Options." + i + ".name");
			String value = req.getParameter("Options." + i + ".value");
			if (name == null || value == null) {
				break;
			}
			options.put(name, value);
		}
	}

}
