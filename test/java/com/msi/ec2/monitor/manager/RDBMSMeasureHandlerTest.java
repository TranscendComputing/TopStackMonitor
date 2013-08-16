package com.msi.ec2.monitor.manager;

import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import com.msi.tough.core.HibernateUtil;
import com.msi.tough.model.monitor.MeasureBean;
import com.msi.tough.monitor.common.MonitorConstants;
import com.msi.tough.monitor.common.manager.RDBMSMeasureHandler;

@Ignore("Transaction boundary isn't working.")
public class RDBMSMeasureHandlerTest
{

    @Test
    @Transactional
    public void testStore() throws Exception
    {
        Session s = HibernateUtil.getSession();
        // store Measure
        MeasureBean measure = new MeasureBean();
        //measure.setInstanceId("instance1");
        measure.setName(MonitorConstants.CPU_UTILIZATION_COMMAND);
        measure.setNamespace("Test/EC2");
        measure.setTimestamp(new Date());
        measure.setUnit(MonitorConstants.PERCENT_UNIT);
        measure.setValue(0.34567);
        RDBMSMeasureHandler cass = new RDBMSMeasureHandler();
        cass.store(s, measure);

        Session session = HibernateUtil.getSession();
        session.beginTransaction();
        try
        {
            Criteria criteria =
                session
                    .createCriteria(MeasureBean.class)
                    //.add(Restrictions.eq("instanceId", measure.getInstanceId()))
                    .add(Restrictions.eq("name", measure.getName()))
                    .add(Restrictions.eq("namespace", measure.getNamespace()))
                    .add(Restrictions.eq("timestamp", measure.getTimestamp()))
                    .add(Restrictions.eq("value", measure.getValue()))
                    .add(Restrictions.eq("unit", measure.getUnit()));

            @SuppressWarnings("unchecked")
            List<MeasureBean> check = criteria.list();
            assertTrue(check.size() > 0);
        }
        finally
        {
            session.close();
        }

    }

}
