package com.msi.tough.monitor;

/**
 * This class represents a  hypervisors monitor (one hypervisor per
 * thread).
 *
 * @author mduong
 */

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import com.msi.tough.core.HibernateUtil;
import com.msi.tough.model.AccountBean;
import com.msi.tough.monitor.common.manager.MeasureHandler;
import com.msi.tough.monitor.common.model.exception.MSIMonitorException;

public class AutoScaleMonitor implements Runnable {
	Thread runner;
	MeasureHandler mHandler;
	long gather_sleep;

	public AutoScaleMonitor(final MeasureHandler mHandler, final long sleep)
			throws MSIMonitorException {
		runner = new Thread(this);
		this.mHandler = mHandler;
		gather_sleep = sleep;
		runner.start();
	}

	@Override
	public void run() {
		while (runner != null) {
			try {
				final Session session = HibernateUtil.newSession();
				session.beginTransaction();

				final String hql = "from com.msi.tough.model.AccountBean";
				final Query query = session.createQuery(hql);

				@SuppressWarnings("unchecked")
                final List<AccountBean> accounts = query.list();

				for (final AccountBean acc : accounts) {
					final Monitor mon = new Monitor(null, null);
					mon.setMeasureHandler(mHandler);
					mon.gatherAS(acc.getAccessKey(), acc.getSecretKey());
				}
				Thread.sleep(gather_sleep);
				session.getTransaction().commit();
				try {
					session.close();
				} catch (final Exception e) {
				}
			} catch (final InterruptedException ie) {
				runner = null;
			} catch (final MSIMonitorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Terminates this hypervisor monitor (interrupts the thread).
	 */
	public void terminate() {
		runner.interrupt();
	}

}