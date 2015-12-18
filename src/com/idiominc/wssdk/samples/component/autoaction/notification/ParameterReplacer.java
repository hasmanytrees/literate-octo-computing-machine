package com.idiominc.wssdk.samples.component.autoaction.notification;

import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.workflow.WSTask;
import com.idiominc.wssdk.user.WSClient;
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.user.WSWorkgroup;
import com.idiominc.wssdk.workflow.WSProject;
import com.idiominc.wssdk.workflow.WSProjectGroup;
import com.idiominc.wssdk.workflow.WSProjectType;
import com.idiominc.wssdk.WSAttributeNotSupportedException;
import com.idiominc.wssdk.notification.WSNotificationManager;
import com.idiominc.wssdk.attribute.WSAttributable;
import com.idiominc.wssdk.attribute.WSAttributeValue;
import com.idiominc.wssdk.attribute.WSFileAttributeValue;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Date;
import java.text.DateFormat;

/**
 * Helper class that knows how to substitute various parameters int
 * strings with actual values.
 */
public class ParameterReplacer 
{
    /**
     * The variable that represents the e-mail address of the 
     * user who created the project.
     */
    public static final String PROJECT_CREATOR_EMAIL_PARAM = 
        "${project.creator.email}";
    
    /**
     * The variable that represents the first name of the 
     * user who created the project.
     */
    public static final String PROJECT_CREATOR_FIRSTNAME_PARAM = 
        "${project.creator.firstname}";

    /**
     * The variable that represents the last name of the 
     * user who created the project.
     */
    public static final String PROJECT_CREATOR_LASTNAME_PARAM = 
        "${project.creator.lastname}";
    
    /**
     * The variable that represents the full name of the 
     * user who created the project.
     */
    public static final String PROJECT_CREATOR_FULLNAME_PARAM = 
        "${project.creator.fullname}";
    
    /**
     * The variable that represents the username of the 
     * user who created the project.
     */
    public static final String PROJECT_CREATOR_USERNAME_PARAM = 
        "${project.creator.username}";

    /**
     * The variable that represents the date that the project
     * was created.
     */
    public static final String PROJECT_CREATION_DATE_PARAM = 
        "${project.creationdate}";

    /**
     * The variable that represents the due date for the project.
     */
    public static final String PROJECT_DUE_DATE_PARAM = 
        "${project.duedate}";

    /**
     * The variable that represents the locale for the project.
     */
    public static final String PROJECT_LOCALE_PARAM = 
        "${project.locale}";

    /**
     * The variable that represents the name for the project
     * group that the project is part of.
     */
    public static final String PROJECT_NAME_PARAM = 
        "${project.name}";

    /**
     * The variable that represents the description for the project
     * group that the project is part of.
     */
    public static final String PROJECT_DESCRIPTION_PARAM = 
        "${project.description}";

    /**
     * The prefix for the variable that represents a project attribute
     * to lookup.
     */
    public static final String PROJECT_ATTRIBUTE_PARAM_PREFIX = 
        "${project.attribute.";
    
    public static final String PROJECT_PROJECTTYPE_NAME = "${project.projecttype.name}";
    
    public static final String PROJECT_NUMBER = "${project.number}";
    
    public static final String PROJECT_SOURCE_LOCALE = "${project.sourcelocale}";
    
    public static final String PROJECT_CLIENT_NAME = "${project.client.name}";
    
    public static final String PROJECT_WORKGROUP = "${project.workgroup}";
    		
    private WSProject project;
    private WSUser projectCreator;
    private WSProjectGroup projectGroup;
    private WSProjectType projectType;
    
    ParameterReplacer( WSContext context, WSTask[] tasks ) 
    {
        project = tasks[0].getProject();
        projectCreator = project.getCreator();
        projectGroup = project.getProjectGroup();
        projectType = project.getProjectType();
    }

