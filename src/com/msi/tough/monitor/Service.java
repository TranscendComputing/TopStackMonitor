package com.msi.tough.monitor;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.msi.tough.core.Appctx;
import com.msi.tough.query.Action;

public class Service {
    private final static Logger logger = Appctx.getLogger(Service.class
            .getName());

    private final Map<String, Action> actionMap;

    public Service(final Map<String, Action> actionMap) {
        this.actionMap = actionMap;
    }

    public void process(final HttpServletRequest req,
            final HttpServletResponse resp) throws Exception {
        final String act = req.getParameter("Action");
        logger.debug("Action received " + act);
        final Action a = actionMap.get(act);
        logger.debug("Action object " + a);
        if (a == null) {
            logger.warn("No configured action for: " + act);
        }
        a.process(req, resp);
    }
}
