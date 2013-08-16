package com.msi.tough.monitor.actions;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.msi.tough.core.Appctx;
import com.msi.tough.core.CommaObject;
import com.msi.tough.core.DateHelper;
import com.msi.tough.dasein.DaseinHelper;
import com.msi.tough.engine.aws.Arn;
import com.msi.tough.model.ASGroupBean;
import com.msi.tough.model.AccountBean;
import com.msi.tough.model.monitor.AlarmBean;
import com.msi.tough.model.monitor.DimensionBean;
import com.msi.tough.monitor.common.MonitorConstants;
import com.msi.tough.monitor.common.model.helper.AlarmModelHelper;
import com.msi.tough.monitor.common.model.helper.DimensionHelper;
import com.msi.tough.query.UnsecuredAction;
import com.msi.tough.utils.ASUtil;
import com.msi.tough.utils.AccountUtil;
import com.msi.tough.utils.Constants;

public class CheckAlarms extends UnsecuredAction {

    private DimensionHelper dimensionHelper = null;

    /**
     * Default constructor.
     */
    public CheckAlarms() {
        super();
        setLessVerbose(true); // Avoid lots of logging; this is called on timer.
        // Use session from context, rather than forcing a new one.
        setContextSession(true);
        setManagedTx(true);
    }

    @Override
    @Transactional
    public void process(final HttpServletRequest req,
            final HttpServletResponse resp) throws Exception {
        super.process(req, resp);
    }

    @Resource
    public void setDimensionHelper(DimensionHelper dimensionHelper) {
        this.dimensionHelper = dimensionHelper;
    }

	private static class Stats {
		private Integer count;
		private Double sum;
		private Double min;
		private Double max;

		public Stats(final Integer count, final Double sum, final Double min,
				final Double max) {
			this.count = count;
			this.min = min;
			this.sum = sum;
			this.max = max;
		}

		public Integer getCount() {
			return count;
		}

		public Double getMax() {
			return max;
		}

		public Double getMin() {
			return min;
		}

		public Double getSum() {
			return sum;
		}

		public void setCount(final Integer count) {
			this.count = count;
		}

		public void setMax(final Double max) {
			this.max = max;
		}

		public void setMin(final Double min) {
			this.min = min;
		}

		public void setSum(final Double sum) {
			this.sum = sum;
		}
	}

	private static Logger logger = Appctx
			.getLogger(CheckAlarms.class.getName());

	private void accumulateStats(final Stats totals, final Stats current) {
		totals.setCount(totals.getCount() + current.getCount());
		totals.setSum(totals.getSum() + current.getSum());
		if (totals.getMin() == null) {
			totals.setMin(current.getMin());
		} else {
			if (current.getMin() != null && current.getMin() < totals.getMin()) {
				totals.setMin(current.getMin());
			}
		}
		if (totals.getMax() == null) {
			totals.setMax(current.getMax());
		} else {
			if (current.getMax() != null && current.getMax() > totals.getMax()) {
				totals.setMax(current.getMax());
			}
		}
	}

	private void autoScalingAlarm(final Session session, final AlarmBean alarm)
	        throws Exception {
		logger.debug("ASAlarm " + alarm.getAlarmName());
		final Set<DimensionBean> dims = alarm.getDimensions();
		final DimensionBean dim = dims.iterator().next();
		final String grp = dim.getValue();
		final ASGroupBean asg = ASUtil.readASGroup(session, alarm.getUserId(),
				grp);

		Stats totals = null;
		if (alarm.getMetricName().equals("Latency")) {
			final String lb = asg.getLoadBalancers();

			final DimensionBean idim = dimensionHelper.getDimensionBean(
					"LoadBalancer", lb, false);
			final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
			dimensions.add(idim);
			totals = getMeasures(session, alarm.getMetricName(),
					alarm.getNamespace(), (long) (alarm.getEvaluationPeriods()
							.doubleValue() * alarm.getPeriod().doubleValue()),
					dimensions);
		} else {
			final String insts = asg.getInstances();
			final CommaObject instl = new CommaObject(insts);

			for (final String inst : instl.toList()) {
				final DimensionBean idim = dimensionHelper.getDimensionBean(
						"InstanceId", inst, false);
				final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
				dimensions.add(idim);
				final Stats current = getMeasures(session, alarm.getMetricName(),
						alarm.getNamespace(), (long) (alarm
								.getEvaluationPeriods().doubleValue() * alarm
								.getPeriod().doubleValue()), dimensions);
				if (totals == null) {
					totals = current;
				} else {
					accumulateStats(totals, current);
				}
			}
		}
		if (totals != null) {
			checkAlarm(session, alarm, totals, true);
		}
	}

