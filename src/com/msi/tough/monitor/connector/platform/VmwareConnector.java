/**
 *
 */
package com.msi.tough.monitor.connector.platform;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.msi.tough.core.Appctx;
import com.msi.tough.monitor.common.model.VirtualMachineInstance;
import com.msi.tough.monitor.common.model.exception.MSIMonitorException;
import com.msi.tough.monitor.connector.HypervisorConnector;
import com.msi.tough.monitor.connector.collect.MonitorGatherer;
import com.vmware.apputils.AppUtil;
import com.vmware.apputils.OptionSpec;
import com.vmware.vim.DynamicProperty;
import com.vmware.vim.ManagedObjectReference;
import com.vmware.vim.ObjectContent;

/**
 * VMWare HypervisorConnector implementation. Used to monitor the vmware
 * instances through communicating with a vCenter server.
 *
 * @author heathm
 */
public class VmwareConnector implements HypervisorConnector {
	private static final Logger logger = Appctx.getLogger(VmwareConnector.class
			.getName());
	private final String host;
	private final String username;
	private final String password;
	private Map<String, String> options = new HashMap<String, String>();
	private MonitorGatherer gatherer;
	private final AppUtil cb;

	@SuppressWarnings("unchecked")
    public VmwareConnector(final String driver, final String proto,
			final String username, final String password, final String host,
			final Map<String, String> options) {
		this.username = username;
		this.password = password;
		this.host = host;
		this.options = options;
		cb = new AppUtil("ToughMonitor");
		final OptionSpec[] opts = setupOptions();
		Field privateField;
		try {
			for (final OptionSpec opt : opts) {
				logger.debug("OptionSpecs " + opt.getOptionName() + " : "
						+ opt.getOptionDefault());
				privateField = AppUtil.class.getDeclaredField("optsEntered");
				privateField.setAccessible(true);
				((HashMap<String, String>) privateField.get(cb)).put(
						opt.getOptionName(), opt.getOptionDefault());
			}
		} catch (final SecurityException e) {
			e.printStackTrace();
		} catch (final NoSuchFieldException e) {
			e.printStackTrace();
		} catch (final IllegalArgumentException e) {
			e.printStackTrace();
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public String buildConnectionURI() {
		final StringBuilder connURI = new StringBuilder();
		String protocol = "https://";
		if (options.containsKey("proto") && options.get("proto") != null) {
			final String proto = options.get("proto");
			if ("https".equalsIgnoreCase(proto)) {
				protocol = "https://";
			}
		}
		connURI.append(protocol);
		connURI.append(host);
		final String sdkPath = options.containsKey("SDKPath") ? "/"
				+ options.get("SDKPath") : "/sdk";
		connURI.append(sdkPath);
		logger.info("vCenter connection URI is [" + connURI.toString() + "]");
		return connURI.toString();
	}

	@Override
	public boolean connect() {
		boolean connected = false;
		try {
			logger.debug(cb.getAppName() + cb.getServiceUrl()
					+ cb.get_option("url"));
			cb.connect();
			connected = true;
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return connected;
	}

	@Override
	public Object getConnection() throws MSIMonitorException {
		return cb;
	}

	@Override
	public String getDomainId(final VirtualMachineInstance domain)
			throws MSIMonitorException {
		if (domain == null) {
			throw new MSIMonitorException(
					"Trying to get an id of an empty virtual machine entry");
		}
		if (domain.getId() == null && "".equals(domain.getId())) {
			return domain.getId();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.msi.ec2.monitor.connector.HypervisorConnector#getGatherer()
	 */
	@Override
	public MonitorGatherer getGatherer() {
		return gatherer;
	}

	@Override
	public List<VirtualMachineInstance> getGuestDomains()
			throws MSIMonitorException {
		final List<VirtualMachineInstance> res = new ArrayList<VirtualMachineInstance>();
		if (!isConnected()) {
			connect();
		}
		final String[][] typeInfo = new String[][] { new String[] {
				"ManagedEntity", "name" }, };
		try {
			final ObjectContent[] ocData = cb.getServiceUtil()
					.getContentsRecursively(null, null, typeInfo, true);
			ObjectContent oc = null;
			ManagedObjectReference mor = null;
			DynamicProperty[] pData = null;
			DynamicProperty pc = null;
			if (ocData == null) {
				return null;
			}

			for (final ObjectContent element : ocData) {
				oc = element;
				mor = oc.getObj();
				pData = oc.getPropSet();

				if (!mor.getType().equalsIgnoreCase("VirtualMachine")) {
					continue;
				}

				for (final DynamicProperty element2 : pData) {
					if (element2 == null) {
						continue;
					}
					pc = element2;
					if (!mor.getType().equalsIgnoreCase("VirtualMachine")) {
						continue;
					}

					final VirtualMachineInstance vm = new VirtualMachineInstance();
					vm.setId(mor.get_value());

					if (!pc.getVal().getClass().isArray()) {
						vm.setName(pc.getVal().toString());
						res.add(vm);
					}
				}
			}
		} catch (final Exception e) {
			logout();
			throw new MSIMonitorException("failed to get guest domain list. ["
					+ e.getMessage() + "]");
		}
		logout();
		return res;
	}

	@Override
	public boolean isConnected() {
		return cb.getConnection().isConnected();
	}

	@Override
	public boolean logout() {
		try {
			cb.getConnection().disconnect();
		} catch (final Exception e) {
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.msi.ec2.monitor.connector.HypervisorConnector#setGatherer(com.msi
	 * .ec2.monitor.connector.collect.MonitorGatherer)
	 */
	@Override
	public void setGatherer(final MonitorGatherer gatherer) {
		this.gatherer = gatherer;
	}

	private OptionSpec[] setupOptions() {
		// String port = (options.containsKey("port") && options.get("port") !=
		// null) ? options
		// .get("port") : "443";
		final String ignoreCert = options.containsKey("ignoreCert")
				&& options.get("ignoreCert") != null ? options
				.get("ignoreCert") : "true";
		final OptionSpec[] spec = {
				new OptionSpec(
						"url",
						"String",
						1,
						"Required. Complete URL for the VI API Web service to connect to",
						buildConnectionURI()),
				new OptionSpec(
						"username",
						"String",
						1,
						"Required. User account with privileges to connect to the host",
						username),
				new OptionSpec("password", "String", 1,
						"Required. Password for the user account", password),
				// new OptionSpec("portNumber", "String", 0,
				// "Port to connect to server", port),
				new OptionSpec("ignorecert", "String", 0,
						"Ignore the server certificate validation", ignoreCert), };
		return spec;
	}

}
