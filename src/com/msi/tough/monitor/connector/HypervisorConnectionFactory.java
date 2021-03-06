package com.msi.tough.monitor.connector;

import java.util.Map;

import com.msi.tough.model.monitor.HypervisorConfigBean;
import com.msi.tough.monitor.connector.collect.platform.XenGatherer;
import com.msi.tough.monitor.connector.platform.VirtConnector;

public class HypervisorConnectionFactory {

	public static HypervisorConnector getConnector(
			final HypervisorConfigBean conf) {
		final String type = conf.getType();
		final String proto = conf.getProto();
		final String username = conf.getUsername();
		final String password = conf.getPassword();
		final String host = conf.getHost();
		final Map<String, String> options = conf.getOptions();

		if ("xen".equalsIgnoreCase(type) || "qemu".equalsIgnoreCase(type)) {
			final VirtConnector xenConnector = new VirtConnector(type, proto,
					username, password, host, options);
			xenConnector.setGatherer(new XenGatherer(options));
			return xenConnector;
		}
		return null;
	}
}
