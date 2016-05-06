package com.idiominc.external.servlet.projectcreation;

//internal dependencies

import com.idiominc.external.AISUtils;
import com.idiominc.external.config.Config;
import com.idiominc.external.json.JSONArray;
import com.idiominc.external.json.JSONObject;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSContextManager;
import com.idiominc.wssdk.WSRunnable;
import com.idiominc.wssdk.WSRuntimeException;
import com.idiominc.wssdk.ais.WSAisException;
import com.idiominc.wssdk.ais.WSNode;
import com.idiominc.wssdk.ais.WSSystemPropertyKey;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.linguistic.WSFilterGroup;
import com.idiominc.wssdk.review.WSQualityModel;
import com.idiominc.wssdk.workflow.WSProject;
import com.idiominc.wssdk.workflow.WSProjectGroup;
import com.idiominc.wssdk.workflow.WSTask;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPathException;
import java.io.*;
import java.util.HashMap;
import java.util.Map;


/**
 * External component responsible for listening for project creation request and creating projects in WorldServer.
 *
 * @author SDL Professional Services
 */
public class NewProjectCreation extends HttpServlet {

    //log
    private final Logger log = Logger.getLogger(NewProjectCreation.class);

    protected void doPost(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {

        final Exception[] eRef = new Exception[1];

        try {

            httpServletResponse.setContentType("application/json");
            WSContextManager.runWithToken("2", new WSRunnable() {

                public boolean run(WSContext context) {
                    try {

                        validate(context, httpServletRequest);

                        asJSON(createProject(
                                context,
                                getXMLPayload(httpServletRequest)
                        )).writeJSONString(httpServletResponse.getWriter());

                        return true;

                    } catch (Exception e) {
                        log.error("Project creation error:", e);
                        eRef[0] = e;
                        return false;
                    }
                }
            });

            if (eRef[0] != null)
                throw eRef[0];

        } catch (Exception e) {

            if (e instanceof AuthorizationException) {
                httpServletResponse.setStatus(401);
            } else {
                httpServletResponse.setStatus(400);
            }

            try {
                log.error("Project creation error - ", e);
                JSONObject msg = new JSONObject();
                msg.put("Reason", e.getMessage());
                msg.writeJSONString(httpServletResponse.getWriter());

            } catch (IOException e2) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Process the XML payload first and then create a new project
     *
     * @param context - WorldServer Context
     * @param xmlPayload - XML Payload
     * @return Return the project group
     * @throws Exception
     */
    public WSProjectGroup createProject(WSContext context, String xmlPayload) throws Exception {

        // Process the XML payload
        ParsedPayload parsedPayload = new ParsedPayload(context, xmlPayload);

        // Create a new project
        return createProject(context, parsedPayload);
    }


    /**
     * Create a new project from the processed XML payload
     *
     * @param context - WorldServer Context
     * @param parsedPayload - Processed XML payload
     * @return project group
     * @throws Exception
     */
    public WSProjectGroup createProject(WSContext context, ParsedPayload parsedPayload) throws Exception {

        // Die if the remote content mount doesn't exist
        if (context.getAisManager().getNode("/Remote Content") == null) {
            throw new IOException("The default Remote Content mount does not exist!");
        }

        // Get the current project
        CIProject currentProject = new CIProject(context, parsedPayload);

        // Create a project group from the current project and return it
        return setupProjectGroup(currentProject);
    }


    /**
     * Setup a project group for the current Compassion project
     *
     * @param currentProject - Compassion Project
     * @return project group
     * @throws XPathException
     * @throws IOException
     * @throws WSAisException
     */
    private WSProjectGroup setupProjectGroup(CIProject currentProject)
            throws XPathException, IOException, WSAisException {

        // Grab common elements from the project
        WSContext context = currentProject.getContext();
        ParsedPayload parsedPayload = currentProject.getParsedPayload();
        String returnTextRequirements = currentProject.getReturnTextRequirements();


        // Setup the project in AIS
        WSNode n = AISUtils.createContentInAIS(
                context,
                currentProject.getDirectSrcLocale(),
                parsedPayload.getTargetLocales(),
                currentProject.getWorkgroupName(),
                parsedPayload.getContent()
        );


        // Get the AIS locale folders
        AISUtils.setupTargetLocaleFolders(context, parsedPayload.getSourceLocale(), currentProject.getTargetLocales());

        // Setup the project group
        WSProjectGroup projectGroup = context.getWorkflowManager().createProjectGroup(
                parsedPayload.getProjectName(),
                parsedPayload.getProjectDescription(),
                currentProject.getWorkgroup(),
                currentProject.getTargetLocales(),
                new WSNode[]{n},
                currentProject.getTargetWorkflow(),
                0,
                null,
                false
        );




        // Check to see if resulting project group is valid
        if (projectGroup == null || projectGroup.getProjects().length != parsedPayload.getTargetLocales().length) {
            if (projectGroup != null) {
                for (WSProject p : projectGroup.getProjects()) {
                    p.cancel("Invalid project was created!");
                }
            }
            throw new IOException("Failed to create projects!");
        }

        WSFilterGroup filterGroup = null;

        switch(parsedPayload.getDirection()) {
            case "Third Party Letter":
                filterGroup = context.getLinguisticManager().getFilterGroup("SecondStep");
                break;

            case "Supporter to Beneficiary":
                filterGroup = context.getLinguisticManager().getFilterGroup("FirstStep");
                break;

            case "Beneficiary to Supporter":
                filterGroup = context.getLinguisticManager().getFilterGroup("FirstStep");
                break;

            default:
                filterGroup = context.getLinguisticManager().getFilterGroup("FirstStep");
                break;
        }



        // UPDATED 4/21/2016: Deadlock reducing code change
        WSContextManager.run(context, new WSRunnable() {
            @Override
            public boolean run(WSContext wsContext) {
                return false;
            }
        });

        // store the second step project preference
        configureProjects(context, parsedPayload, currentProject.getWorkflowOverrideName(), currentProject.isSecondStepProjectRequired(), currentProject.isQcNotRequiredOverride(),
                returnTextRequirements, currentProject.getLanguageExceptionType(), filterGroup, projectGroup.getId());

        // Start the tasks only after the project creation is completed
        WSProject[] projects = projectGroup.getProjects();
        for (WSProject project : projects) {
            context.getWorkflowManager().startTasks(project.getTasks());
        }

//        // store the second step project preference
//        for(WSProject p : projectGroup.getProjects()) {
//            p.setAttribute("secondStepProjectRequired", Boolean.toString(currentProject.isSecondStepProjectRequired()));
//            p.setAttribute("qcNotRequiredOverride", Boolean.toString(currentProject.isQcNotRequiredOverride()));
//            p.setAttribute("workflowOverride", currentProject.getWorkflowOverrideName());
//            p.setAttribute("electronicContent", Boolean.toString(
//                    parsedPayload.getProcessRequired().equals(CIProjectConfig._PROCESS_ISL)));
//            //set the default quality model
//            WSQualityModel qModel = context.getReviewManager().getQualityModel("Default QC Model");
//            if(qModel != null) {
//                p.setQualityModel(qModel);
//            }
//            p.setAttribute("returnTextRequirements", returnTextRequirements);
//            p.setAttribute("LanguageExceptionType", currentProject.getLanguageExceptionType());
//
//            for(WSTask task: p.getTasks()) {
//                if(filterGroup != null) {
//                    context.getAisManager().getMetaDataNode(((WSAssetTask) task).getTargetPath()).setProperty(WSSystemPropertyKey.FILTER_GROUP, filterGroup);
//                }
//            }
//        }


        return projectGroup;
    }

    // 4/21/2016: DB deadlock reducing code change
    private void configureProjects(WSContext wsContext, final ParsedPayload parsedPayload,
                                   final String workflowOverrideName, final boolean secondStepProjectRequired,
                                   final boolean qcNotRequiredOverride, final String returnTextRequirements, final String languageExceptionType,
                                   final WSFilterGroup filterGroup, final int projectGroupId) throws XPathException, WSAisException {
        WSContextManager.run(wsContext, new WSRunnable() {
            @Override
            public boolean run(WSContext context) {
                try {

                    WSProjectGroup pg = context.getWorkflowManager().getProjectGroup(projectGroupId);
                    for (WSProject p : pg.getProjects()) {
                        // set the default quality model
                        WSQualityModel qModel = context.getReviewManager().getQualityModel("Default QC Model");
                        if (qModel != null) {
                            p.setQualityModel(qModel);
                        }

                        Map<String, String> attributesValues = new HashMap<>();
                        attributesValues.put("secondStepProjectRequired", Boolean.toString(secondStepProjectRequired));
                        attributesValues.put("qcNotRequiredOverride", Boolean.toString(qcNotRequiredOverride));
                        attributesValues.put("workflowOverride", workflowOverrideName);
                        attributesValues.put("electronicContent", Boolean.toString(parsedPayload.getProcessRequired().equals(CIProjectConfig._PROCESS_ISL)));
                        attributesValues.put("returnTextRequirements", returnTextRequirements);
                        attributesValues.put("LanguageExceptionType", languageExceptionType);
                        p.setAttributes(attributesValues);
                        for (WSTask task : p.getTasks()) {
                            if (filterGroup != null) {
                                context.getAisManager().getMetaDataNode(((WSAssetTask) task).getTargetPath()).setProperty(WSSystemPropertyKey.FILTER_GROUP, filterGroup);
                            }
                        }
                    }
                } catch (XPathException | WSAisException e) {
                    throw new WSRuntimeException(e);
                }
                return true;
            }
        });
    }


    /**
     * Get just the XML payload out of the HTTP request
     *
     * @param request HTTP Request
     * @return The XML data
     * @throws IOException
     */
    private String getXMLPayload(HttpServletRequest request) throws IOException {

        StringBuffer payloadBuffer = new StringBuffer();
        String line;
        boolean firstLine = true;

        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));

        while ((line = reader.readLine()) != null) {
            /**
             * This fixes a known Java issue where the Byte Order Mark is appended to the beginning on the
             * first line, this removes the UTF-8 BOM
             */

            if (firstLine) {
                line = removeUTF8BOM(line);
                firstLine = false;
            }

            payloadBuffer.append(line);
        }
        return payloadBuffer.toString();
    }

    /**
     * There is a known XML issue where a UTF BOM character can precede the XML request and this needs to be
     * stripped from the beginning of the file otherwise the XML classes error.
     *
     * @param xmlData - The input XML file data
     * @return The fixed XML data
     */
    private static String removeUTF8BOM(String xmlData) {
        String UTF8_BOM = "\uFEFF";

        if (xmlData.startsWith(UTF8_BOM)) {
            xmlData = xmlData.substring(1);
        }

        return xmlData;
    }


    /**
     * Validates the HTTP servlet request
     *
     * @param context - WorldServer context
     * @param httpServletRequest - HTTP Request
     * @throws AuthorizationException
     */
    protected void validate(WSContext context, HttpServletRequest httpServletRequest) throws AuthorizationException {
        try {
            // allow for comma-separated list of authorized user accounts
            String authorizedNTLMUserStr = Config.getNTLMUser(context);
            String[] authorizedNTLMUsers = authorizedNTLMUserStr.split(",");

            boolean authorizedNTLMUserFound = false;

            for (String authorizedNTLMUser : authorizedNTLMUsers) {
                if (httpServletRequest.getUserPrincipal() !=
                        null && httpServletRequest.getUserPrincipal().getName().equals(authorizedNTLMUser)) {
                    authorizedNTLMUserFound = true;
                }
            }

            if (!authorizedNTLMUserFound)
                throw new AuthorizationException("User is not authorized.");

        } catch (IOException e) {
            throw new AuthorizationException(e.getMessage());
        }
    }


    /**
     * Create an Authorization Exception
     */
    private class AuthorizationException extends Exception {
        AuthorizationException(String msg) {
            super(msg);
        }
    }


    /**
     *
     * @param projectGroup - Project Group
     * @return Project info as JSON
     */
    private JSONObject asJSON(WSProjectGroup projectGroup) {

        JSONObject projectGroupJSON = new JSONObject();

        JSONArray projectList = new JSONArray();
        projectGroupJSON.put("projects", projectList);

        for (final WSProject project : projectGroup.getProjects()) {
            JSONObject projectJSON = new JSONObject() {{
                put("id", new Integer(project.getId()));
                put("tasks", new JSONArray() {{
                    add(new JSONObject() {{
                        put("id", new JSONArray() {{
                            for (final WSTask t : project.getActiveTasks()) {
                                add(new Integer(t.getId()));
                            }
                        }});
                    }});
                }});
            }};

            projectList.add(projectJSON);
        }

        return projectGroupJSON;
    }
}
