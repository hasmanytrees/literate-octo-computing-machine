package com.idiominc.ws.integration.compassion.autoaction;

//internal dependencies
import com.idiominc.ws.integration.compassion.utilities.twostepproject.*;
import com.idiominc.ws.integration.compassion.utilities.metadata.AttributeValueIdentifier;
import com.idiominc.ws.integration.compassion.utilities.metadata.Enumeration_Attributes;
import com.idiominc.ws.integration.compassion.utilities.metadata.MetadataException;

//profserv
import com.idiominc.ws.integration.profserv.commons.wssdk.autoaction.WSCustomProjectAutomaticActionWithParameters;
import com.idiominc.ws.integration.profserv.commons.wssdk.exceptions.WSInvalidParameterException;
import com.idiominc.ws.integration.profserv.commons.wssdk.WSAisUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.XML;
import com.idiominc.ws.integration.profserv.commons.FileUtils;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSContextManager;
import com.idiominc.wssdk.ais.WSNode;
import com.idiominc.wssdk.ais.WSAisException;
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.user.WSWorkgroup;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.component.WSParameter;
import com.idiominc.wssdk.component.WSParameterFactory;
import com.idiominc.wssdk.component.autoaction.WSActionResult;
import com.idiominc.wssdk.workflow.*;

//log4j
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

//dom, xpath, and java
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.*;
import java.io.IOException;
import java.io.Writer;
import java.io.OutputStreamWriter;

/**
 * An automatic action to identify and create two-step project
 */
public class CreateTwoStepProject extends WSCustomProjectAutomaticActionWithParameters {

    //log
    private static Logger log = Logger.getLogger(CreateTwoStepProject.class);
    static {
        log.setLevel(Level.INFO);
    }

    //transition
    private static final String _TRANSITION_CREATED = "Created";
    private static final String _TRANSITION_SKIPPED = "Skipped";

    //parameters
    private final static String _PARAMFILTERGROUP = "PARAMFILTERGROUP";
    private final static String _ROOT = "ROOT";
    private final static String _WORKFLOW = "WORKFLOW";
    private String workflowName;
    private String filterGroup;
    private String root;


    //constants
    private final static String _BENEFICIARY_TO_SUPPORTER = "Beneficiary To Supporter";
    private final static String _PREFIX = "[Second Step Project] ";
    private final static String _PAGESXPATH = "SBCCommunicationDetails/Pages";
    private final static String _ENGLISHNODENAME = "EnglishTranslatedText";
    private final static String _TARGETNODENAME = "TranslatedText";
    private final static String _TWOSTEPPROCESS_ATTR = "TwoStepProcess";

     /**
     * Load AA parameters into the variables
     * @param parameters - map of the parameters
     * @throws WSInvalidParameterException - exepction
     */
    public void preLoadParameters(Map parameters) throws WSInvalidParameterException {
        filterGroup = preLoadParameter(parameters, _PARAMFILTERGROUP, true);
        root = preLoadParameter(parameters, _ROOT, true);
        workflowName = preLoadParameter(parameters, _WORKFLOW, true);
    }

    /**
     * Obtain the array of valid transisitons out of AA
     * @return Transitions
     */
    public String[] getReturns() {
        return new String[]{_TRANSITION_CREATED, _TRANSITION_SKIPPED};
    }

    /**
     * Get AA version
     * @return version number
     */
    public String getVersion() {
        return "1.01";
    }

    /**
     * Obtain AA name
     * @return AA name
     */
    public String getName() {
        return "Create second step project";
    }

    /**
     * Obtain AA description
     * @return AA description
     */
    public String getDescription() {
        return "Identify and create Two-Step Project";
    }

    /**
     * Creates parameters for this AA
     * @return  Array of parameters
     */
    public WSParameter[] getParameters() {
        return new WSParameter[]{
                WSParameterFactory.createStringParameter(_PARAMFILTERGROUP, "Filter Group", ""),
                WSParameterFactory.createStringParameter(_ROOT, "Root position for new target", ""),
                WSParameterFactory.createStringParameter(_WORKFLOW, "Workflow", "")
        };
    }

