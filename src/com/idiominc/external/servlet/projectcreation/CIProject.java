package com.idiominc.external.servlet.projectcreation;

import com.idiominc.external.config.Config;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.user.WSWorkgroup;
import com.idiominc.wssdk.workflow.WSWorkflow;


/**
 *  Class to setup a Compassion project
 *
 * Created by cslack on 3/18/2016.
 */

public class CIProject  {

    // prepare data
    private WSLocale originalSrcLocale;
    private WSLocale translationTgtLocale;

    // direct source is used for use with manual translation only
    private WSLocale directSrcLocale;
    private WSLocale intermediaryLocale;
    private WSLocale[] targetLocales;
    private String direction;
    private WSWorkflow targetWorkflow;
    private String workgroupName;
    private WSWorkgroup workgroup;
    private String workflowOverrideName;
    private boolean secondStepProjectRequired;
    private boolean qcNotRequiredOverride;

    /*
    capture info required to perform content clean-up at delivery time
    This will capture true/false for each of the following three fields
    OriginalText, English Translated Text, Final Translated Text
    If True, then the content will be sent back; if false, then empty content will be sent back for each corresponding field
    "[true|false]|[true|false]|[true|false]"
    */
    private String returnTextRequirements;

    private String LanguageExceptionType;
    private ParsedPayload parsedPayload;
    private WSContext context;

    public CIProject(WSContext context, ParsedPayload parsedPayload) throws Exception {

        this.context = context;
        this.parsedPayload = parsedPayload;

        // Setup local vars
        String processRequired = parsedPayload.getProcessRequired();
        String translationType;

        // Setup the defaults based on payload information
        originalSrcLocale = parsedPayload.getSourceLocale();
        translationTgtLocale = parsedPayload.getTargetLocales()[0];
        directSrcLocale = parsedPayload.getSourceLocale();
        intermediaryLocale = context.getUserManager().getLocale(Config.getIntermediaryLocale(context));
        direction = parsedPayload.getDirection();
        targetWorkflow = parsedPayload.getWorkflow();

        workgroupName = parsedPayload.getWorkgroupName();
        workgroup = parsedPayload.getWorkgroup();
        workflowOverrideName = null;
        qcNotRequiredOverride = false;
        LanguageExceptionType = "";

        // Determine the incoming project type
        if(originalSrcLocale.getName().equals(translationTgtLocale.getName())) {
            translationType = CIProjectConfig._PROJECT_SAME_LANGUAGE_PAIR;
        } else if(originalSrcLocale.getName().equals(intermediaryLocale.getName())) {
            translationType = CIProjectConfig._PROJECT_ENGLISH_AS_SOURCE;
        } else if(translationTgtLocale.getName().equals(intermediaryLocale.getName())) {
            translationType = CIProjectConfig._PROJECT_ENGLISH_AS_TARGET;
        } else {
            translationType = CIProjectConfig._PROJECT_DIFFERENT_LANGUAGE_PAIR;
        }

        // Get the overall project type configuration information
        CIProjectConfig projectConfig = new CIProjectConfig();

        System.out.println("This project is: " + CIProjectConfig.mapIndex(
                translationType,
                processRequired,
                direction,
                parsedPayload.getGPOptInForTranslation()
        ));

        // Get the project info for this specific project
        CIProjectInfo projectInfo = projectConfig.getProjectSettings(CIProjectConfig.mapIndex(
                translationType,
                processRequired,
                direction,
                parsedPayload.getGPOptInForTranslation()
        ));

        // Set all of the values for this project
        processProject(context, projectInfo);
    }


    /**
     * This class assigns all of the values for this project based on the configuration CIProjectConfig
     *
     * @param context - WorldServer Context
     * @param projectInfo - The instructions for the current project
     * @throws Exception
     */

