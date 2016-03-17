package com.idiominc.ws.integration.compassion.servlet.TranslatorPaymentReport;

import com.idiominc.ws.integration.compassion.reporting.ArchivedProject;
import com.idiominc.ws.integration.compassion.reporting.ArchivedProjects;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.component.servlet.WSHttpServlet;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.user.WSWorkgroup;
import org.apache.log4j.Logger;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.*;

/**
 * Handles interface between Javascript and Worldserver for the translator payment report.
 *
 * @author SDL Professional Services
 */
public class TranslatorPaymentCommands extends WSHttpServlet {


    private static final Logger log = Logger.getLogger(TranslatorPaymentCommands.class);
    private static final int _STATUS_OK = 200;
    private static final int _STATUS_ERROR = 400;

    public synchronized boolean handle(WSContext context, HttpServletRequest request, HttpServletResponse response) {

        try {

            // Get the command from the HTTP request
            String command = request.getParameter("command");

            // Send a valid HTTP response
            response.setContentType("application/json");
            response.setStatus(_STATUS_OK);

            Object result = null;

            /**
             * Parse commands from the HTTP request, assign them to the corresponding method
             */
            if ("getWorkgroups".equals(command)) {
                result = command_getWorkgroups(context);
            } else if("getDetailedReport".equals(command)) {
                result = command_getDetailedReport(context, request);
            } else if("getSummaryReport".equals(command)) {
                result = command_getSummaryReport(context, request);
            }

            // Return the result to Javascript
            JSONValue.writeJSONString(result, response.getWriter());

            // Always commit so always return true
            return true;

        } catch (Exception e) {

            log.error("API ERROR", e);

            response.setStatus(_STATUS_ERROR);

            try {
                JSONValue.writeJSONString((e.getMessage() != null ? e.getMessage() : "UNKNOWN"), response.getWriter());
            } catch (Exception e2) {
                log.error("API ERROR", e);
            }

            return false;
        }
    }


    /******************************************************************************************************************
     *
     *  command_ methods
     *
     ******************************************************************************************************************/

    /**
     * Returns a list of translators in alphabetical order in order to populate the select box
     *
     * @param context WS context
     * @return Returns vendor list
     */
    public Object command_getWorkgroups(WSContext context)  {

        List<Map<String, Object>> displayWorkgroups = new ArrayList<Map<String, Object>>();

        try {

            WSUser currentUser = context.getUser();

            if(currentUser != null) {
                WSWorkgroup[] workgroups = currentUser.getWorkgroups();

                if(workgroups != null) {

                    for(WSWorkgroup workgroup: workgroups) {
                        // Create an key/value pair array to pass back to be used for populating a select box
                        Map<String, Object> workgroupInfo = new HashMap<String, Object>();
                        workgroupInfo.put("id", workgroup.getName());
                        workgroupInfo.put("name", workgroup.getName());
                        displayWorkgroups.add(workgroupInfo);
                    }
                } else {
                    log.error("No workgroups found for current user.");
                }
            } else {
                log.error("Current user not found.");
            }
        } catch( Exception e ) {
            log.error("Could not get workgroups.", e);
        }

        return displayWorkgroups;

    }


    /**
     * Returns a list of translators in alphabetical order in order to populate the select box
     *
     * @param context WS context
     * @return Returns vendor list
     */
    public Object command_getDetailedReport(WSContext context, HttpServletRequest request)  {

        ArchivedProjects archivedProjects = new ArchivedProjects(context);

        // Get the values from the HTTP request
        long startDateVal = Long.parseLong(request.getParameter("startDate"));
        long endDateVal = Long.parseLong(request.getParameter("endDate"));
        String workgroupsString = request.getParameter("workgroups");

        // Convert csv of workgroups to array
        String[] workgroups = workgroupsString.split("\\s*,\\s*");

        // Turn date strings into dates
        Date createdAfter = new Date(startDateVal);
        Date createdBefore = new Date(endDateVal);

        archivedProjects.init(workgroups,createdBefore,createdAfter);

        // Create the array to return to Javascript for display
        ArrayList<ArrayList<String>> returnList = new ArrayList<ArrayList<String>>();

        // Create a list of projects from the SQL database
        List<ArchivedProject> projects = null;
        try {
            projects = archivedProjects.runQuery();
        } catch (SQLException e) {
            log.error("SQL error executing query for Detailed Report.", e);
            return returnList;
        }

        // Check if any projects returned
        if(projects != null) {

            // If so loop through them
            for (ArchivedProject project : projects) {

                // Create the row array for the report table
                ArrayList<String> projectList = new ArrayList<String>();

                // Add in the elements in the left to right order
                projectList.add(project.getStepAcceptedBy());
                projectList.add(project.getStepName());
                projectList.add(project.getTemplateId());
                projectList.add(project.getCommunicationId());
                projectList.add(project.getSourceLocale());
                projectList.add(project.getTargetLocale());
                projectList.add(project.getProjectStartDate());
                projectList.add(project.getStepAcceptedOn());
                projectList.add(project.getStepCompletedOn());
                projectList.add(project.getProjectCompletionDate());
                projectList.add(project.isWasEscalated());
                projectList.add(project.isRequiredRework());
                projectList.add(project.isWasReturnedToQueue());

                // Add the row to the full array
                returnList.add(projectList);
            }
        }

        // Return the full array to Javascript for display
        return returnList;
    }