    /**
     * Execute Automatic Action
     * @param context - WS Context
     * @param tasks - project's tasks
     * @return Execution Results
     */
    public WSActionResult execute(WSContext context, WSAssetTask[] tasks) {

        //required data for project instantiation
        WSProject p = tasks[0].getProject();
        String desiredWorkgroupName;
        WSLocale targetLocale = p.getTargetLocale();
        Map<String,List<String>> affectedTasks = new HashMap<String, List<String>>();
        Map<String,Integer> wgs = new HashMap<String, Integer>();

        //first, validate parameters passed to this automatic action
        if(null == context.getLinguisticManager().getFilterGroup(filterGroup)) {
            return new WSActionResult(WSActionResult.ERROR, "Not configured filter group "
                                      + filterGroup);
        }
        WSWorkflow workflow = context.getWorkflowManager().getWorkflow(workflowName);
        if(null == workflow) {
            return new WSActionResult(WSActionResult.ERROR, "Not configured workflow "
                                      + workflowName);
        }
        WSWorkgroup workgroup;


        try
        {

           //second, check the availability of root
           if(null == root) root = "";
           if(!root.endsWith("/")) {
               root = root + "/";
           }
           if(!root.startsWith("/")) {
               root = "/" + root;
           }
           if(null == context.getAisManager().getNode(root)) {
               throw new TwoStepProjectException("Can't access root node " + root);
           }

          for(WSAssetTask t: tasks) {

              WSNode targetAssetNode = t.getTargetAisNode();

              if(null == targetAssetNode) {
                  log.error("No target node found for asset " + t.getSourcePath());
                  continue;
              }

              Document targetAsset = AttributeValueIdentifier.init(targetAssetNode.getFile());
              String desiredTranslationLocale = AttributeValueIdentifier.getValue(targetAsset, Enumeration_Attributes.TranslationLanguage.getXPath());
              WSLocale desiredLocale = context.getUserManager().getLocale(desiredTranslationLocale);

              if(null == desiredLocale) {
                  throw new TwoStepProjectException("Translation locale is not configured: " + desiredTranslationLocale);
              }

              String optInForLanguageTranslation = AttributeValueIdentifier.getValue(targetAsset, Enumeration_Attributes.GlobalPartnerSetup.getXPath());
              String direction = AttributeValueIdentifier.getValue(targetAsset, Enumeration_Attributes.Direction.getXPath());

              //figure out desired workgroup name
              String foName = AttributeValueIdentifier.getValue(targetAsset, Enumeration_Attributes.FieldOfficeName.getXPath());
              String gpID = AttributeValueIdentifier.getValue(targetAsset, Enumeration_Attributes.GlobalPartnerId.getXPath());

              if(_BENEFICIARY_TO_SUPPORTER.equalsIgnoreCase(direction)) {
                  desiredWorkgroupName = "GP_" + gpID;
              } else {
                  desiredWorkgroupName = "FO_" + foName;
              }

              workgroup = context.getUserManager().getWorkgroup(desiredWorkgroupName);

              if(null == workgroup) {
                  log.error("Workgroup " + desiredWorkgroupName + " was not configured!!");
                  throw new TwoStepProjectException("Can't find workgroup " + desiredWorkgroupName);
              }

              if(targetLocale.getName().equalsIgnoreCase(desiredTranslationLocale)) {
                  log.info("Task for asset " + t.getSourcePath() + " is not qualified for 2-step project: " +
                           "target locale for the project matches the ultimate desired locale " + desiredTranslationLocale);
                  continue;
              }
              if(_BENEFICIARY_TO_SUPPORTER.equalsIgnoreCase(direction)) {
                  if("FALSE".equalsIgnoreCase(optInForLanguageTranslation)) {
                      log.info("Task direction is " + direction + " and the Opt-In flag is FALSE; task for asset " +
                                 t.getSourcePath() + " is not qualified for 2-step project");
                      continue;
                  }
              }

              if(!affectedTasks.containsKey(desiredTranslationLocale)) {
                  affectedTasks.put(desiredTranslationLocale, new ArrayList<String>());
              }
              affectedTasks.get(desiredTranslationLocale).add(t.getTargetPath());

              if(!wgs.containsKey(desiredTranslationLocale)) {
                  wgs.put(desiredTranslationLocale, workgroup.getId());
              }

              //create new target node!
              WSNode baseLocaleNode =  WSAisUtils.getBaseLocaleNode(context,
                                                                    t.getTargetAisNode(),
                                                                    p.getTargetLocale());
              if(null == baseLocaleNode) {
                  throw new TwoStepProjectException("No base locale node for the target is found!");
              }

              TargetNodeCreator tnc = new TargetNodeCreator(root,
                                                            desiredWorkgroupName,
                                                            t.getTargetAisNode().getParent().getPath(),
                                                            baseLocaleNode.getPath(),
                                                            //ProjectCreationUtilities.getLanguageCode(desiredLocale.getLanguage()),
                                                            desiredLocale.getName(),
                                                            desiredLocale.getId(),
                                                            filterGroup,
                                                            false);

              WSContextManager.runAsUser(context, p.getCreator(), tnc);
              //throw exception if we failed!
              tnc.checkStatus();

              //copy english-translated text to translated text
              copyNodesContent(targetAsset);
              Writer writer = new OutputStreamWriter(targetAssetNode.getOutputStream(), "UTF-8");
              try {
                XML.serialize(targetAsset, writer);
              } catch (Exception e) {
                  log.error(e);
                  return new WSActionResult(WSActionResult.ERROR, e.getLocalizedMessage());
              }
              FileUtils.close(writer);

          }

          //create new projects
          for(String locale: affectedTasks.keySet()) {

              if(!wgs.containsKey(locale)) {
                 throw new TwoStepProjectException("Can't access workgroup for locale " + locale);
              }

              CustomProjectCreator cpc = new CustomProjectCreator(p.getProjectGroup().getName(),
                                                                  _PREFIX + p.getProjectGroup().getDescription(),
                                                                  context.getUserManager().getLocale(locale).getId(),
                                                                  wgs.get(locale),
                                                                  workflow.getId());
              for(String path:affectedTasks.get(locale) ) {
                  cpc.add(path);
              }

              WSContextManager.runAsUser(context, p.getCreator(), cpc);
              int projectGroupID = cpc.getProjectGroupId();
              WSProjectGroup pgCreated = context.getWorkflowManager().getProjectGroup(projectGroupID);
              if(null == pgCreated) {
                  throw new TwoStepProjectException("Can't retrieve project group by ID " + projectGroupID);
              }
              WSProject createdProject = pgCreated.getProjects()[0];
              createdProject.setAttributes(p.getAttributes());

              //Add the two-step process project status for added clarification
              createdProject.setAttribute(_TWOSTEPPROCESS_ATTR, _PREFIX);
           }

       } catch(TwoStepProjectException e) {
            log.error(e.getLocalizedMessage());
            return new WSActionResult(WSActionResult.ERROR, e.getLocalizedMessage());
       } catch (MetadataException e) {
            log.error(e.getLocalizedMessage());
            return new WSActionResult(WSActionResult.ERROR, e.getLocalizedMessage());
        } catch(WSAisException e) {
            log.error(e.getLocalizedMessage());
            return new WSActionResult(WSActionResult.ERROR, e.getLocalizedMessage());
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
            return new WSActionResult(WSActionResult.ERROR, e.getLocalizedMessage());
        }

        if(affectedTasks.keySet().size() > 0) {
          return new WSActionResult(_TRANSITION_CREATED, "Instantiated "
                                  + affectedTasks.keySet().size() +
                                  " two-step project(s)");
        } else {
            return new WSActionResult(_TRANSITION_SKIPPED, "No second step projects were created");
        }

    }

