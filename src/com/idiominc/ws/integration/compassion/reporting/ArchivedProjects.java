package com.idiominc.ws.integration.compassion.reporting;

import com.idiominc.ws.integration.profserv.commons.CollectionUtils;
import com.idiominc.wssdk.WSContext;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ArchivedProjects {

    private static final Logger log = Logger.getLogger(ArchivedProjects.class);

    // These 3 groups of attributes are only on the projects with the payment report
    // auto actions running
    protected final static String[] NEW_PROJECT_ATTRIBUTES = new String[] {
            "TranslationEscalated",
            "ReturnedToQueue",
            "TranslationSubmittedForRework"
    };

    protected final static String[] TRANSLATE_STEP_ATTRIBUTES = new String[] {
            "TranslateStepAcceptedBy",
            "TranslateStepAcceptedOn",
            "TranslateStepCompletedBy",
            "TranslateStepCompletedOn",
    };

    protected final static String[] QC_STEP_ATTRIBUTES = new String[] {
            "QCStepAcceptedBy",
            "QCStepAcceptedOn",
            "QCStepCompletedBy",
            "QCStepCompletedOn"
    };

    // These attributes have existed on all CI projects, they need to be separated for the
    // SQL query to work properly
    protected final static String[] CI_PROJECT_ATTRIBUTES = new String[] {
            "CompassionSBCId",
            "TemplateId",
            "OriginalLanguage",
            "TranslationLanguage",
            "TwoStepProcess"
    };

    //result of the DB query
    private List<ArchivedProject> archivedProjects = new ArrayList<ArchivedProject>();

    // WorldServer context to access SDK objects, interfaces and methods
    private WSContext wsContext;

    // Input parameters
    private String[] workgroupNames; // select only such projects that belong to these workgroups

    private Date projectsCompletedAfter; // select only such projects that were completed after this date
    private Date projectsCompletedBefore; // select only such projects that were completed before this date

    public ArchivedProjects(WSContext wsContext) {
        this.wsContext = wsContext;
    }


    /**
     * Initialize the query parameters for this list of projects
     *
     * @param workgroupNames - Array of workgroup names from UI
     * @param createdBefore - Last date (inclusive) to consider
     * @param createdAfter - First date (inclusive) to consider
     */
    public void init(String[] workgroupNames,
                     Date createdBefore,
                     Date createdAfter) {

        // comma-separated list of workgroups
        this.workgroupNames = workgroupNames;

        // project filter end date
        this.projectsCompletedBefore = createdBefore;

        // project filter start date
        this.projectsCompletedAfter  = createdAfter;
    }


    /**
     * Construct and execute the SQL query of the WS database
     *
     * @return A list of projects
     * @throws SQLException
     */
    public List<ArchivedProject> runQuery() throws SQLException {

        /**
         * Construct a list of all of the custom project attributes assigned to the project in
         * order to construct the SQL query, this is used in the query for being able to access
         * all of these attributes, including ones that exist on all projects
         */
        List<String> allProjectAttributes = new ArrayList<String>();

        allProjectAttributes.addAll(Arrays.asList(NEW_PROJECT_ATTRIBUTES));
        allProjectAttributes.addAll(Arrays.asList(CI_PROJECT_ATTRIBUTES));
        allProjectAttributes.addAll(Arrays.asList(QC_STEP_ATTRIBUTES));
        allProjectAttributes.addAll(Arrays.asList(TRANSLATE_STEP_ATTRIBUTES));

        String[] checkAllAttributes = allProjectAttributes.toArray(new String[allProjectAttributes.size()]);

        /**
         * Construct a list of just the custom project attributes that are found in the projects that
         * have the automatic actions applied that are needed for the translator payment report.  This
         * is used in the query for pulling just these projects
         */
        List<String> coreProjectAttributes = new ArrayList<String>();

        coreProjectAttributes.addAll(Arrays.asList(NEW_PROJECT_ATTRIBUTES));
        coreProjectAttributes.addAll(Arrays.asList(QC_STEP_ATTRIBUTES));
        coreProjectAttributes.addAll(Arrays.asList(TRANSLATE_STEP_ATTRIBUTES));

        String[] checkCoreAttributes = coreProjectAttributes.toArray(new String[coreProjectAttributes.size()]);

        // Construct the SQL Query
        String projectAttributeQuery = "" +
                "SELECT\n" +
                // All of the normal project attributes
                "  pa.projectId,\n" +
                "  pa.projectGroupId,\n" +
                "  pa.workgroupName,\n" +
                "  pa.creationDate,\n" +
                "  pa.completionDate,\n" +
                "  pa.localeName,\n" +

                // The getting the table containing custom project attributes
                "  cpaa.name,\n" +
                "  cpaa.valueString,\n" +
                "  cpaa.valueDate,\n" +

                // Normal project group attributes
                "  pga.sourceLocale\n" +

                // Getting it from these tables
                "FROM projectsArchive pa, customProjectAttributesArchive cpaa, projectGroupsArchive pga\n" +

                // Making sure the tables match up by ID
                "WHERE cpaa.projectId = pa.projectId AND pga.projectGroupId = pa.projectGroupId\n" +

                // Only within the given date range requested
                " AND pa.completionDate >= ?" +
                " AND pa.completionDate <= ?" +

                // Only within the workgroups specified
                " AND pa.workgroupName in ("+getParamCount(workgroupNames)+")\n" +

                // Get all of the custom attributes that we need
                " AND cpaa.name in ("+getParamCount(checkAllAttributes)+")\n" +

                // This section limits us to retrieving only the projects containing the "new" attributes
                // that are included in projects by the automatic actions associated with this report
                " AND EXISTS(SELECT * FROM customProjectAttributesArchive cpaa2, projectsArchive pa2\n" +
                " WHERE pa.projectId=pa2.projectId AND cpaa2.projectId = pa2.projectId AND cpaa2.name\n" +
                " IN (" +getParamCount(checkCoreAttributes)+ ") and cpaa2.valueString IS NOT NULL);";

        // Get the query if we're in debug mode
        log.debug("SQL Query = " + projectAttributeQuery);

        // Setup the connection and results for use below
        Connection wsDbConnection = null;
        ResultSet rs;

        try {

            // Get DB connection from WorldServer
            wsDbConnection = wsContext.getConnectionManager().getWSDbPoolConnection();

            // Setup a prepared statement to process the placeholders in the query
            PreparedStatement ps = wsDbConnection.prepareStatement(projectAttributeQuery);

            // Check to make sure it's valid
            if(ps != null) {

                // Insert placeholders
                setDbQueryParams(ps, checkAllAttributes, checkCoreAttributes);

                // Run the query
                rs = ps.executeQuery();

                // Check if valid
                if (rs != null) {
                    // Process the results of the query
                    processQueryData(rs);
                    rs.close();
                }

                ps.close();
            }
        } finally {
            if(wsDbConnection != null) {
                wsDbConnection.close();
            }
        }

        // Return the final list of projects
        return archivedProjects;
    }


    /**
     * Populate the placeholders in the SQL query
     *
     * @param preparedStatement - The prepared statement
     * @param allAttributes - All WS custom project attributes needed
     * @param coreAttributes - Just the "new" attributes for this report
     * @throws SQLException
     */
    private void setDbQueryParams (PreparedStatement preparedStatement, String[] allAttributes, String[] coreAttributes) throws SQLException {
        // sets query parameters in the following order:

        // 1. start and end-date params.
        // 2. workgroup params
        // 3. attribute params

        // Apply the dates for the date placeholders
        preparedStatement.setDate(1, new java.sql.Date(projectsCompletedAfter.getTime()));
        preparedStatement.setDate(2, new java.sql.Date(projectsCompletedBefore.getTime()));

        // For SQL - 1 based index starting at 3rd element
        int totalIndex = 3;

        // Apply the workgroup name placeholders
        for (int index = 0; index < workgroupNames.length; index++, totalIndex++) {
            preparedStatement.setString(totalIndex, workgroupNames[index]);
        }

        // Apply all of the attributes for the main query placeholder
        for (int index = 0; index < allAttributes.length; index++, totalIndex++) {
            preparedStatement.setString(totalIndex, allAttributes[index]);
        }

        // Apply only the new attributes for the sub-query placeholder
        for (int index = 0; index < coreAttributes.length; index++, totalIndex++) {
            preparedStatement.setString(totalIndex, coreAttributes[index]);
        }
    }


    /**
     * Counts the number of elements in an array and generates placeholders for the SQL query
     *
     * @param listOfValues - The array to count
     * @return returns a CSV of question marks
     */
    private String getParamCount(String[] listOfValues) {

        String[] newArray = new String[listOfValues.length];

        for (int index = 0; index <listOfValues.length; index++) {
            newArray[index] = "?";
        }

        return CollectionUtils.arrayToCsv(newArray);
    }


    /**
     * Once we get the query this is what we do with it
     *
     * @param results - The SQL query results
     * @throws SQLException
     */
    private void processQueryData(ResultSet results) throws SQLException {

        // Create a map to hold project info
        HashMap<String, ProjectInfo> allProjectInfo = new HashMap<String, ProjectInfo>();

        // Loop through all of the project results
        while (results.next()) {

            // Get the project attributes
            String projectId = results.getString("projectId");

            /**
             * The "getDate()" method for ResultSet doesn't actually return a Java.util.Date
             * object, it returns an SQL date object that doesn't include time.  In order to get
             * a standard date we need to get the timestamp and then the getTime() which returns a
             * UNIX timestamp that we can use to get a normal Date
             */
            Date creationDate = new Date(results.getTimestamp("creationDate").getTime());
            Date completionDate = new Date(results.getTimestamp("completionDate").getTime());

            String attributeName = results.getString("name");
            String strAttributeValue = results.getString("valueString");
            Date dateAttributeValue = results.getDate("valueDate");
            String targetLocale = results.getString("localeName");
            String sourceLocale = results.getString("sourceLocale");

            ProjectInfo tempProjectInfo;

            // Check to see if the project already exists
            if (allProjectInfo.containsKey(projectId)) {

                // If so, get it
                tempProjectInfo = allProjectInfo.get(projectId);

            } else {
                // If not, create it
                tempProjectInfo = new ProjectInfo();
                tempProjectInfo.setProjectId(projectId);
                tempProjectInfo.setCreationDate(creationDate);
                tempProjectInfo.setCompletionDate(completionDate);
                tempProjectInfo.setSourceLocale(sourceLocale);
                tempProjectInfo.setTargetLocale(targetLocale);
            }

            // Add the current project into the list of all projets
            allProjectInfo.put(projectId, tempProjectInfo);

            if ( strAttributeValue != null ) {
                allProjectInfo.get(projectId).getGenericProjectAttributes().put(
                        attributeName, strAttributeValue);
            }

            if ( dateAttributeValue != null ) {
                allProjectInfo.get(projectId).getGenericProjectAttributes().put(
                        attributeName, dateAttributeValue);
            }
        }

        // Add the project info to the list of projects
        setArchivedProjectsInfo(allProjectInfo);

    }


    /**
     * This is where we set most of the info we are concerned about for the reports
     *
     * @param allProjectInfo - The main list of projects
     */
    private void setArchivedProjectsInfo(HashMap<String, ProjectInfo> allProjectInfo) {

        // Loop through all projects
        for (String projectId : allProjectInfo.keySet()) {

            // Get the project info for this project
            ProjectInfo projectInfo = allProjectInfo.get(projectId);

            // Get the Translate step info from the project
            ArchivedProject archivedProject = new ArchivedProject(
                    wsContext,
                    projectInfo,
                    "Translate",
                    TRANSLATE_STEP_ATTRIBUTES
            );

            // Add to the archived projects list
            archivedProjects.add(archivedProject);

            // Get the user who claimed the QC step, if any
            String qcClaimedBy =
                    (String) projectInfo.getGenericProjectAttributes().get("QCStepAcceptedBy");

            // Only proceed to add QC step if it was claimed by a user
            if(qcClaimedBy != null && !qcClaimedBy.equals("")) {

                // Get the QC step info from the project
                archivedProject = new ArchivedProject(
                        wsContext,
                        projectInfo,
                        "QC",
                        QC_STEP_ATTRIBUTES
                );

                // Add the QC step to the projects list
                archivedProjects.add(archivedProject);
            }
        }
    }
}