	private void checkAlarm(final Session session, final AlarmBean alarm,
			final Stats data, final boolean autoScaling) {

		if (data.getCount() <= 0) {
			return;
		}
		final String beforeState = alarm.getState() != null ? alarm.getState()
				: Constants.STATE_OK;
		final String stats = alarm.getStatistic();
		final String operator = alarm.getComparator();
		final double threshold = alarm.getThreshold();
		double newval = 0;
		if (stats.equalsIgnoreCase("Sum")) {
			newval = data.getSum();
		}
		if (stats.equalsIgnoreCase("Maximum")) {
			newval = data.getMax() == null ? 0 : data.getMax();
		}
		if (stats.equalsIgnoreCase("Minimum")) {
			newval = data.getMin() == null ? 0 : data.getMin();
		}
		if (stats.equalsIgnoreCase("Average")) {
			newval = data.getSum() / data.getCount();
		}
		boolean alarmSet = false;

		if (Constants.COMPARE_GREATER.equalsIgnoreCase(operator)
				&& newval > threshold) {
			alarmSet = true;
		} else if (Constants.COMPARE_GREATER_OR_EQUAL
				.equalsIgnoreCase(operator) && newval >= threshold) {
			alarmSet = true;
		} else if (Constants.COMPARE_LESS.equalsIgnoreCase(operator)
				&& newval < threshold) {
			alarmSet = true;
		} else if (Constants.COMPARE_LESS_OR_EQUAL.equalsIgnoreCase(operator)
				&& newval <= threshold) {
			alarmSet = true;
		}
		final String newState = alarmSet ? Constants.STATE_ALARM
				: Constants.STATE_OK;

		// check for kicking off actions.
		triggerRequiredActions(session, alarm, beforeState, newState,
				autoScaling);
	}

    public List<AlarmBean> getAlarms(Session session) {
        final List<AlarmBean> alarms = new ArrayList<AlarmBean>();
        @SuppressWarnings("unchecked")
        final List<AlarmBean> a = session.createQuery("from AlarmBean").list();
        if (a != null) {
            alarms.addAll(a);
        }
        return alarms;
    }

	private void checkAlarmThresholds(Session session, final long acid)
			throws Exception {
		logger.debug("in checkAlarmThresholds");

		for (final AlarmBean alarm : getAlarms(session)) {
			final Set<DimensionBean> dims = alarm.getDimensions();
			final DimensionBean dim = dims.iterator().next();
			if (dim.getKey().equals("AutoScalingGroup")) {
				autoScalingAlarm(session, alarm);
			} else {
				genericAlarm(session, alarm);
			}
		}
	}

	private void genericAlarm(final Session session, final AlarmBean alarm)
	    throws Exception {
		logger.debug("GenericAlarm " + alarm.getAlarmName());
		final Set<DimensionBean> dims = alarm.getDimensions();

		Stats totals = null;
		for (final DimensionBean dim : dims) {
		    final Set<DimensionBean> dimensions = new HashSet<DimensionBean>();
		    dimensions.add(dim);
		    final Stats current = getMeasures(session, alarm.getMetricName(),
		            alarm.getNamespace(), (long) (alarm
		                    .getEvaluationPeriods().doubleValue() * alarm
		                    .getPeriod().doubleValue()), dimensions);
		    if (totals == null) {
		        totals = current;
		    } else {
		        accumulateStats(totals, current);
		    }
		}
		if (totals != null) {
		    checkAlarm(session, alarm, totals, false);
		}
	}

	@SuppressWarnings("unchecked")
	private Stats getMeasures(final Session session, final String name,
			final String namespace, final long prd,
			final Set<DimensionBean> dimensions) {
		final String sel = "m.timestmp, m.unit, m.value";
		String from = "measures m";
		final CommaObject wh = new CommaObject();
		wh.setSeparator(" and ");
		wh.add("m.name='" + name + "'");
		wh.add("m.namespace='" + namespace + "'");
		final Date tm = new Date(System.currentTimeMillis() - (prd + 50) * 1000);
		wh.add("m.timestmp > '" + DateHelper.getSQLDate(tm) + "'");
		if (dimensions != null) {
			from = "measures m, measure_dimension md";
			final CommaObject whdim = new CommaObject();
			for (final DimensionBean dim : dimensions) {
				whdim.add("" + dim.getId());
			}
			wh.add("md.dimension_id in (" + whdim.toString() + ")");
			wh.add("md.measure_id=m.id");
		}
		final String sql = "select " + sel + " from " + from + " where "
				+ wh.toString() + " order by m.timestmp";

		logger.info("MeasureBean " + sql);

		final SQLQuery sq = session.createSQLQuery(sql);
		final List<Object[]> measures = sq.list();

		Integer cnt = 0;
		Double sum = new Double(0);
		Double min = null;
		Double max = null;

		for (final Object[] measure : measures) {
			int c = 0;
			@SuppressWarnings("unused")
            final Timestamp tms = (Timestamp) measure[c++];
			@SuppressWarnings("unused")
            final String unit = (String) measure[c++];
			final Double val = (Double) measure[c++];

			sum += val;
			cnt++;
			if (min == null || val < min) {
				min = val;
			}
			if (max == null || val > max) {
				max = val;
			}
		}
		return new Stats(cnt, sum, min, max);
	}

	@Override
	public String process0(final Session s, final HttpServletRequest req,
			final HttpServletResponse resp, final Map<String, String[]> map)
			throws Exception {
		checkAlarmThresholds(s, 0L);
		return "DONE";
	}

