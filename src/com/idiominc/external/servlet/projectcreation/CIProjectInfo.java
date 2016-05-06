package com.idiominc.external.servlet.projectcreation;

/**
 * This data structure contains information about the project used in setting it up
 *
 * Created by cslack on 3/21/2016.
 */
public class CIProjectInfo {

    public enum CIWorkgroups {
        _WORKGROUP_FIELD_OFFICE,
        _WORKGROUP_GLOBAL_PARTNER
    }

    public enum CITargetLocale {
        _TARGET_LOCALE_TARGET,
        _TARGET_LOCALE_INTERMEDIARY,
        _PAYLOAD_LOCALES
    }

    public enum CIDirectSource {
        _DIRECT_SOURCE_TRANSLATION_TARGET_DIRECT,
        _DIRECT_SOURCE_ORIGINAL_SRC,
        _DIRECT_SOURCE_INTERMEDIARY_LOCALE_DIRECT,
        _DIRECT_SOURCE_INTERMEDIARY_LOCALE,
        _DIRECT_SOURCE_PAYLOAD_SRC_LOCALE
    }

    private Boolean secondStepProjectRequired;
    private Boolean qcNotRequiredOverride;
    private String workflowOverrideName;
    private String returnTextRequirements;
    private String targetWorkflowName;
    private CITargetLocale targetLocaleType;
    private CIWorkgroups workgroupLocation;
    private CIDirectSource directSourceLocaleType;
    private String languageExceptionType;
    private String exception;

    public CIProjectInfo() {
        secondStepProjectRequired = null;
        qcNotRequiredOverride = null;
        workflowOverrideName = null;
        returnTextRequirements = null;
        targetWorkflowName = null;
        targetLocaleType = null;
        workgroupLocation = null;
        directSourceLocaleType = null;
        languageExceptionType = null;
        exception = null;
    }


    /**
     * Getters and Setters
     */
    public Boolean getSecondStepProjectRequired() {
        return secondStepProjectRequired;
    }

    public void setSecondStepProjectRequired(Boolean secondStepProjectRequired) {
        this.secondStepProjectRequired = secondStepProjectRequired;
    }

    public Boolean getQcNotRequiredOverride() {
        return qcNotRequiredOverride;
    }

    public void setQcNotRequiredOverride(Boolean qcNotRequiredOverride) {
        this.qcNotRequiredOverride = qcNotRequiredOverride;
    }

    public String getWorkflowOverrideName() {
        return workflowOverrideName;
    }

    public void setWorkflowOverrideName(String workflowOverrideName) {
        this.workflowOverrideName = workflowOverrideName;
    }

    public String getReturnTextRequirements() {
        return returnTextRequirements;
    }

    public void setReturnTextRequirements(String returnTextRequirements) {
        this.returnTextRequirements = returnTextRequirements;
    }

    public String getTargetWorkflowName() {
        return targetWorkflowName;
    }

    public void setTargetWorkflowName(String targetWorkflowName) {
        this.targetWorkflowName = targetWorkflowName;
    }

    public CITargetLocale getTargetLocaleType() {
        return targetLocaleType;
    }

    public void setTargetLocaleType(CITargetLocale targetLocaleType) {
        this.targetLocaleType = targetLocaleType;
    }

    public CIWorkgroups getWorkgroupLocation() {
        return workgroupLocation;
    }

    public void setWorkgroupLocation(CIWorkgroups workgroupLocation) {
        this.workgroupLocation = workgroupLocation;
    }

    public CIDirectSource getDirectSourceLocaleType() {
        return directSourceLocaleType;
    }

    public void setDirectSourceLocaleName(CIDirectSource directSourceLocaleType) {
        this.directSourceLocaleType = directSourceLocaleType;
    }

    public String getLanguageExceptionType() {
        return languageExceptionType;
    }

    public void setLanguageExceptionType(String languageExceptionType) {
        this.languageExceptionType = languageExceptionType;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }
}
