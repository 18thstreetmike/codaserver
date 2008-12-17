/*
 * SessionTimerJob.java
 *
 * Created on November 15, 2007, 8:35 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 * @author michaelarace
 */
public class SessionTimerJob implements Job {
    
    /** Creates a new instance of ProcedureJob */
    public SessionTimerJob() {
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        CodaServer server = (CodaServer)dataMap.get("server");
        
        server.expireSessions();
      
    }
    
}
