package com.idiominc.ws.integration.compassion.utilities.twostepproject;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.user.WSWorkgroup;
import com.idiominc.wssdk.user.WSLocale;
import com.idiominc.wssdk.WSRunnable;
import com.idiominc.wssdk.workflow.WSProjectGroup;
import com.idiominc.wssdk.workflow.WSWorkflow;
import com.idiominc.wssdk.workflow.WSWorkflowPrintingListener;
import com.idiominc.wssdk.ais.WSNode;
import com.idiominc.wssdk.ais.WSAisException;

//java
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.ArrayList;

//apache
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Helper class to instantiate spawn-off project to create second-step projects.
 *
 * @author SDL Professional Services
 */
public class CustomProjectCreator  implements WSRunnable {

    private int            _groupID;
    private String         _name;
    private String         _description;
    private int            _localeID;
    private List<String>   _paths;
    private int            _workgroupID;
    private int            _workflowID;
    private String         _message;
    private Writer         _stringWriter;

    /**
     * Get state of the object
     * @return Object's state
     */
    public String toString() {
      return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    /**
     * Add (new) source node path. The node will become a source node in the instantiated project
     * @param path - node path
     */
    public void add(final String path) {
        _paths.add(path);
    }

    /**
     * Obtains project group ID of the newly created project
     * @return project ID
     * @throws TwoStepProjectException - if project creation failed
     */
    public int getProjectGroupId() throws TwoStepProjectException
    {
        if(null != _message) {
           throw new TwoStepProjectException("Invalid object state: " + _message);
        }
        if(_groupID <= 0) {
            throw new TwoStepProjectException("Project Creation Error: " +_stringWriter.toString());
        }
        return _groupID;
    }

    /**
     * Conversion constructor
     * @param name - project name
     * @param description - project description
     * @param localeID - source locale ID
     * @param workgroupID - workgroup ID
     * @param workflowID - workflow ID
     */
    public CustomProjectCreator(final String name,
                                final String description,
                                final int localeID,
                                final int workgroupID,
                                final int workflowID
                                ) {
        this._workgroupID = workgroupID;
        this._workflowID  = workflowID;
        this._name = name;
        this._description = description;
        this._localeID = localeID;
        this._stringWriter = new StringWriter();
        this._paths = new ArrayList<String>();
        this._message = null;

    }

    /**
     * Runs project creation in a separate context
     * @param wsContext - worldServer context
     * @return true if run is succesful
     */
    public boolean run(WSContext wsContext) {
        try {
          wsContext.getWorkflowManager().addWorkflowListener(new WSWorkflowPrintingListener(new PrintWriter(_stringWriter)));
        } catch(Exception e) {
            _message = e.getLocalizedMessage();
            return true;
        }
        /*
         NOTE: *every* WS object you use in inner transaction has to be reloaded before calling createProjectGroup; clients, project-type, locale, etc.
         Don't pass any objects between transactions.
        */
        WSWorkgroup workgroup = wsContext.getUserManager().getWorkgroup(_workgroupID);
        WSWorkflow workflow   = wsContext.getWorkflowManager().getWorkflow(_workflowID);
        WSLocale targetLocale = wsContext.getUserManager().getLocale(_localeID);
        if(_name == null || workgroup == null && targetLocale == null || workflow == null) {
            _message = this.toString();
            return true;
        }
        List<WSNode> nodes = new ArrayList<WSNode>();
        for(String strPath: _paths) {
          try {
            WSNode node = wsContext.getAisManager().getNode(strPath);
            if(node != null) {
                nodes.add(node);
            }
          }  catch(WSAisException e) {}
        }
        if(nodes.size() > 0) {
           WSProjectGroup group = wsContext.getWorkflowManager().createProjectGroup(
                _name,
                _description,
                workgroup,
                new WSLocale[] {targetLocale},
                nodes.toArray(new WSNode[nodes.size()]),
                workflow);
            _groupID = (group == null) ? -1: group.getId();
        } else {
            _message = "Can't find any valid asset nodes";
        }
        return true;
    }
}