    public void processProject(WSContext context, CIProjectInfo projectInfo) throws Exception {

        if(projectInfo != null) {
            // Check to see if the project combination is valid, if not throw an exception
            if (projectInfo.getException() != null) {
                throw new Exception(projectInfo.getException());
            }

            if (projectInfo.getTargetWorkflowName() != null) {
                this.targetWorkflow = context.getWorkflowManager().getWorkflow(projectInfo.getTargetWorkflowName());
            }

            CIProjectInfo.CITargetLocale targetLocaleType = projectInfo.getTargetLocaleType();

            // Assign the target locale based on the configured type
            if (targetLocaleType != null) {

                switch (targetLocaleType) {
                    case _TARGET_LOCALE_INTERMEDIARY:
                        this.targetLocales = new WSLocale[]{intermediaryLocale};
                        break;

                    case _TARGET_LOCALE_TARGET:
                        this.targetLocales = new WSLocale[]{translationTgtLocale};
                        break;

                    case _PAYLOAD_LOCALES:
                        this.targetLocales = parsedPayload.getTargetLocales();
                        break;
                }
            }

            if (projectInfo.getSecondStepProjectRequired() != null) {
                secondStepProjectRequired = projectInfo.getSecondStepProjectRequired();
            }

            if (projectInfo.getQcNotRequiredOverride() != null) {
                qcNotRequiredOverride = projectInfo.getQcNotRequiredOverride();
            }

            CIProjectInfo.CIWorkgroups workgroupLocation = projectInfo.getWorkgroupLocation();

            // Assign the workgroup based on configured type (currently only 1 option)
            if (workgroupLocation != null) {

                switch (workgroupLocation) {
                    case _WORKGROUP_FIELD_OFFICE:
                        workgroupName = parsedPayload.getFOWorkgroupName();
                        workgroup = context.getUserManager().getWorkgroup(workgroupName);
                        break;

                    case _WORKGROUP_GLOBAL_PARTNER:
                        workgroupName = parsedPayload.getGPWorkgroupName();
                        workgroup = context.getUserManager().getWorkgroup(workgroupName);
                        break;
                }
            }

            if (projectInfo.getWorkflowOverrideName() != null) {
                workflowOverrideName = projectInfo.getWorkflowOverrideName();
            }

            CIProjectInfo.CIDirectSource directSourceLocaleType = projectInfo.getDirectSourceLocaleType();

            // Assign the direct source locale based on the configured type
            if (directSourceLocaleType != null) {

                switch (directSourceLocaleType) {
                    case _DIRECT_SOURCE_TRANSLATION_TARGET_DIRECT:
                        directSrcLocale =
                                context.getUserManager().getLocale(translationTgtLocale.getName() + "-Direct");
                        break;

                    case _DIRECT_SOURCE_ORIGINAL_SRC:
                        directSrcLocale = originalSrcLocale;
                        break;

                    case _DIRECT_SOURCE_INTERMEDIARY_LOCALE_DIRECT:
                        directSrcLocale =
                                context.getUserManager().getLocale(Config.getIntermediaryLocale(context) + "-Direct");
                        break;

                    case _DIRECT_SOURCE_INTERMEDIARY_LOCALE:
                        directSrcLocale = intermediaryLocale;
                        break;

                    case _DIRECT_SOURCE_PAYLOAD_SRC_LOCALE:
                        directSrcLocale = parsedPayload.getSourceLocale();
                        break;
                }
            }

            if (projectInfo.getLanguageExceptionType() != null) {
                LanguageExceptionType = projectInfo.getLanguageExceptionType();
            }

            returnTextRequirements = projectInfo.getReturnTextRequirements();

//          This code in place for automated testing
//
//            BufferedWriter writer;
//
//            File testFile = new File("ccs_test_file.txt");
//
//            writer = new BufferedWriter(new FileWriter(testFile));
//
//            writer.write("SecondStepProjectRequired = " + Boolean.toString(secondStepProjectRequired) + "\n");
//            writer.write("QcNotRequiredOverride = " + Boolean.toString(qcNotRequiredOverride) + "\n");
//            writer.write("workflowOverride = " + workflowOverrideName + "\n");
//            writer.write("returnTextRequirements = " + returnTextRequirements + "\n");
//            writer.write("LanguageExceptionType = " + LanguageExceptionType + "\n");
//            writer.write("directSrcLocale = " + directSrcLocale.getName() + "\n");
//            writer.write("workgroupName = " + workgroupName + "\n");
//            writer.write("targetLocales = " + targetLocales[0].getName() + "\n");
//            writer.write("targetWorkflow = " + targetWorkflow.getName() + "\n");
//
//            writer.close();

        }
    }


    /**
     * Getters and Setters
     */

    public WSLocale getDirectSrcLocale() {
        return directSrcLocale;
    }

    public WSLocale[] getTargetLocales() {
        return targetLocales;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public WSWorkflow getTargetWorkflow() {
        return targetWorkflow;
    }

    public String getWorkgroupName() {
        return workgroupName;
    }

    public WSWorkgroup getWorkgroup() {
        return workgroup;
    }

    public void setWorkgroup(WSWorkgroup workgroup) {
        this.workgroup = workgroup;
    }

    public String getWorkflowOverrideName() {
        return workflowOverrideName;
    }

    public boolean isSecondStepProjectRequired() {
        return secondStepProjectRequired;
    }

    public boolean isQcNotRequiredOverride() {
        return qcNotRequiredOverride;
    }

    public String getLanguageExceptionType() {
        return LanguageExceptionType;
    }

    public String getReturnTextRequirements() {
        return returnTextRequirements;
    }

    public WSContext getContext() {
        return context;
    }

    public void setContext(WSContext context) {
        this.context = context;
    }

    public ParsedPayload getParsedPayload() {
        return parsedPayload;
    }
}
