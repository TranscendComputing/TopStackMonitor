package com.msi.tough.monitor.actions;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;

import com.generationjava.io.xml.XMLNode;
import com.msi.tough.core.Appctx;
import com.msi.tough.model.monitor.HypervisorConfigBean;
import com.msi.tough.query.AbstractAction;
import com.msi.tough.query.MarshallStruct;

public class ViewHypervisorConfig extends AbstractAction<Object> {
	private final static Logger logger = Appctx
			.getLogger(ViewHypervisorConfig.class.getName());
	private List<HypervisorConfigBean> hvlist;

	@Override
	public String marshall(MarshallStruct<Object> input,
			HttpServletResponse resp) throws Exception {
		logger.debug("Marshalling the response into the xml format...");
		XMLNode vhcr = new XMLNode("ViewHypervisorConfigResponse");
		XMLNode vhcres = new XMLNode("ViewHypervisorConfigResult");
		vhcr.addNode(vhcres);

		Iterator<HypervisorConfigBean> itr = hvlist.iterator();

		while (itr.hasNext()) {
			XMLNode host = new XMLNode("Host");
			vhcres.addNode(host);
			XMLNode name = new XMLNode();
			name.setPlaintext(itr.next().getHost());
			host.addNode(name);
		}

		return vhcr.toString();
	}

	@SuppressWarnings("unchecked")
    @Override
	public Object process0(Session session, HttpServletRequest req,
			HttpServletResponse resp, Map<String, String[]> map)
			throws Exception {
		String hql = "from com.msi.tough.model.monitor.HypervisorConfigBean";
		Query query = session.createQuery(hql);

		hvlist = query.list();

		return null;
	}

}
