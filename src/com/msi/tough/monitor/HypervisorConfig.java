/**
 *
 */
package com.msi.tough.monitor;

import java.util.HashMap;
import java.util.Map;

/**
 * This is all the necessary information to connect to a hypervisor.
 *
 * @author heathm
 *
 */
public class HypervisorConfig {
	private String username;
	private String type;
	private String proto;
	private String password;
	private String host;
	private Map<String, String> options = new HashMap<String, String>();

	public Map<String, String> getOptions() {
		return options;
	}

	public void setOptions(Map<String, String> options) {
		this.options = options;
	}

	public String getOption(String name) {
		return this.options.get(name);
	}

	public String getHost() {
		return host;
	}

	public String getPassword() {
		return password;
	}

	public String getProto() {
		return proto;
	}

	public String getType() {
		return type;
	}

	public String getUsername() {
		return username;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setProto(String proto) {
		this.proto = proto;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}
