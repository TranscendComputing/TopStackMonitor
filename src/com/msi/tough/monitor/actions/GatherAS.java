package com.msi.tough.monitor.actions;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.slf4j.Logger;

import com.msi.tough.core.Appctx;
import com.msi.tough.model.ASGroupBean;
import com.msi.tough.model.monitor.DimensionBean;
import com.msi.tough.model.monitor.MeasureBean;
import com.msi.tough.monitor.common.manager.MeasureHandler;
import com.msi.tough.query.UnsecuredAction;
import com.msi.tough.utils.CWUtil;

public class GatherAS extends UnsecuredAction {
    private static Logger logger = Appctx.getLogger(GatherAS.class.getName());

    /**
     * Default constructor.
     */
    public GatherAS() {
        super();
        setLessVerbose(true); // Avoid lots of logging; this is called on timer.
    }

    @SuppressWarnings({ "unchecked" })
    private void gather(final Session s) throws Exception {
        final MeasureHandler mHandler = Appctx.getBean("measurehandler");
        final List<ASGroupBean> list = s.createQuery("from ASGroupBean").list();
        for (final ASGroupBean asg : list) {
            final long acid = asg.getUserId();
            if (logger.isDebugEnabled() && !isLessVerbose()) {
                    logger.debug("Gather AC " + acid + " Cluster " + asg.getName());
            }

            final DimensionBean dim = CWUtil.getDimensionBean(s, acid,
                    "AutoScalingGroupName", asg.getName(), true);
            final Set<DimensionBean> dims = new HashSet<DimensionBean>();
            dims.add(dim);
            for (final String k : new String[] { "GroupMinSize",
                    "GroupMaxSize", "GroupDesiredCapacity",
                    /*
                     * "GroupInServiceInstances", "GroupPendingInstances",
                     * "GroupTerminatingInstances",
                     */"GroupTotalInstances" }) {
                final MeasureBean measure = new MeasureBean();
                measure.setDimensions(dims);
                measure.setName(k);
                measure.setNamespace("AWS/AutoScaling");
                measure.setTimestamp(new Date());
                measure.setUnit("Count");
                if (k.equals("GroupMinSize")) {
                    measure.setValue((double) asg.getMinSz());
                }
                if (k.equals("GroupMaxSize")) {
                    measure.setValue((double) asg.getMaxSz());
                }
                if (k.equals("GroupDesiredCapacity")) {
                    measure.setValue((double) asg.getCapacity());
                }
                if (k.equals("GroupTotalInstances")) {
                    List<?> instances = asg.getScaledInstances(s);
                    //TODO: should this look at status of instances, only count healthy?
                    measure.setValue((double) instances.size());
                }
                mHandler.store(s, measure);
            }
        }
    }

    @Override
    public String process0(final Session s, final HttpServletRequest req,
            final HttpServletResponse resp, final Map<String, String[]> map)
            throws Exception {
        gather(s);
        return "DONE";
    }
}
