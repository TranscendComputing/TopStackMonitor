package com.msi.tough.monitor.connector.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.common.util.StringHelper;
import org.libvirt.Connect;
import org.libvirt.ConnectAuth;
import org.libvirt.ConnectAuthDefault;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.slf4j.Logger;

import com.msi.tough.core.Appctx;
import com.msi.tough.monitor.common.model.VirtualMachineInstance;
import com.msi.tough.monitor.common.model.exception.MSIMonitorException;
import com.msi.tough.monitor.connector.HypervisorConnector;
import com.msi.tough.monitor.connector.collect.MonitorGatherer;

/**
 * The Hypervisor Connector for any libvirt style connection.
 *
 * @author heathm
 */
public class VirtConnector implements HypervisorConnector {

    private static int connectFlags = 3;

	public static int getConnectFlags() {
        return connectFlags;
    }

    public static Class<? extends HypervisorConnector> setConnectFlags(int connectFlags) {
        VirtConnector.connectFlags = connectFlags;
        return VirtConnector.class;
    }

    /**
	 * ConnectionPassAuth is used as the callback object for authentication with
	 * the libvirt connection.
	 *
	 * @author heathm
	 */
	public class ConnectionPassAuth extends ConnectAuth {

		public ConnectionPassAuth() {
			// Libvirt uses this to determine the types of potential
			// authentication.
			credType = new CredentialType[] { CredentialType.VIR_CRED_USERNAME,
					CredentialType.VIR_CRED_ECHOPROMPT,
					CredentialType.VIR_CRED_REALM,
					CredentialType.VIR_CRED_PASSPHRASE,
					CredentialType.VIR_CRED_NOECHOPROMPT };
		}

		/**
		 * This is the callback in the libvirt that handles challenges for
		 * authentication. The Credential[] needs populating for handling of
		 * authentication in the C library that backs libvirt (JNA).
		 */
		@Override
		public int callback(final Credential[] cred) {
			logger.debug("ConnectAuth callback invoked with " + cred);
			try {
				for (final Credential c : cred) {
					String response = "";
					logger.info("Got here!");
					switch (c.type) {
					case VIR_CRED_USERNAME:
						logger.info("grabbed Username!");
						response = username;
						break;
					case VIR_CRED_AUTHNAME:
					case VIR_CRED_ECHOPROMPT:
					case VIR_CRED_REALM:
					case VIR_CRED_PASSPHRASE:
						logger.info("grabbed password!");
						response = password;
						System.out.println(response);
						break;
					case VIR_CRED_NOECHOPROMPT:
                    default:
					}
					if (response.equals("") && !c.defresult.equals("")) {
						c.result = c.defresult;
					} else {
						c.result = response;
					}
					if (c.result.equals("")) {
						return -1;
					}
				}
			} catch (final Exception e) {
				return -1;
			}
			return 0;
		}

	}

	// supported xen protocals
	public enum Protocals {
		LOCAL, TLS, SSH, TCP, UNIX, OTHER;
		public String getURIBase(final String driverName, final Protocals p) {
			switch (p) {
			case LOCAL:
			case TLS:
				return driverName;
			case TCP:
				return driverName + "+tcp";
			case UNIX:
				return driverName + "+unix";
			case SSH:
				return driverName + "+ssh";
			case OTHER:
				if ("vpx".equalsIgnoreCase(driverName)
						|| "esx".equalsIgnoreCase(driverName)
						|| "gsx".equalsIgnoreCase(driverName)) {
					return driverName;
				}
			default:
				return "";
			}
		}
	}

	private static final Logger logger = Appctx.getLogger(VirtConnector.class
			.getName());

	private String host;
	private String username;
	private final String password;
	private Map<String, String> options = new HashMap<String, String>();
	private Connect conn;
	private MonitorGatherer gatherer;;
	private String driver = "xen";

	private Protocals proto = Protocals.LOCAL;

	// constructor
	public VirtConnector(final String driver, final String proto,
			final String username, final String password, final String host,
			final Map<String, String> options) {
		this.driver = driver;
		this.username = username;
		this.password = password;
		this.host = host;
		this.options = options;
		useMethod(proto.toUpperCase());
	}

	/**
	 * Builds the connection URI for libvirt.
	 *
	 * @return
	 */
	public String buildConnectionURI() {
		final String connectType = proto.getURIBase(driver, proto);
		String separator = "";
		if (StringHelper.isEmpty(host)) {
			host = "127.0.0.1";
		}
		if (StringHelper.isEmpty(username)) {
			username = "";
		} else {
			separator = "@";
		}

		final StringBuilder connURI = new StringBuilder(connectType + "://"
				+ username + separator + host + "/");
		if ("qemu".equalsIgnoreCase(driver)) {
			connURI.append("system");
		}
		boolean firstPass = true;
		for (final Map.Entry<String, String> e : options.entrySet()) {
			if (firstPass) {
				connURI.append("?");
				firstPass = false;
			}
			connURI.append(e.getKey() + "=" + e.getValue() + "&");
		}
		if (!firstPass) {
			connURI.deleteCharAt(connURI.length() - 1);
		}
		logger.info("virt connection URI is [" + connURI.toString() + "]");
		return connURI.toString();
	}

