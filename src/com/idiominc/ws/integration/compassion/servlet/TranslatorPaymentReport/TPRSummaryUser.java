package com.idiominc.ws.integration.compassion.servlet.TranslatorPaymentReport;

/**
 * Data structure to use as key containing user and step information for the summary
 * report
 *
 * Created by cslack on 2/4/2016.
 */

public class TPRSummaryUser {
    private String name;
    private String stepName;
    private String template;


    /**
     * Constructor
     *
     * @param name - User name
     * @param stepName - Name of the workflow step
     * @param template - Compassion template ID
     */
    public TPRSummaryUser(String name, String stepName, String template) {

        // Replace nulls with empty strings to prevent problems later
        if(name == null) name = "";
        if(stepName == null) stepName = "";
        if(template == null) template = "";

        // Initialize all class variables
        this.name = name;
        this.stepName = stepName;
        this.template = template;
    }

    /**
     * Getters and Setters
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStepName() {
        return stepName;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TPRSummaryUser that = (TPRSummaryUser) o;

        return name.equals(that.name) &&
                stepName.equals(that.stepName) &&
                template.equals(that.template);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + stepName.hashCode();
        result = 31 * result + template.hashCode();
        return result;
    }
}
