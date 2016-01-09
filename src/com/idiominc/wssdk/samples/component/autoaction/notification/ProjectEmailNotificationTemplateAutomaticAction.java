/*
 * Copyright (c) 2010 SDL PLC. All Rights Reserved.
 *
 * All company product or service names referenced herein are
 * properties of their respective owners.
 */

package com.idiominc.wssdk.samples.component.autoaction.notification;

/*
 * The following import statements are required to implement any
 * automatic action.
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.idiominc.ws.sdkcore.asset.WSTextSegmentTranslationHistoryImp;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.asset.WSTextSegmentTranslation;
import com.idiominc.wssdk.asset.WSTextSegmentTranslationHistory;
import com.idiominc.wssdk.workflow.WSTask;
import com.idiominc.wssdk.component.WSParameter;
import com.idiominc.wssdk.component.WSParameterFactory;
import com.idiominc.wssdk.component.autoaction.WSActionResult;
import com.idiominc.wssdk.component.autoaction.WSProjectAutomaticAction;

/*
 * Add any additional import statements needed for your automatic
 * action here.
 */
import com.idiominc.wssdk.user.WSUser;
import com.idiominc.wssdk.workflow.WSProject;
import com.idiominc.wssdk.workflow.WSProjectGroup;
import com.idiominc.wssdk.workflow.WSProjectType;
import com.idiominc.wssdk.WSAttributeNotSupportedException;
import com.idiominc.wssdk.notification.WSNotificationManager;
import com.idiominc.wssdk.attribute.WSAttributable;
import com.idiominc.wssdk.attribute.WSAttributeValue;
import com.idiominc.wssdk.attribute.WSFileAttributeValue;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Date;
import java.text.DateFormat;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/**
 * Sends an email with the specified subject and body to the specified
 * user.  Optionally attaches the content of the specified project 
 * attribute to the e-mail.
 *
 * <p>
 * 
 * One example of the use of this feature would be if a workflow contained
 * the sample automatic action for generating translation kits.  One of the
 * parameters of that automatic action  allows the user to set
 * a project attachment to contain the kit that has been generated.
 * Using these two automatic actions on in a workflow together the
 * ProjectEmailAutomaticAction would be able to attach the generated
 * translation kit to the email that is sent out.  Other examples of the use
 * of this feature may include sending out scoping reports when the project
 * has completed.
 *
 * <p>
 *
 * <b> Parameters </b>
 *
 * <p>
 *
 * The action takes the following parameters.
 *
 * <p>
 *
 * <table border = 1>
 *   <tr>
 *     <th> Name </th>
 *     <th> Description </th>
 *     <th> Default Value </th>
 *   </tr>
 *   <tr>
 *     <td> <tt> Send To </tt> </td>
 *     <td> 
 *        A text field that allows the user to specify the email
 *        addresses of the persons to be notified. Multiple email
 *        addresses can be specified in this field by separating
 *        them with simi-colon (;).
 *        <p>
 *        Use ${project.creator.email} to dynamically populate
 *        the project creator's e-mail address.
 *        <p>
 *        If a project attribute contains an email address or addresses
 *        separated by semi-colon, it can be used in this field as well.
 *        See the variable section below for how to specify an attribute
 *        as variable.
 *     </td>
 *     <td> ${project.creator.email} 
 *     </td>
 *   </tr>
 *   <tr>
 *     <td> <tt> Subject </tt> </td>
 *     <td> 
 *        A text field that allows the user to specify the subject
 *        of the email that is sent. 
 *        <p>
 *        Use any of the variables described below to dynamically populate
 *        the subject.
 *     </td>
 *     <td> 
 *        The WorldServer project '${project.name}' is now
 *        complete. 
 *     </td>
 *   </tr>
 *   <tr>
 *     <td> <tt> Message </tt> </td>
 *     <td> 
 *       A text field that allows the user to specify the body of
 *       the email that is sent. 
 *        <p>
 *        Use any of the variables described below to dynamically populate
 *        the body.
 *     </td>
 *     <td>
 *       The WorldServer project '${project.name}' that was
 *       submitted by ${project.creator.username} on 
 *       ${project.creationdate} is now complete.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td> <tt> File Attachment Attribute Name </tt> </td>
 *     <td> If a user has setup a project file attachment attribute, they can
 *          then use this optional parameter.  Set this parameter by specifying
 *          the name of the project file attachment attribute.  The automatic
 *          action will look at value of the specified attribute and try
 *          to attach the file contained in its value to the email.
 *     </td>
 *     <td> No default value </td>
 *   </tr>
 *
 * </table>
 *
 * <p>
 *
 * The "Send To", "Email Subject", and "Email Body" parameters can contain
 * variables 
 * that will allow be dynamically populated when the e-mail is created.  The
 * following table lists the variables that can be used.
 *
 * <p>
 * 
 * <table border = 1>
 *   <tr>
 *     <th> Variable </th>
 *     <th> Description </th>
 *     <th> Example </th>
 *   </tr>
 *   <tr>
 *     <td> ${project.attribute.XXX} </td>
 *     <td> The value of the specified project attribute. </td>
 *     <td> Attribute Value </td>
 *   </tr>
 *   <tr>
 *     <td> ${project.creationdate} </td>
 *     <td> The date that the project was created on. </td>
 *     <td> 2005-01-31 13:50:50 </td>
 *   </tr>
 *   <tr>
 *     <td> ${project.creator.email} </td>
 *     <td> The e-mail address of the user who created the project. </td>
 *     <td> user@company.com </td>
 *   </tr>
 *   <tr>
 *     <td> ${project.creator.firstname} </td>
 *     <td> The first name of the user who created the project. </td>
 *     <td> John </td>
 *   </tr>
 *   <tr>
 *     <td> ${project.creator.lastname} </td>
 *     <td> The last name of the user who created the project. </td>
 *     <td> Smith </td>
 *   </tr>
 *   <tr>
 *     <td> ${project.creator.fullname} </td>
 *     <td> The full name of the user who created the project. </td>
 *     <td> John Smith </td>
 *   </tr>
 *   <tr>
 *     <td> ${project.creator.username} </td>
 *     <td> The username of the user who created the project. </td>
 *     <td> jsmith </td>
 *   </tr>
 *   <tr>
 *     <td> ${project.description} </td>
 *     <td> The description of the project. </td>
 *     <td> Translate all of the application reference guides. </td>
 *   </tr>
 *   <tr>
 *     <td> ${project.duedate} </td>
 *     <td> The date that the project is due on. </td>
 *     <td> 2005-02-31 </td>
 *   </tr>
 *   <tr>
 *     <td> ${project.locale} </td>
 *     <td> The locale that the project is for. </td>
 *     <td> English </td>
 *   </tr>
 *   <tr>
 *     <td> ${project.name} </td>
 *     <td> The name of the project. </td>
 *     <td> Translate Books </td>
 *   </tr>
 * </table>
 *
 * <p>
 *
 * <b> Return Values </b>
 *
 * <p>
 *
 * The automatic action has the following return values.
 *
 * <p>
 *
 * <table border = 1>
 *   <tr>
 *     <th> Name </th>
 *     <th> Description </th>
 *   </tr>
 *   <tr>
 *     <td> <tt> Done </tt> </td>
 *     <td> Returned in all cases. </td>
 *   </tr>
 * </table>
 *
 * <p>
 *
 * <b> Auto-Error Conditions </b>
 *
 * <p>
 *
 * The automatic action will go into auto-error under these conditions:
 * <p>
 *
 * <ul>
 *   <li> The specified project attribute is a value for which no file exists. </li>
 * 
 *   <li> Failure to attach file to the email properly. </li>
 * </ul>
 *
 * <p>
 *
 * <b> Sample Workflow </b>
 *
 * <p>
 
 * This automatic action could be used in a 
 * <a href = "doc-files/ProjectEmailNotificationAutomaticAction.workflow.xml">
 * workflow like this:
 * </a>
 *
 * <img src = "doc-files/ProjectEmailNotificationAutomaticAction.workflow.png">
 * <p>
 *
 * <b> Descriptor </b>
 *
 * <p>
 *
 * This automatic action can be deployed via a 
 * <a href = "doc-files/ProjectEmailNotificationAutomaticAction.desc.xml">
 * descriptor file like this:
 * </a>
 *
 * <p>
 * <IFRAME SRC="doc-files/ProjectEmailNotificationAutomaticAction.desc.xml"
 *    WIDTH="100%">
 *    <a href = "doc-files/ProjectEmailNotificationAutomaticAction.desc.xml">
 *      Sample Descriptor
 *    </a>
 *  </IFRAME>
 *
 *
 *
 * @author <a href="mailto:abonfiglio@idiominc.com">A.Bonfiglio</a>
 *
 * @version WorldServer 8.0.0
 *
 * @since 7.5.1 Service Pack 3
 */
