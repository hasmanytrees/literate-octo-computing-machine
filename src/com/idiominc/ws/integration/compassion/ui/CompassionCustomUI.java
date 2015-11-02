package com.idiominc.ws.integration.compassion.ui;

import com.idiominc.external.config.Config;
import com.idiominc.ws.integration.compassion.authenticator.saml.CISAMLAuthorization;
import com.idiominc.ws.integration.compassion.servlet.Embed;
import com.idiominc.ws.integration.profserv.commons.sso.SSOAuthorization;
import com.idiominc.ws.integration.profserv.commons.sso.SSOHook;
import com.idiominc.ws.integration.profserv.commons.sso.SSORequestConsumer;
import com.idiominc.ws.integration.profserv.commons.wssdk.ui.WSHtmlLink;
import com.idiominc.ws.integration.profserv.commons.wssdk.ui.WSHtmlTable;
import com.idiominc.ws.integration.profserv.commons.wssdk.ui.WSHtmlUIHook;
import com.idiominc.ws.integration.profserv.commons.wssdk.ui.WSPopupLink;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.component.ui.WSUIHooksComponent;
import com.idiominc.wssdk.workflow.WSTask;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * WorldServer Custom UI Hook built for Compassion WorldServer instance.
 * This is a singleton-class; there will be only one custom UI Hook class.
 *
 */
public class CompassionCustomUI extends WSUIHooksComponent {

    private static final Logger log = Logger.getLogger(CompassionCustomUI.class);

    private static final SSOHook samlHook = new SSOHook() {
        public SSORequestConsumer getSSORequestConsumer() {
            return new SSORequestConsumer() {
                public SSOAuthorization getSSOAuthorizer() {
                    return new CISAMLAuthorization();
                }
            };
        }
    };

    /**
     * Generate custom login page snippet to include the SAML SSO login button
     * @param context WorldServer context
     * @param request HTTP request
     * @return custom HTML snippet
     */
    public String generateLoginPageSnippet(WSContext context, HttpServletRequest request) {
        return samlHook.generateLoginPageSnippet(context, request);
    }

    /**
     * Generate custom task page snippet to include the button to launch the transcription UI
     * @param context WorldServer context
     * @param request HTTP request
     * @return custom HTML snippet
     */
    public String generateTaskListPageSnippet(WSContext context, HttpServletRequest request) {
        try {

            System.out.println("task-list");
            WSHtmlUIHook hook = new WSHtmlUIHook(context, request);
            Embed.embedJS(context, hook, "shared/js/ci_task_list.js");
            return hook.getHtml();

        } catch (Exception e) {
            e.printStackTrace();
            log.error("UI Error", e);
            return "UI Error: " + e.getMessage();
        }

    }

    /**
     * Generate custom Browser Workbench snippet to include the stop-word list
     * @param context WorldServer Context
     * @param request HTTP request
     * @return custom HTML snippet
     */
    public String generateBWBSnippet(WSContext context, HttpServletRequest request) {

        try {

            WSHtmlUIHook hook = new WSHtmlUIHook(context, request);
            WSHtmlTable twoTables = new WSHtmlTable(context, request, null, 2);

            // add stop word data
            //String taskIds = request.getParameter(WSUIHooksComponent.OBJECT_ID_PARAMETER);
            String taskIds = getTaskId(request);
            if(taskIds == null) {
                log.error("UI Error: Could not identify associated task ID!");
                return "UI Error: Could not identify associated task ID!";
            }

            WSTask task = context.getWorkflowManager().getTask(Integer.parseInt(taskIds));
            if(task == null) {
                log.error("UI Error: Could not find task with ID:" + taskIds);
                return "UI Error: Could not find task with ID:" + taskIds;
            }

            String stopWordData = task.getAttribute(Config.getStopWordsAttributeName(context));
            WSHtmlTable stopwordTable = new WSHtmlTable(context, request, null, 3);
            if(stopWordData != null && !stopWordData.equals("")) {

                // create and setup stop-word table
                stopwordTable.setAttribute("border", "1");
                stopwordTable.addRow(new String[] {"Segment ID", "Text", "Stop Word"});

                // add stop words as it was found and captured in the custom attribute by earlier preprocessing
                String[] rowDatas = stopWordData.split("\\|\\|");
                for(String rowData : rowDatas) {
                    String[] rowDataStrings = rowData.split("\\|");
                    String segID = rowDataStrings[0];
                    String srcText = rowDataStrings[1];
                    String term = collectAllTerms(rowDataStrings);

                    stopwordTable.addRow(new String[] {segID, srcText, term});
                }

//                hook.add(table);
            }

            // add other useful links table
            // -- This is R4 requirements; we will revisit
//            WSHtmlTable linkstable = new WSHtmlTable(context, request, null, 1);
//            linkstable.addRow(new String[]{"Helpful Links"});
//            WSPopupLink searchLink = new WSPopupLink(context, request, null, "Helpful Links", "http://www.google.com", "General Search", WSPopupLink.NORMAL);
//            WSPopupLink sponsorLink = new WSPopupLink(context, request, null, "Helpful Links", "http://www.compassion.com/sponsor_a_child/default.htm", "Sponsor a child", WSPopupLink.NORMAL);
//            WSPopupLink donateLink = new WSPopupLink(context, request, null, "Helpful Links", "http://www.compassion.com/where-most-needed.htm", "Donate now", WSPopupLink.NORMAL);
//            linkstable.addRow(new Object[] {searchLink});
//            linkstable.addRow(new Object[] {sponsorLink});
//            linkstable.addRow(new Object[]{donateLink});
//            twoTables.addRow(new WSHtmlTable[]{stopwordTable, linkstable});

            twoTables.addRow(new WSHtmlTable[]{stopwordTable, new WSHtmlTable(context, request, null, 1)});
            hook.add(twoTables);

            String saveTaskId = "&task=" + taskIds;
            hook.saveParam("task",saveTaskId);


            return hook.getHtml();

        } catch (Exception e) {
            e.printStackTrace();
            log.error("UI Error" , e);
            return "UI Error: " + e.getMessage();
        }
    }