    /**
     *  Copy English text to Translated Text
     * @param targetAsset - target asset document
     * @throws TwoStepProjectException - XPATH search exception
     */
    private void copyNodesContent(Document targetAsset) throws TwoStepProjectException {
      try {

        NodeList pages = (NodeList) AttributeValueIdentifier.performXPathQuery(targetAsset,
                                                                               _PAGESXPATH,
                                                                               XPathConstants.NODESET);
        //iterate through pages
        for (int i = 0; null != pages && i < pages.getLength(); i++) {
            NodeList pagesChildren = pages.item(i).getChildNodes();
            Element targetNode;
            Queue<String> fifo = new LinkedList<String>();
            for(int j = 0; null != pagesChildren && j < pagesChildren.getLength(); j++) {
              if(pagesChildren.item(j).getNodeType() == Node.ELEMENT_NODE) {
                 Element element = (Element)pagesChildren.item(j);
                 if(element.getNodeName().trim().equals(_ENGLISHNODENAME)) {
                   fifo.add(element.getTextContent());
                 }
                 if(element.getNodeName().trim().equals(_TARGETNODENAME)) {
                   targetNode = element;
                   if(fifo.isEmpty()) {
                       throw new TwoStepProjectException("Invalid document structure: "
                                                         + _ENGLISHNODENAME + " does not preceed " + _TARGETNODENAME);
                   }
                   String content = fifo.remove();
                   targetNode.setTextContent(content);
                 }
              }
            }
            if(!fifo.isEmpty()) {
                throw new TwoStepProjectException("Invalid document structure: "
                                                  + _ENGLISHNODENAME + " > " + _TARGETNODENAME);
            }
        }
      } catch (XPathExpressionException e) {
        throw new TwoStepProjectException(e.getLocalizedMessage());
      }

    }

}