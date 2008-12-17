/*
 * ProcedureJob.java
 *
 * Created on July 23, 2007, 1:58 AM
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

import java.util.Vector;
import java.util.logging.Level;

/**
 *
 * @author michaelarace
 */
public class ProcedureJob implements Job {
    
    /** Creates a new instance of ProcedureJob */
    public ProcedureJob() {
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        CodaServer server = (CodaServer)dataMap.get("server");
        String applicationName = (String)dataMap.get("applicationName");
        int environment = ((Integer)dataMap.get("environment")).intValue();
        String procedureName = (String)dataMap.get("procedureName");
        Vector<String> parameters = (Vector)dataMap.get("parameters");
        long userId = ((Long)dataMap.get("userId")).longValue();
        String cronName = (String)dataMap.get("cronName");
        
        String sessionKey = server.login(userId);
        if (sessionKey != null) {
          boolean success = server.setSessionApplication(sessionKey, applicationName, environment, null);
          if (success) {
              CodaResponse response = server.execProcedure(sessionKey, procedureName, parameters, null);
              if (response.getError()) {
                  server.log(Level.WARNING, "Cron " +cronName + " failed to run: Eror with procedure.");
              }
          } else {
              server.log(Level.WARNING, "Cron " +cronName + " failed to run: Unable to set application and environment.");
          }
          server.logout(sessionKey);
        } else {
            server.log(Level.WARNING, "Cron " +cronName + " failed to run: Invalid user account specified.");
        }
    }
    
}