    // get the task ID that was passed in, or stored if we submitted from the workbench
    private String getTaskId(HttpServletRequest request) {
        String tempTaskIdValue = request.getParameter(WSUIHooksComponent.OBJECT_ID_PARAMETER);
        try {
            int tempIdValue = Integer.parseInt(tempTaskIdValue);

        } catch (NumberFormatException e) {
            tempTaskIdValue = request.getParameter("task");
        }

        if ( (null != tempTaskIdValue) && !("".equals(tempTaskIdValue))) {
            return tempTaskIdValue;
        } else {
            tempTaskIdValue = request.getParameter("task");
        }

        if ((null != tempTaskIdValue) && !("".equals(tempTaskIdValue))) {
            return tempTaskIdValue;
        } else {
            return null;
        }

    }

    // For displaying the stop-word terms and combining all terms found for each stop-word found
    private String collectAllTerms(String[] rowDataStrings) {
        StringBuilder str = new StringBuilder();

        for(int i = 2; i < rowDataStrings.length; i++) {
            str.append(rowDataStrings[i]).append(",");
        }

        String retStr = str.toString();
        return retStr.substring(0, retStr.length()-1);
    }

    /**
     * Generate custom Segmented asset editor snippet to include the stop-word list
     * @param context WorldServer context
     * @param request HTTP request
     * @return custom HTML snippet
     */
    public String generateSAESnippet(WSContext context, HttpServletRequest request) {
        try {

            WSHtmlUIHook hook = new WSHtmlUIHook(context, request);
            WSHtmlTable twoTables = new WSHtmlTable(context, request, null, 2);

            //String taskIds = request.getParameter(WSUIHooksComponent.OBJECT_ID_PARAMETER);
            String taskIds = getTaskId(request);
            if(taskIds == null) {
                log.error("UI Error: Could not identify associated task ID!");
                return "UI Error: Could not identify associated task ID!";
            }

            WSTask task = context.getWorkflowManager().getTask(Integer.parseInt(taskIds));
            if(task == null) {
                log.error("UI Error: Could not find task with ID:" + taskIds);
                return "UI Error: Could not find task with ID:" + taskIds;
            }

            String stopWordData = task.getAttribute(Config.getStopWordsAttributeName(context));
            WSHtmlTable stopwordTable = new WSHtmlTable(context, request, null, 3);
            if(stopWordData != null && !stopWordData.equals("")) {

                // create and setup stop-word table
                WSHtmlTable table = new WSHtmlTable(context, request, null, 3);
                table.setAttribute("border", "1");
                table.addRow(new String[]{"Segment ID", "Text", "Stop Word"});

                // add stop words as it was found and captured in the custom attribute by earlier preprocessing
                String[] rowDatas = stopWordData.split("\\|\\|");
                for(String rowData : rowDatas) {
                    String[] rowDataStrings = rowData.split("\\|");
                    String segID = rowDataStrings[0];
                    String srcText = rowDataStrings[1];
                    String term = collectAllTerms(rowDataStrings);

                    stopwordTable.addRow(new String[] {segID, srcText, term});
                }

                //hook.add(table);
            }

            twoTables.addRow(new WSHtmlTable[]{stopwordTable, new WSHtmlTable(context, request, null, 1)});
            hook.add(twoTables);

            String saveTaskId = "&task=" + taskIds;
            hook.saveParam("task",saveTaskId);

            return hook.getHtml();

        } catch (Exception e) {
            e.printStackTrace();
            log.error("UI Error" , e);
            return "UI Error: " + e.getMessage();
        }

    }


    public String getDescription() {
        return "Compassion UI Hook; includes SAML support";
    }

    public String getVersion() {
        return "1.0";
    }
}
