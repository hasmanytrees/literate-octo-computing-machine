package com.idiominc.ws.integration.compassion.servlet.TranslatorPaymentReport;

/**
 * Data structure for storing the results for the summary report
 *
 * Created by cslack on 2/4/2016.
 */
public class TPRSummaryResults {
    private int tasksCompleted;
    private int escalationCount;
    private int reworkCount;
    private int returnedToQueueCount;

    /**
     * Constructor
     */
    public TPRSummaryResults() {
        this.tasksCompleted = 0;
        this.escalationCount = 0;
        this.reworkCount = 0;
        this.returnedToQueueCount = 0;
    }

    /**
     * Getters and Setters - Setters are mostly setup as incrementers
     */
    public int getTasksCompleted() {
        return tasksCompleted;
    }

    public void incTasksCompleted() {
        this.tasksCompleted++;
    }

    public int getEscalationCount() {
        return escalationCount;
    }

    public void incEscalationCount() {
        this.escalationCount++;
    }

    public int getReworkCount() {
        return reworkCount;
    }

    public void incReworkCount() {
        this.reworkCount++;
    }

    public int getReturnedToQueueCount() {
        return returnedToQueueCount;
    }

    public void incReturnedToQueueCount() {
        this.returnedToQueueCount++;
    }
}
