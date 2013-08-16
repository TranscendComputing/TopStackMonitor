package com.msi.tough.monitor;

import java.util.ArrayList;
import java.util.List;


/**
 * 
 * @author heathm
 *
 */
public class HypervisorConfigList {
	List<HypervisorConfig> configList = new ArrayList<HypervisorConfig>();
	
	public HypervisorConfigList() {
		
	}
	
	public HypervisorConfigList(List<HypervisorConfig> configList) {
		this.configList = configList;
	}

	public List<HypervisorConfig> getConfigList() {
		return configList;
	}

	public void setConfigList(List<HypervisorConfig> configList) {
		this.configList = configList;
	}
	
}
