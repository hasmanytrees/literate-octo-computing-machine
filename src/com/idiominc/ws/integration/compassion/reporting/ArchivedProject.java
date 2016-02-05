package com.idiominc.ws.integration.compassion.reporting;

import com.idiominc.wssdk.WSContext;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class ArchivedProject {

    private static final Logger log = Logger.getLogger(ArchivedProject.class);

    //standard project information
    private String projectId;
    private String projectStartDate;
    private String projectCompletionDate;

    // custom project attributes
    private String templateId;
    private String communicationId;
    private String wasEscalated;
    private String requiredRework;
    private String wasReturnedToQueue;

    private String stepName = "Unknown";
    private String stepAcceptedBy = "Unknown";
    private String stepAcceptedOn = null;
    private String stepCompletedOn = null;
    private String sourceLocale;
    private String targetLocale;

    // Set the formats we'll use for date and time display
    private DateFormat dateFormat;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    // The format that we read in dates
    private SimpleDateFormat inputFormat = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");


    /**
     * Main constructor
     *
     * @param context - WS Context
     * @param projectInfo - Project info for this project
     * @param stepName - Current workflow step name
     * @param stepAttributes - Workflow step attributes
     */
    public ArchivedProject(WSContext context, ProjectInfo projectInfo, String stepName, String[] stepAttributes) {

        // Set the WorldServer user's display locale for date format display info
        Locale userLocale = context.getUser().getRegionalSettingsLanguage().getLocale();

        // Initialize the date format based on user's locale
        this.dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, userLocale);

        // Set standard project attributes
        setProjectId(projectInfo.getProjectId());
        setStepName(stepName);
        setProjectStartDate(this.dateFormat.format(projectInfo.getCreationDate()) + " " + timeFormat.format(projectInfo.getCreationDate()));
        setProjectCompletionDate(this.dateFormat.format(projectInfo.getCompletionDate()) + " " + timeFormat.format(projectInfo.getCompletionDate()));

        // Set custom attributes
        setCommunicationId((String)projectInfo.getGenericProjectAttributes().get("CompassionSBCId"));
        setTemplateId((String)projectInfo.getGenericProjectAttributes().get("TemplateID"));
        setTargetLocale(projectInfo.getTargetLocale());
        setRequiredRework((String)projectInfo.getGenericProjectAttributes().get("TranslationSubmittedForRework"));
        setWasReturnedToQueue((String)projectInfo.getGenericProjectAttributes().get("ReturnedToQueue"));
        setWasEscalated((String)projectInfo.getGenericProjectAttributes().get("TranslationEscalated"));
        setStepValues(stepAttributes, projectInfo.genericProjectAttributes);

        String secondStep = (String)projectInfo.getGenericProjectAttributes().get("TwoStepProcess");

        // Set the sourceLocale as depending on the step we're on, this is due to the two step process
        // for Compassion
        if( secondStep != null && !secondStep.equals("")) {
            setSourceLocale(projectInfo.getSourceLocale());
        } else {
            setSourceLocale((String)projectInfo.getGenericProjectAttributes().get("OriginalLanguage"));
        }
   }


    /**
     * Use to setup step attribute values
     *
     * @param stepAttrNames - Names of all step attributes
     * @param genericProjectAttributes - All project attributes from query
     */
    private void setStepValues(String[] stepAttrNames, HashMap<String, Object> genericProjectAttributes) {

        // Loop through the custom attributes for translate or QC steps
        for (String attrName : stepAttrNames) {

            // Assign the AcceptedBy username attribute
            if (attrName.endsWith("AcceptedBy")) {
                setStepAcceptedBy((String) genericProjectAttributes.get(attrName));
            }

            // Assign the AcceptedOn date
            if (attrName.endsWith("AcceptedOn")) {

                Date acceptedOn;

                try {
                    // Get the date string
                    String dateString = (String) genericProjectAttributes.get(attrName);

                    // Check if valid
                    if(dateString != null) {

                        // If so convert it into a java.util.Date
                        acceptedOn = inputFormat.parse(dateString);

                        // Convert back into a string with localized format
                        setStepAcceptedOn(this.dateFormat.format(acceptedOn) + " " + timeFormat.format(acceptedOn));
                    }

                } catch (ParseException e) {
                    log.error("Error setting step values for report.",e);
                }
            }

            // Assign the CompletedOn date
            if (attrName.endsWith("CompletedOn")) {

                Date completedOn;

                try {
                    // Get the date string
                    String dateString = (String) genericProjectAttributes.get(attrName);

                    // Check if valid
                    if(dateString != null) {

                        // If so convert it into a java.util.Date
                        completedOn = inputFormat.parse(dateString);

                        // Convert back into a string with localized format
                        setStepCompletedOn(this.dateFormat.format(completedOn) + " " + timeFormat.format(completedOn));
                    }

                } catch (ParseException e) {
                    log.error("Error parsing CompletedOn value.",e);
                }
            }
        }
    }


    /**
     * Takes a boolean encoded as a string and "converts" to "Yes" or "No"
     *
     * @param word - Boolean value to convert
     * @return - Yes or no depending on string based boolean value
     */
    private String stringBoolean(String word) {
        if(word != null && word.equals("true")) {
            return "Yes";
        } else {
            return "No";
        }
    }

    /**
     * Getters and Setters
     */
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectStartDate() {
        return projectStartDate;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getStepAcceptedBy() {
        return stepAcceptedBy;
    }

    public void setStepAcceptedBy(String stepAcceptedBy) {
        this.stepAcceptedBy = stepAcceptedBy;
    }

    public String getStepAcceptedOn() {
        return stepAcceptedOn;
    }

    public void setStepAcceptedOn(String stepAcceptedOn) {
        this.stepAcceptedOn = stepAcceptedOn;
    }

    public String getStepCompletedOn() {
        return stepCompletedOn;
    }

    public void setStepCompletedOn(String stepCompletedOn) {
        this.stepCompletedOn = stepCompletedOn;
    }

    public void setProjectStartDate(String projectStartDate) {
        this.projectStartDate = projectStartDate;
    }

    public String getProjectCompletionDate() {
        return projectCompletionDate;
    }

    public void setProjectCompletionDate(String projectCompletionDate) {
        this.projectCompletionDate = projectCompletionDate;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getCommunicationId() {
        return communicationId;
    }

    public void setCommunicationId(String communicationId) {
        this.communicationId = communicationId;
    }

    public String isWasEscalated() {
        return wasEscalated;
    }

    public void setWasEscalated(String wasEscalated) {
        this.wasEscalated = stringBoolean(wasEscalated);
    }

    public String isRequiredRework() {
        return requiredRework;
    }

    public void setRequiredRework(String requiredRework) {
        this.requiredRework = stringBoolean(requiredRework);
    }

    public String isWasReturnedToQueue() {
        return wasReturnedToQueue;
    }

    public void setWasReturnedToQueue(String wasReturnedToQueue) {
        this.wasReturnedToQueue = stringBoolean(wasReturnedToQueue);
    }

    public String getSourceLocale() {
        return sourceLocale;
    }

    public void setSourceLocale(String sourceLocale) {
        this.sourceLocale = sourceLocale;
    }

    public String getTargetLocale() {
        return targetLocale;
    }

    public void setTargetLocale(String targetLocale) {
        this.targetLocale = targetLocale;
    }


    /**
     * Equals and Hashcodes
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArchivedProject that = (ArchivedProject) o;

        return getProjectId().equals(that.getProjectId())
                && getStepName().equals(that.getStepName());

    }

    @Override
    public int hashCode() {
        int result = getProjectId().hashCode();
        result = 31 * result + getStepName().hashCode();
        return result;
    }
}