public class ProjectEmailNotificationTemplateAutomaticAction
    extends WSProjectAutomaticAction {

    /**
     * Logger for debugging.
     */
    private static final Logger log =
        Logger.getLogger( ProjectEmailNotificationTemplateAutomaticAction.class );
    
    /**
     * The name of the success return value.
     */
    private static String DONE_RETURN_VALUE = "Done";

    /**
     * The internal name of the e-mail recipient parameter.
     */
    private static final String USER_TO_EMAIL_PARAM_NAME =
        "EMAIL_RECIPIENT";

    /**
     * The external name of the e-mail recipient parameter.
     */
    private static final String USER_TO_EMAIL_PARAM_DESC =
        "Send To";

    /**
     * The default value for the e-mail recipient.
     */
    private static final String USER_TO_EMAIL_PARAM_DEFAULT =
        "${project.creator.email}";

    /**
     * The internal name of the e-mail subject parameter.
     */
    private static final String EMAIL_SUBJECT_PARAM_NAME =
        "EMAIL_SUBJECT";

    /**
     * The external name of the e-mail subject parameter.
     */
    private static final String EMAIL_SUBJECT_PARAM_DESC =
        "Subject";

    /**
     * The default value for the e-mail subject. 
     */
    private static final String EMAIL_SUBJECT_PARAM_DEFAULT =
        "The WorldServer project '${project.name}' is now complete.";

    /**
     * The internal name of the e-mail body parameter.
     */
    private static final String EMAIL_BODY_PARAM_NAME =
        "EMAIL_BODY_TEMPLATE";

    /**
     * The external name of the e-mail body parameter.
     */
    private static final String EMAIL_BODY_PARAM_DESC =
        "Message template file";

    /**
     * The default value for the email body.
     */
    private static final String EMAIL_BODY_PARAM_DEFAULT = "/Customization/email_template.txt";

    /**
     * The internal name of the attachment attribute parameter.
     */
    private static final String PROJECT_FILE_ATTACHMENT_ATTRIBUTE_PARAM_NAME =
        "PROJECT_FILE_ATTACHMENT_ATTRIBUTE_NAME";
    
    /**
     * The internal name of the attachment attribute parameter.
     */
    private static final String PROJECT_FILE_ATTACHMENT_ATTRIBUTE_PARAM_DESC =
        "Attribute Name";

    /**
     * Gets the name of the automatic action.
     *
     * @return  The name of the automatic action.
     */
    public String getName() {
        return "Send E-mail (Template)";
    }

    /**
     * Gets the automatic action version.
     *
     * @return  The automatic action version.
     */
    public String getVersion() {
        return "1.0.1";
    }
    

    /**
     * Gets the detailed description of the automatic action.
     *
     * @return  The detailed description of the automatic action.
     */
    public String getDescription() {
        return ( "A project-scope automatic action component for " +
            "workflow-driven email notification.  By default, the automatic " +
            "action sends email to the creator of a project.  It is also " +
            "configurable to send email to any user, with a configurable " +
            "subject line and body. Message body is defined in a template (text file)." +
            "\n\nIf a user has set up a project-level attachment attribute, " +
            "an optional parameter--when specified--will attach " +
            "the contents of the specified attribute to the email message." +
            "\n\nOne example of the use of this feature " +
            "would be if a workflow contained the sample automatic action " +
            "for generating translation kits.  Using these two automatic " +
            "actions in a workflow " +
            "would attach the generated translation kits for the project " +
            "to the email message that is sent.\n\n" +
            "Other examples of the use of this feature may " +
            "include sending out scoping reports when a projects has " +
            "finished." +
            "\n\nTo insert dynamic values into the message, you can use " +
            "these substitution placeholders in the arguments (other than " +
            "Attribute Name): " +
            "\n${project.attribute.XXX}" +
            "\n${project.creationdate}" +
            "\n${project.creator.email}" +
            "\n${project.creator.firstname}" +
            "\n${project.creator.lastname}" +
            "\n${project.creator.fullname}" +
            "\n${project.creator.username}" +
            "\n${project.description}" +
            "\n${project.duedate}" +
            "\n${project.locale}" +
            "\n${project.projecttype.name}" +
            "\n${project.number}" +
            "\n${project.sourcelocale}" +
            "\n${project.client.name}" +
            "\n${project.workgroup}" +
            "\n${project.name}");

    }


    /**
     * Gets the list of possible auto action parameters.  These
     * parameters will allow the user of this autoaction to specify
     * optional text that will be added to the email notification, who
     * shall recieve the notification, and the subject of the
     * notification.
     *
     * @return list of WSParamters.
     */
    public WSParameter[] getParameters(){

        WSParameter USER_TO_EMAIL_PARAM = 
            WSParameterFactory.createStringParameter
            ( USER_TO_EMAIL_PARAM_NAME,
              USER_TO_EMAIL_PARAM_DESC,
              USER_TO_EMAIL_PARAM_DEFAULT );

        WSParameter EMAIL_SUBJECT_PARAM = 
            WSParameterFactory.createStringParameter
            ( EMAIL_SUBJECT_PARAM_NAME,
              EMAIL_SUBJECT_PARAM_DESC,
              EMAIL_SUBJECT_PARAM_DEFAULT);

        WSParameter EMAIL_BODY_PARAM = 
            WSParameterFactory.createStringParameter
            ( EMAIL_BODY_PARAM_NAME,
              EMAIL_BODY_PARAM_DESC,
              EMAIL_BODY_PARAM_DEFAULT);

        WSParameter ATTRIBUTE_NAME_PARAM =  
            WSParameterFactory.createStringParameter
            ( PROJECT_FILE_ATTACHMENT_ATTRIBUTE_PARAM_NAME,
              PROJECT_FILE_ATTACHMENT_ATTRIBUTE_PARAM_DESC,
              null );
        
        WSParameter [] AUTOACTION_PARAMS = 
          { USER_TO_EMAIL_PARAM,
            EMAIL_SUBJECT_PARAM,
            EMAIL_BODY_PARAM,
            ATTRIBUTE_NAME_PARAM };

        return  ( AUTOACTION_PARAMS );
    }

    /**
     * Automatic Actions are responsible for defining the workflow
     * transition that leads from the workflow step represented by the
     * automatic action to the net workflow step.
     *
     * The following method defines these transitions.  In this case
     * there is only one transition.  This transition represents when
     * the autoaction has completed.
     *
     * @return Comleted transition value is "DONE"
     */
    public String [] getReturns(){
        return new String [] {DONE_RETURN_VALUE};
    }

    /**
     *
     * This automatic action will email the creator of a project once
     * the project has completed.  It is also configurable to email
     * any user with the subject line and email text also being
     * configurable.  It returns the result packaged as a
     * {@link WSActionResult}.
     *
     * <p> The workflow engine will call this method when the workflow
     * reaches the step corresponding to this automatic action.  
     *
     * <p>
     *
     * @param context     The WorldServer SDK Context.
     * @param parameters  A map of auto action parameters and their values.
     * @param tasks       The tasks (part of the same project) for which to
     *                    execute the project-scope automatic step and its
     *                    corresponding action.
     * <p>
     * @return            The result of the auto action execution.
     */
    public WSActionResult execute (WSContext context,
                                   Map parameters, WSTask[] tasks) {

    	log.setLevel(Level.DEBUG);
    	
        /*
         * Figure out who to send the email to.
         */
        String userToNotify =
            (String) parameters.get(USER_TO_EMAIL_PARAM_NAME);
        if ( ( userToNotify == null ) || 
             ( userToNotify.length() ) == 0 ) {
            
            return new WSActionResult
              ( WSActionResult.ERROR, 
                "Missing parameter: " + USER_TO_EMAIL_PARAM_DESC );
        }
        
        /*
         * Figure out the email subject. 
         */
        String emailSubject =
            (String) parameters.get(EMAIL_SUBJECT_PARAM_NAME);
        if ( emailSubject == null ) {
            emailSubject = EMAIL_SUBJECT_PARAM_DEFAULT;
        }
        
        /*
         * Figure out the email body. 
         */
        String emailTemplate = (String) parameters.get(EMAIL_BODY_PARAM_NAME);
        if (StringUtils.isNullOrEmpty(emailTemplate)) {
        	return new WSActionResult(WSActionResult.ERROR, "Email body template parameter cannot be empty.");
        }
        
        File _templateFile = null;
        try {
	        _templateFile = context.getAisManager().getNode(emailTemplate).getFile();
	        if(_templateFile == null) {
	        	return new WSActionResult(WSActionResult.ERROR, "AIS Manager could not get the file from node: " + emailTemplate);
	        }
	        log.debug("Template file: " + _templateFile.getAbsolutePath());
		} catch (Exception e) {
			log.error(e);
        	return new WSActionResult(WSActionResult.ERROR, "Could not get email template file: " + e.getMessage());
		}
        

        String projectFileAttachmentAttribute = (String)
            parameters.get(PROJECT_FILE_ATTACHMENT_ATTRIBUTE_PARAM_NAME);
        if ( projectFileAttachmentAttribute == null ) {
            projectFileAttachmentAttribute = "";
        }
        
        // Check to see that there are tasks in this project.  This
        // should always be the case
        if ( tasks.length < 1 ) {
            return new WSActionResult(WSActionResult.ERROR, 
                                      "There are no tasks in the project");
        }

        // Replace any parameters in the strings.
        ParameterReplacer replacer = new ParameterReplacer( context, tasks );
        userToNotify = replacer.replaceParameters( userToNotify );
        emailSubject = replacer.replaceParameters( emailSubject );
        
        log.debug("userToNotify: " + userToNotify);
        log.debug("emailSubject: " + emailSubject);
        
        String emailText;
        try {
        	emailText = getEmailText(_templateFile, replacer);
		} catch (Exception e) {
        	return new WSActionResult(WSActionResult.ERROR, "Could not read from template file: " + e.getMessage());
		}
        
        // Grab the attachement values if they have been specified.
        File [] attachments = null;
        if ( ( projectFileAttachmentAttribute != null ) &&
             ( ! projectFileAttachmentAttribute.equals( "" ) ) ) {

            //User has specified attachmentFileAttribute.  Get its value.
            WSProject project = tasks[0].getProject();

            try {
                WSAttributeValue attributeValue =
                    project.getAttributeValue( projectFileAttachmentAttribute );
                if ( ! ( attributeValue instanceof WSFileAttributeValue ) ) {
                   return new WSActionResult
                       ( WSActionResult.ERROR,
                         "Specified attribute (" + projectFileAttachmentAttribute
                         + ") is not an attachment attribute.");
                }
                   
                if ( attributeValue != null ) {
                   attachments = ((WSFileAttributeValue)attributeValue).getValues();
                }
            }
            catch ( WSAttributeNotSupportedException e ) {
                // This means the attribute specified does not exist.
                return new WSActionResult
                    ( WSActionResult.ERROR,
                      "Specified attribute (" + projectFileAttachmentAttribute
                      + ") does not exist for the project.");
            }
        }
        
        WSNotificationManager notification = 
          context.getNotificationManager();
        if ( attachments == null ) {
            // No attachment attribute specified so send email without
            // attachment.
            notification.sendNotification( userToNotify, 
					   emailSubject, 
					   emailText);
        }
        else {
            // Send the e-mail with the attachment
            notification.sendNotification( userToNotify, emailSubject, 
					   emailText, attachments );
        }
        
        return new WSActionResult(DONE_RETURN_VALUE, 
                                  "E-mail sent to: '" + userToNotify + "'");
    }

    private String getEmailText(File templateFile, ParameterReplacer replacer) throws IOException {
    
    	log.debug("Trying to read lines from template file: " + templateFile.getAbsolutePath());
    	
    	List<String> _lines = FileUtils.readLinesFromFile(templateFile);
    	List<String> _replacedLines = new ArrayList<String>();
    	
    	String _newLine = "";
    	for (String _line : _lines) {
    		log.debug("Replacing paramters in line: " + _line);
    		_newLine = replacer.replaceParameters(_line);
    		log.debug("New line: " + _newLine);
    		_replacedLines.add(_newLine);
		}
    	
    	return StringUtils.join(_replacedLines, System.getProperty("line.separator"));
    }

    /**
     * Copies source file to destination file.  Used to rename the file
     * that is sent in the email to a more user friendly name.
     * @param source
     * @param target
     * @return true on success
     */
     private boolean copyFile( File source, File target ) {

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try{
            inputStream = new FileInputStream(source);
            outputStream = new FileOutputStream(target);

            //Needed to transfer bytes from the inputstream to the
            //output stream.
            byte[] buffer = new byte[1024];
            int len;

            //Write to outputstream in 1024 byte chunks.
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }

            return true;
        }
        //Catch and log any File IO exception
        catch(IOException e){
            log.error("ERROR: Creating TKit.  The exception is: "+ e);
            return false;
        }
        finally {
            //Close streams, they are no longer needed.
            try {
                if ( null != inputStream ) {
                    inputStream.close();
                }
                if ( null != outputStream ) {
                    outputStream.close();
                }
            }
            catch ( IOException ioe ) {
                // Just eat the exception since we are already in the
                // finally clause
            }
        }
    }
     
}



