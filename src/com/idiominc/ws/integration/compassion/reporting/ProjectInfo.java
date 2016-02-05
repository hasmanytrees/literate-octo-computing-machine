package com.idiominc.ws.integration.compassion.reporting;

import java.util.Date;
import java.util.HashMap;

class ProjectInfo {

    private Date creationDate;
    private Date completionDate;
    HashMap<String,Object> genericProjectAttributes = new HashMap<String,Object>();
    String projectId;

    private String sourceLocale;
    private String targetLocale;

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

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getCompletionDate() {
        return completionDate;
    }

    public HashMap<String, Object> getGenericProjectAttributes() {
        return genericProjectAttributes;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setCompletionDate(Date completionDate) {
        this.completionDate = completionDate;
    }
}