	private void triggerRequiredActions(final Session session,
			final AlarmBean alarm, final String beforeState,
			final String newState, final boolean autoScaling) {
	    /*
		 * For Auto Scaling policy notifications, the alarm continues to invoke
		 * the action for every period that the alarm remains in the new state.
		 * For Amazon SNS notifications, no additional actions are invoked.
		 */

		if (newState.equals(Constants.STATE_ALARM)) {
			final CommaObject cnm = new CommaObject(alarm.getActionNames());
			if (autoScaling) {
			    logger.info("autoScaling " + autoScaling + " new " + newState
			            + " before " + beforeState);
				boolean alarmSw = false;
				for (final String act : cnm.toList()) {
					alarmSw = ASUtil.executeASPolicy(session,
							alarm.getUserId(), act);
				}
				if (alarmSw) {
				    AlarmModelHelper.newAction(alarm);
				}
			}
			else if (!autoScaling && !newState.equals(beforeState)) {
                for (final String act : cnm.toList()) {
                    Arn arn = new Arn(act);
                    if (arn.isOfType(MonitorConstants.ARN_AUTOMATE)) {
                        automate(alarm, arn);
                    }
                }
                AlarmModelHelper.newAction(alarm);
            }
		}
		else if (newState.equals(Constants.STATE_OK)
				&& !newState.equals(beforeState)) {
		    logger.info("Trigger State OK Actions");
		    final CommaObject cnm = new CommaObject(alarm.getOkActions());
            for (final String act : cnm.toList()) {
                Arn arn = new Arn(act);
                if (arn.isOfType(MonitorConstants.ARN_AUTOMATE)) {
                    automate(alarm, arn);
                } else {
                    logger.warn("Unhandled action "+arn);
                }
                AlarmModelHelper.newAction(alarm);
            }
		}
		if (!autoScaling && !newState.equals(beforeState)) {
            AlarmModelHelper.newState(alarm, beforeState, newState);
		}
	}

	/**
	 * Take automated actions based on a triggered alarm.
	 * Current actions are stop, terminate.
	 *
	 * @param alarm
	 * @param arn
	 */
	private void automate(AlarmBean alarm, Arn arn) {
        final Set<DimensionBean> dims = alarm.getDimensions();

        for (final DimensionBean dim : dims) {
            if (dim.getKey().equals(DimensionBean.DIMENSION_INSTANCE_ID)) {
                AccountBean asAccount = AccountUtil.readAccount(getSession(),
                        alarm.getUserId());
                String instanceId = dim.getValue();
                if (arn.isOfType(MonitorConstants.ARN_STOP)) {
                    logger.info("Need to stop "+ instanceId);
                    stopInstance(instanceId, asAccount);
                } else if (arn.isOfType(MonitorConstants.ARN_TERMINATE)) {
                    logger.info("Need to terminate "+ instanceId);
                    terminateInstance(instanceId, asAccount);
                }
            }
        }
	}

    private void terminateInstance(String instanceId, AccountBean asAccount) {
        //TODO: replace with a call to client, once available.
        CloudProvider cloudProvider = null;
        try {
            cloudProvider = DaseinHelper.getProvider(
                    asAccount.getDefZone(), asAccount.getTenant(),
                    asAccount.getAccessKey(), asAccount.getSecretKey());
        } catch (Exception e) {
            logger.error("Error stopping instance " + instanceId, e);
        }
        final ComputeServices comp = cloudProvider.getComputeServices();
        final VirtualMachineSupport vmServ = comp.getVirtualMachineSupport();

        try {
            if (vmServ.getVirtualMachine(instanceId) == null) {
                logger.warn("Attempted to terminate instance " + instanceId +
                        ", but instance is not found.");
                return;
            }
            logger.info("Attempting to shut down instance " + instanceId);

            vmServ.terminate(instanceId);
        } catch (InternalException e) {
            logger.error("Error terminating instance " + instanceId, e);
        } catch (CloudException e) {
            logger.error("Error terminating instance " + instanceId, e);
        }
    }

    private void stopInstance(String instanceId, AccountBean asAccount) {
        //TODO: replace with a call to client, once available.
        CloudProvider cloudProvider = null;
        try {
            cloudProvider = DaseinHelper.getProvider(
                    asAccount.getDefZone(), asAccount.getTenant(),
                    asAccount.getAccessKey(), asAccount.getSecretKey());
        } catch (Exception e) {
            logger.error("Error stopping instance " + instanceId, e);
        }
        final ComputeServices comp = cloudProvider.getComputeServices();
        final VirtualMachineSupport vmServ = comp.getVirtualMachineSupport();

        try {
            if (vmServ.getVirtualMachine(instanceId) == null) {
                logger.warn("Attempted to stop instance " + instanceId +
                        ", but instance is not found.");
                return;
            }
            logger.info("Attempting to shut down instance " + instanceId);

            vmServ.stop(instanceId);
        } catch (InternalException e) {
            logger.error("Error stopping instance " + instanceId, e);
        } catch (CloudException e) {
            logger.error("Error stopping instance " + instanceId, e);
        }
    }
}