	/**
	 * Connect to the lib virt based hypervisor.
	 */
	@Override
	public boolean connect() {
		ConnectAuth cAuth;
		if (StringHelper.isEmpty(password)) {
			// Connect without auth
			cAuth = new ConnectAuthDefault();
			logger.debug("Using ConnectAuthDefault()");
		} else {
			// need a specific callback ConnectAuth implemented for this.
			cAuth = new ConnectionPassAuth();
			logger.debug("Using ConnectionPassAuth()");
		}
		try {
			final String uri = buildConnectionURI();
			conn = new Connect(uri, cAuth, connectFlags);
			return conn.isConnected();
		} catch (final LibvirtException lve) {
			lve.printStackTrace();
		}
		return false;
	}

	/**
	 * returns a libvirt Connect object.
	 *
	 * @return
	 */
	@Override
	public Object getConnection() throws MSIMonitorException {
		if (conn == null) {
			connect();
		}
		return conn;
	}

	/**
	 * Retrieve the Domain ID for the given domain Object. If the id isn't
	 * populated then it will look it up by name.
	 */
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
		Domain dom = null;
		try {
			if (domain.getUuid() != null) {
				dom = conn.domainLookupByUUID(domain.getUuid());
			}
			if (dom == null && domain.getName() != null) {
				dom = conn.domainLookupByName(domain.getName());
			}
			if (dom != null) {
				domain.setId(String.valueOf(dom.getID()));
				return domain.getId();
			}
		} catch (final LibvirtException le) {
			throw new MSIMonitorException(le);
		}
		logger.warn("A domain [" + domain.getUuid() + ":" + domain.getId()
				+ ":" + domain.getName() + "] was unavailable for lookup.");
		return null;
	}

	/**
	 * get the Monitor statistics gatherer for this type of connection.
	 *
	 * @return
	 */
	@Override
	public MonitorGatherer getGatherer() {
		return gatherer;
	}

	/**
	 * List all the guest domains for this Xen instance.
	 */
	@Override
	public List<VirtualMachineInstance> getGuestDomains()
			throws MSIMonitorException {
		final List<VirtualMachineInstance> domains = new ArrayList<VirtualMachineInstance>();
		final Connect conn = (Connect) getConnection();

		try {
			final int[] doms = conn.listDomains();
			for (final int dom : doms) {
				if (dom > 0) {
					final VirtualMachineInstance vmi = new VirtualMachineInstance();
					vmi.setId(String.valueOf(dom));
					final Domain domainObj = conn.domainLookupByID(dom);
					vmi.setName(domainObj.getName());
					domains.add(vmi);
				}
			}
		} catch (final LibvirtException e) {
			throw new MSIMonitorException(e.getMessage(), e);
		}

		return domains;
	}

	/**
	 * Test to see if we have a connection to the hypervisor.
	 */
	@Override
	public boolean isConnected() {
		if (conn == null) {
			connect();
		}
		try {
			return conn.isConnected();
		} catch (final LibvirtException le) {
			return false;
		}
	}

	/**
	 * Check to see if an individual instance is active
	 *
	 * @param vm
	 * @return
	 * @throws MSIMonitorException
	 */
	public boolean isRunning(final VirtualMachineInstance vm)
			throws MSIMonitorException {
		if (vm == null) {
			return false;
		}
		boolean active = false;

		try {
			final int domId = Integer.parseInt(getDomainId(vm));
			final Domain dom = conn.domainLookupByID(domId);
			active = dom.isActive() == 1 ? true : false;
		} catch (final LibvirtException le) {
			throw new MSIMonitorException("Error checking instance state: "
					+ le.getMessage(), le);
		} catch (final NumberFormatException nfe) {
			// ignore that invalid identifiers are running if we get them.
			active = false;
		}
		return active;
	}

	@Override
	public boolean logout() {
	    try {
	        if (conn != null) {
	            conn.close();
	        }
            conn = null;
        } catch (LibvirtException e) {
            logger.warn("Exception closing libvirt connection.", e);
            return false;
        }
		return true;
	}

	/**
	 * setter injected method of gathering information from the target
	 * hypervisor.
	 *
	 * @param gatherer
	 */
	@Override
	public void setGatherer(final MonitorGatherer gatherer) {
		this.gatherer = gatherer;
	}

	/**
	 * Set the connection protocol. By default this is direct (ran on localhost
	 * of Dom0). However, the options are then enumerated type Protocols. LOCAL
	 * : local connection UNIX : same host via daemon TLS : TLS/x509
	 * Certificates required SSH : SSH Tunnel TCP : SASL/Kerberos
	 *
	 * @param connectProtocal
	 */
	public void useMethod(final String connectProtocal) {
		proto = Protocals.valueOf(connectProtocal);
		if (proto == null) {
			proto = Protocals.LOCAL;
		}
	}
}