    String replaceParameters( String inputString )
    {
        StringBuffer outputString = new StringBuffer();
        
        // Create a pattern that will look for strings that look like
        // "${...}"
        Pattern p = Pattern.compile( "\\$\\{[^\\}]+\\}" );
        Matcher m = p.matcher( inputString );

        int lastChar = 0;
        String currentPattern;
        String replacementValue;
        while ( m.find() ) {

            // Copy everything from the last character up to the
            // start character to the output.
            if ( m.start() > lastChar ) {
                outputString.append( inputString.substring( lastChar, 
                                                            m.start() ) );
            }
                                 
            currentPattern = inputString.substring( m.start(), m.end() );
            lastChar = m.end();

            replacementValue = handlePattern( currentPattern );

            outputString.append( replacementValue );            
        }

        // Add on the remaining part of the string.
        if ( inputString.length() > lastChar ) {
            outputString.append( inputString.substring( lastChar, 
                                                        inputString.length() ) );
        }
        
        
        //System.out.println( "Returning String: " + outputString );
        return( outputString.toString() );
    }
    
    private String handlePattern( String currentPattern )
    {
        String replacementValue;
        
        // Handle the pattern
        if ( PROJECT_CREATOR_EMAIL_PARAM.equals( currentPattern ) ) {
            replacementValue = projectCreator.getEmail();
        }
        else if ( PROJECT_CREATOR_FIRSTNAME_PARAM.equals( currentPattern ) ) {
            replacementValue = projectCreator.getFirstName();
        }
        else if ( PROJECT_CREATOR_LASTNAME_PARAM.equals( currentPattern ) ) {
            replacementValue = projectCreator.getLastName();
        }
        else if ( PROJECT_CREATOR_FULLNAME_PARAM.equals( currentPattern ) ) {
            replacementValue = projectCreator.getFullName();
        }
        else if ( PROJECT_CREATOR_USERNAME_PARAM.equals( currentPattern ) ) {
            replacementValue = projectCreator.getUserName();
        }
        else if ( PROJECT_CREATION_DATE_PARAM.equals( currentPattern ) ) {
            Date date = project.getCreationDate();
            replacementValue = DateFormat.getDateTimeInstance().format(date);
        }
        else if ( PROJECT_DUE_DATE_PARAM.equals( currentPattern ) ) {
            Date date = project.getDueDate();
            replacementValue = date !=null ? DateFormat.getDateTimeInstance().format(date) : "N/A";
        }
        else if ( PROJECT_LOCALE_PARAM.equals( currentPattern ) ) {
            replacementValue = project.getTargetLocale().getName();
        }
        else if ( PROJECT_NAME_PARAM.equals( currentPattern ) ) {
            replacementValue = projectGroup.getName();
        }
        else if ( PROJECT_DESCRIPTION_PARAM.equals( currentPattern ) ) {
            replacementValue = projectGroup.getDescription();
        }
        else if (PROJECT_PROJECTTYPE_NAME.equals(currentPattern)) {
        	replacementValue = projectType != null ? projectType.getName() : "N/A";
        }
        else if (PROJECT_NUMBER.equals(currentPattern)) {
        	replacementValue = Integer.toString(project.getId());
        }
        else if (PROJECT_SOURCE_LOCALE.equals(currentPattern))	{
        	replacementValue = project.getSourceLocale().getName();
        }
        else if (PROJECT_CLIENT_NAME.equals(currentPattern)) {
        	WSClient _client = project.getClient();
        	replacementValue = _client != null ? _client.getName() : "N/A";
        }
        else if (PROJECT_WORKGROUP.equals(currentPattern))	{
        	WSWorkgroup _workgroup = projectType != null ? projectType.getWorkgroup() : null;
        	replacementValue = _workgroup != null ? _workgroup.getName() : "N/A";
        	
        }
        else if ( currentPattern.startsWith( PROJECT_ATTRIBUTE_PARAM_PREFIX ) ) {
            String attributeName = "";
            try {
                attributeName =
                    currentPattern.substring
                    ( PROJECT_ATTRIBUTE_PARAM_PREFIX.length(),
                      currentPattern.length() - 1 );
                replacementValue = project.getAttribute( attributeName );
            }
            catch ( WSAttributeNotSupportedException e ) {
                replacementValue = "<Unknown attribute " + 
                    attributeName + ">";
            }
        }
        else {
            replacementValue = "UNKNOWN_PARAMETER";
        }
        
        return( replacementValue );
    }

}