package com.msi.tough.monitor.connector.platform.vmware;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.slf4j.Logger;

import com.msi.tough.core.Appctx;
import com.vmware.apputils.AppUtil;

public class AppUtilConnector {
	private static final Logger logger = Appctx
			.getLogger(AppUtilConnector.class.getName());
	private final AppUtil appUtil;

	public AppUtilConnector(final AppUtil util) {
		appUtil = util;
	}

	public AppUtil getAppUtil() {
		return appUtil;
	}

	public void set_option(final String key, final String value) {
		Field privateField;
		logger.debug("pre modify [" + appUtil.get_option(key) + "]");
		try {
			privateField = AppUtil.class.getDeclaredField("optsEntered");
			privateField.setAccessible(true);
			@SuppressWarnings({ "unused", "unchecked" })
            String put = ((HashMap<String, String>) privateField.get(appUtil)).put(key,
					value);
			logger.debug("Value changed to [" + value + "]");
		} catch (final SecurityException e) {
			e.printStackTrace();
		} catch (final NoSuchFieldException e) {
			e.printStackTrace();
		} catch (final IllegalArgumentException e) {
			e.printStackTrace();
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}
		logger.debug("post modify [" + appUtil.get_option(key) + "]");
	}

}
