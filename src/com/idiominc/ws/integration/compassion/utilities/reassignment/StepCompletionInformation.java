package com.idiominc.ws.integration.compassion.utilities.reassignment;

//log4j
import org.apache.log4j.Logger;

//sdk
import com.idiominc.wssdk.workflow.*;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.ui.WSDictionary;

//java
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;
import java.util.Date;
import java.text.MessageFormat;

public class StepCompletionInformation {

    //1 minute
    private static final long _SECONDS_60 = 60L;

    //log
    private static final Logger log = Logger.getLogger(StepCompletionInformation.class);

    public static String getComment(WSTaskStepHistory historyEntry) {
        if(null == historyEntry) {
            log.info("History Entry is null");
            return "";
        }
        return getComment(historyEntry.getUser(), historyEntry.getDescription());
    }

    public static WSWorkflowTransition getWorkflowTransition(WSHumanTaskStep step, WSTaskStepHistory historyEntry) {
        if(null == historyEntry || null == step) {
            log.info("Either history entry or step is null");
            return null;
        }
        String description = historyEntry.getDescription();
        WSWorkflowTransition[] transitions = step.getWorkflowStep().getTransitions();
        for(WSWorkflowTransition transition: transitions) {
            if(description.indexOf(transition.getName()) >= 0) {
                return transition;
            }
        }
        return null;
    }

    public static WSTaskStepHistory getStepHistoryFromTaskStep(WSTask task,
                                                               WSHumanTaskStep hts) {
       WSTaskHistory history = task.getTaskHistory();
       WSTaskStepHistory[] stepsHistory = history.getStepsHistory();
       for(WSTaskStepHistory stepH: stepsHistory) {
          if(stepH.getEventType() == WSTaskStepHistoryEventType.ACTION_COMPLETED && stepH.getDescription() != null) {
             String stepName = getStep(stepH);
             if(null == stepName) {
                continue;
             }
             if(hts.getWorkflowStep().getName().equalsIgnoreCase(stepName.trim())
                &&
                sameDates(hts.getCompletionDate(), stepH.getDate())) {
                   return stepH;
             }
          }
       }
       return null;
    }


    public static WSHumanTaskStep getHumanTaskStepFromHistory(WSTask task,
                                                              WSTaskStepHistory historyEntry) {
        String stepName = getStep(historyEntry);
        for(WSHumanTaskStep hts: task.getHumanSteps()) {
            if(hts.getWorkflowStep().getName().equalsIgnoreCase(stepName.trim())
               &&
               sameDates(hts.getCompletionDate(), historyEntry.getDate())) {
                  return hts;
            }
        }
        return null;
    }

    private static boolean sameDates(Date dt1, Date dt2) {
        if(null == dt1 || null == dt2) {
            return false;
        }
        long diff = Math.abs(dt1.getTime() - dt2.getTime());
        long diffSeconds = diff / 1000 % 60;
        return diffSeconds < _SECONDS_60;
    }

    private static String getStep(WSTaskStepHistory historyEntry) {
        if(null == historyEntry) {
            log.info("History entry is null");
            return null;
        }
        WSUser user = historyEntry.getUser();
        String description = historyEntry.getDescription();
        if(null == user || null == description || 0 == description.length()) {
            log.info("User is not identified or description is not available");
            return null;
        }
        String regexp2 =  getString(user, "engine.taskstep.completed.regexp2");
        if(null == regexp2) {
            return null;
        }
        int index_0 = regexp2.indexOf("{0}");
        int index_1 = regexp2.indexOf("{1}");
        if(index_0 < 0 || index_1 < 0) {
            return null;
        }
        int searchIndex = (index_1 > index_0)? 1:2;
        regexp2 = getFormattedString(regexp2.substring(0, Math.max(index_0,index_1) + 3), user.getDisplayLanguage().getLocale(),
                                     new Object[] {"(.*)", "(.*)"});
        Matcher m = Pattern.compile(regexp2).matcher(description);
        String stepName = null;
        if (m.find())
        {
             stepName = m.group(searchIndex);
        }
        return stepName;
    }

    private static String getComment(WSUser user, String description) {
        if(null == user || null == description || 0 == description.length()) {
            return "";
        }
        String regexp1 =  getFormattedString(user, "engine.taskstep.completed.regexp1",
                              new Object[] {".*", ".*"});
        String regexp2 =  getFormattedString(user, "engine.taskstep.completed.regexp2",
                              new Object[] {".*", ".*"});
        Matcher m = Pattern.compile(regexp1).matcher(description);
        if (m.find()) {
             return description.substring(m.end());
        }
        m = Pattern.compile(regexp2).matcher(description);
        if (m.find()) {
             return description.substring(m.end());
        }
        return "";
    }

    private static WSDictionary getStringDictionary(WSUser user) {
       return user.getDictionary(WSDictionary.CORE_DICTIONARY);
    }

    private static String getFormattedString(WSUser user, String str1, Object[] obj) {
       return getStringDictionary(user).getFormattedString(str1, obj);
    }

    private static String getFormattedString(String message, Locale locale, Object[] args) {
        MessageFormat mf = new MessageFormat(message);
        mf.setLocale(locale);
        return mf.format(args);
    }

    private static String getString(WSUser user, String str1) {
       return getStringDictionary(user).getString(str1);
    }



}
