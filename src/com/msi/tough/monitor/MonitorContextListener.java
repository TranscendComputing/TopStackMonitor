/**
 *
 */
package com.msi.tough.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import com.msi.tough.core.Appctx;
import com.msi.tough.servlet.StandardContextListener;
//import com.msi.tough.monitor.hedwig.ServiceSubscriber;

/**
 * @author tdhite
 */
public class MonitorContextListener extends StandardContextListener {

	private ServletContext servletContext;
	private static long gather_sleep;
	static {
		gather_sleep = Long.parseLong((String) Appctx
				.getConfigurationBean("GatherSleepTime"));
	}

	private final List<HypervisorMonitor> monitors = new ArrayList<HypervisorMonitor>();
	//private final List<ServiceSubscriber> sublist = new ArrayList<ServiceSubscriber>();

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.
	 * ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(final ServletContextEvent event) {
	    super.contextDestroyed(event);
		// stopThreads();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * javax.servlet.ServletContextListener#contextInitialized(javax.servlet
	 * .ServletContextEvent)
	 */
	@Override
	public void contextInitialized(final ServletContextEvent event) {
	    super.contextInitialized(event);
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		servletContext = event.getServletContext();
		// try {
		servletContext.setAttribute("monitor.hypervisormonitor.list", monitors);
		servletContext.setAttribute("monitor.hypervisormonitor.sleep",
				gather_sleep);
		// startThreads();
		// } catch (final ParseException e) {
		// logger.error(e.getMessage());
		// e.printStackTrace();
		// logger.error("A general parse exception occurred at startup - Monitor may not work correctly!");
		// }
	}

	//
	// private void startThreads() throws ParseException {
	// final ArrayList<HypervisorConnector> connectors = new
	// ArrayList<HypervisorConnector>();
	//
	// final Session session = HibernateUtil.newSession();
	// session.beginTransaction();
	//
	// final String hql =
	// "from com.msi.tough.model.monitor.HypervisorConfigBean where enable='Y'";
	// final Query query = session.createQuery(hql);
	//
	// session.getTransaction().commit();
	// final List<HypervisorConfigBean> hvlist = query.list();
	// try {
	// session.close();
	// } catch (final Exception e) {
	// }
	//
	// for (final HypervisorConfigBean config : hvlist) {
	// final HypervisorConnector conn = HypervisorConnectionFactory
	// .getConnector(config);
	// connectors.add(conn);
	// }
	//
	// final MeasureHandler mHandler = Appctx.getBean("measurehandler");
	// for (final HypervisorConnector conn : connectors) {
	// HypervisorMonitor mon = null;
	// try {
	// mon = new HypervisorMonitor(mHandler, conn, gather_sleep);
	// } catch (final MSIMonitorException e) {
	// logger.error(e.getMessage());
	// e.printStackTrace();
	// logger.error("A general MSIMonitorException occurred at startup - Monitor may not work correctly!");
	// } catch (final Exception e) {
	// logger.error(e.getMessage());
	// e.printStackTrace();
	// logger.error("A general exception occurred at startup - Monitor may not work correctly!");
	// } finally {
	// if (mon == null) {
	// logger.error("Monitor failed to start for mHandler: "
	// + mHandler.toString());
	// } else {
	// monitors.add(mon);
	// }
	// }
	// }
	//
	// /*
	// * ServiceSubscriber asSub = new ServiceSubscriber(Constants.AS_TOPIC);
	// * try { asSub.subscribe(new ASMetricHandler()); } catch
	// * (CouldNotConnectException e) { logger.log(Level.SEVERE,
	// * e.getMessage()); e.printStackTrace(); logger.log(Level.SEVERE,
	// *
	// "A CouldNotConnectException occurred at startup - subscription may not work correctly!"
	// * ); } catch (ClientAlreadySubscribedException e) {
	// * logger.log(Level.SEVERE, e.getMessage()); e.printStackTrace();
	// * logger.log(Level.SEVERE,
	// *
	// "A ClientAlreadySubscribedException occurred at startup - subscription may not work correctly!"
	// * ); } catch (ServiceDownException e) { logger.log(Level.SEVERE,
	// * e.getMessage()); e.printStackTrace(); logger.log(Level.SEVERE,
	// *
	// "A ServiceDownException occurred at startup - subscription may not work correctly!"
	// * ); } catch (UnsupportedEncodingException e) {
	// * logger.log(Level.SEVERE, e.getMessage()); e.printStackTrace();
	// * logger.log(Level.SEVERE,
	// *
	// "A UnsupportedEncodingException occurred at startup - subscription may not work correctly!"
	// * ); } catch (ClientNotSubscribedException e) {
	// * logger.log(Level.SEVERE, e.getMessage()); e.printStackTrace();
	// * logger.log(Level.SEVERE,
	// *
	// "A ClientNotSubscribedException occurred at startup - subscription may not work correctly!"
	// * ); } catch (InvalidSubscriberIdException e) {
	// * logger.log(Level.SEVERE, e.getMessage()); e.printStackTrace();
	// * logger.log(Level.SEVERE,
	// *
	// "A InvalidSubscriberIdException occurred at startup - subscription may not work correctly!"
	// * ); } sublist.add(asSub);
	// */
	// }
	//
	// private void stopThreads() {
	// if (monitors != null) {
	// for (final HypervisorMonitor mon : monitors) {
	// mon.terminate();
	// }
	// }
	/*
	 * if(this.sublist != null){ for(ServiceSubscriber sub : this.sublist){ try
	 * { sub.terminate(); } catch (ClientNotSubscribedException e) {
	 * logger.log(Level.SEVERE, e.getMessage()); e.printStackTrace();
	 * logger.log(Level.SEVERE,
	 * "A ClientNotSubscribedException occurred at startup - subscription may not work correctly!"
	 * ); } catch (InvalidSubscriberIdException e) { logger.log(Level.SEVERE,
	 * e.getMessage()); e.printStackTrace(); logger.log(Level.SEVERE,
	 * "A InvalidSubscriberIdException occurred at startup - subscription may not work correctly!"
	 * ); } catch (CouldNotConnectException e) { logger.log(Level.SEVERE,
	 * e.getMessage()); e.printStackTrace(); logger.log(Level.SEVERE,
	 * "A CouldNotConnectException occurred at startup - subscription may not work correctly!"
	 * ); } catch (ServiceDownException e) { logger.log(Level.SEVERE,
	 * e.getMessage()); e.printStackTrace(); logger.log(Level.SEVERE,
	 * "A ServiceDownException occurred at startup - subscription may not work correctly!"
	 * ); } } }
	 */
	// }
}