class FileUtils
{
	public static void writeLinesToFile(File aFile, List<String> lines) throws IOException {

        WSTextSegmentTranslationHistoryImp hist;


		BufferedWriter _bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(aFile), Charset.forName("UTF-8")));
		
		for(String _line : lines) {
			_bufferedWriter.write(_line);
			_bufferedWriter.newLine();
		}
		_bufferedWriter.flush();
		_bufferedWriter.close();
	}
	
	public static List<String> readLinesFromFile(File aFile) throws IOException {
		
		BufferedReader _bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(aFile), Charset.forName("UTF-8")));
		List<String> _lines = new ArrayList<String>();
		
		String _line = null;
		while((_line = _bufferedReader.readLine()) != null) {
			_lines.add(_line);
		}
		_bufferedReader.close();
		return _lines;
	}
}

class StringUtils 
{
	public static boolean isNullOrEmpty(String string) {
		
		return string == null || string.isEmpty();
		
	}

	public static List<String> replaceInLines(List<String> lines, Map<String, String> replacements) {
		
		List<String> _linesWithReplacements = new ArrayList<String>();
		
		for(String _line : lines) {
			String _replaceLine = _line;
			for(Entry<String, String> _entry : replacements.entrySet()) {
				_replaceLine = _replaceLine.replace(_entry.getKey(), _entry.getValue());
			}
			_linesWithReplacements.add(_replaceLine);
		}
		
		return _linesWithReplacements;
	}
	
	public static String join(List<String> lines, String separator) {
		
		StringBuilder _stringBuilder = new StringBuilder();
		
		for(String _line : lines) {
			_stringBuilder.append(_line);
			_stringBuilder.append(separator);
		}
		
		return _stringBuilder.toString();
	}	
}