    /**
     * Returns a list of translators in alphabetical order in order to populate the select box
     *
     * @param context WS context
     * @return Returns vendor list
     */
    public Object command_getSummaryReport(WSContext context, HttpServletRequest request)  {

        ArchivedProjects archivedProjects = new ArchivedProjects(context);

        // Get the values from the HTTP request
        long startDateVal = Long.parseLong(request.getParameter("startDate"));
        long endDateVal = Long.parseLong(request.getParameter("endDate"));
        String workgroupsString = request.getParameter("workgroups");

        // Convert csv of workgroups to array
        String[] workgroups = workgroupsString.split("\\s*,\\s*");

        // Turn date strings into dates
        Date createdAfter = new Date(startDateVal);
        Date createdBefore = new Date(endDateVal);

        archivedProjects.init(workgroups,createdBefore,createdAfter);

        // Create the full array containing the table to display in Javascript
        ArrayList<ArrayList<String>> returnList = new ArrayList<ArrayList<String>>();

        try {

            // Get a list of projects from the SQL database
            List<ArchivedProject> projects = archivedProjects.runQuery();

            // Check to see if any projects match query
            if(projects != null) {

                // If so then create a map to contain the counters keyed on the user
                // and step information required in the report
                Map<TPRSummaryUser,TPRSummaryResults> summaryMap = new HashMap<TPRSummaryUser,TPRSummaryResults>();

                // Loop through projects
                for (ArchivedProject project : projects) {

                    // Build the key for the map
                    TPRSummaryUser key = new TPRSummaryUser(
                            project.getStepAcceptedBy(),
                            project.getStepName(),
                            project.getTemplateId());

                    // If the key doesn't exist then create it
                    if(!summaryMap.containsKey(key)) summaryMap.put(key, new TPRSummaryResults());

                    // Get the reference to the current data mapped to the key
                    TPRSummaryResults currentResults = summaryMap.get(key);

                    // Increment the number of tasks completed for this key
                    currentResults.incTasksCompleted();

                    // Increment the escalation count for this key if needed
                    if(project.isWasEscalated().equals("Yes")) {
                        currentResults.incEscalationCount();
                    }

                    // Increment the rework count for this key if needed
                    if(project.isRequiredRework().equals("Yes")) {
                        currentResults.incReworkCount();
                    }

                    // Increment the returned to queue count for this key if needed
                    if(project.isWasReturnedToQueue().equals("Yes")) {
                        currentResults.incReturnedToQueueCount();
                    }
                }

                // Now that the map is built loop through it to build the report array
                for(Map.Entry<TPRSummaryUser,TPRSummaryResults> entry : summaryMap.entrySet()) {

                    // Create an array row for this key
                    ArrayList<String> projectList = new ArrayList<String>();

                    // Add the identifying information to the row
                    projectList.add(entry.getKey().getName());
                    projectList.add(entry.getKey().getStepName());
                    projectList.add(entry.getKey().getTemplate());

                    // Add the counter information to the row
                    projectList.add(String.valueOf(entry.getValue().getTasksCompleted()));
                    projectList.add(String.valueOf(entry.getValue().getEscalationCount()));
                    projectList.add(String.valueOf(entry.getValue().getReworkCount()));
                    projectList.add(String.valueOf(entry.getValue().getReturnedToQueueCount()));

                    // Add the row to the table
                    returnList.add(projectList);
                }
            }
        } catch (SQLException e) {
            log.error("SQL error executing query for Summary Report", e);
        }

        // Return the full array for display by Javascript
        return returnList;
    }


    /******************************************************************************************************************
     *
     *  WorldServer Configuration Methods
     *
     ******************************************************************************************************************/

    public String getName() {
        return "translator_payment_ajax_commands";
    }

    public String getDescription() {
        return "Translator Payment AJAX commands";
    }

    public String getVersion() {
        return "1.0";
    }

}
