package com.idiominc.ws.integration.compassion.utilities.twostepproject;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSRunnable;
import com.idiominc.wssdk.linguistic.WSFilterGroup;
import com.idiominc.wssdk.ais.WSAisException;
import com.idiominc.wssdk.ais.WSNode;
import com.idiominc.wssdk.ais.WSSystemPropertyKey;
import com.idiominc.wssdk.user.WSLocale;

//profserv
import com.idiominc.ws.integration.profserv.commons.wssdk.WSAisUtils;

//apache
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class TargetNodeCreator implements WSRunnable {

    private String            _oldTargetFolderNodePath;
    private String            _newTargetFolderNodePath;
    private String            _newTargetLocaleFolderNodePath;
    private int               _newTargetLocaleID;
    private String            _filterGroup;
    private String            _message;

    /**
     * Get state of the object
     * @return Object's state
     */
    public String toString() {
      return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    /**
     * Check status of the new target folder node creation
     * @throws TwoStepProjectException - the new target node folder was not created
     */
    public void checkStatus() throws TwoStepProjectException {
        if(_message != null) {
            throw new TwoStepProjectException(_message + " " + this.toString());
        }
    }

    /**
     *  Conversion constructor
     * @param root - path to the root folder for target translation
     * @param workgroup - workgroup name
     * @param oldTargetFolderNodePath - full path to folder that has the translated document (stage 1)
     * @param oldTargetBaseLocaleNodePath  - full path to locale folder for transled document (stage 1)
     * @param newLocale  - abbreviation of the locale folder for stage 2 target node
     * @param newTargetLocaleID - locale ID for stage 2 target
     * @param filterGroup  - filter group
     * @param placeWorkgroupBeforeLocale - true if workgroup info should be placed above the locale
     */
    public TargetNodeCreator(String root,
                             String workgroup,
                             String oldTargetFolderNodePath,
                             String oldTargetBaseLocaleNodePath,
                             String newLocale,
                             int newTargetLocaleID,
                             String filterGroup,
                             boolean placeWorkgroupBeforeLocale
                                 ) {
        _oldTargetFolderNodePath = oldTargetFolderNodePath;
        String tail = "";
        if (oldTargetFolderNodePath.length() > oldTargetBaseLocaleNodePath.length()) {
          tail = oldTargetFolderNodePath.substring(oldTargetBaseLocaleNodePath.length());
        }
        if(tail.length() > 0 && !tail.startsWith("/")) {
            tail = "/" + tail;
        }
        if(placeWorkgroupBeforeLocale) {
          _newTargetLocaleFolderNodePath = root + workgroup + "/" + newLocale;
          _newTargetFolderNodePath = _newTargetLocaleFolderNodePath + tail;
        } else {
            _newTargetLocaleFolderNodePath = root + newLocale;
            _newTargetFolderNodePath = _newTargetLocaleFolderNodePath + tail;
        }
        _newTargetLocaleID = newTargetLocaleID;
        _filterGroup    =  filterGroup;
        _message = null;
    }


    /**
     *  Creates new target folder node
     * @param wsContext -WS Context
     * @return true
     */
    private boolean createNode(WSContext wsContext) {

        try{

            WSNode oldTargetFolderNode   = wsContext.getAisManager().getNode(_oldTargetFolderNodePath);
            WSLocale newTargetLocale = wsContext.getUserManager().getLocale(_newTargetLocaleID);
            WSFilterGroup fg = wsContext.getLinguisticManager().getFilterGroup(_filterGroup);

            if(null == oldTargetFolderNode ||  null == newTargetLocale) {
                _message = "Invalid object state";
                return true;
            }

            WSNode newTargetFolderNode = wsContext.getAisManager().getNode(_newTargetFolderNodePath);
            if(null == newTargetFolderNode) {
               if(_newTargetFolderNodePath.endsWith("/")) {
                   WSAisUtils.createPath(wsContext, oldTargetFolderNode, _newTargetFolderNodePath);
               } else {
                   WSAisUtils.createPath(wsContext, oldTargetFolderNode, _newTargetFolderNodePath + "/");
               }
               newTargetFolderNode = wsContext.getAisManager().getNode(_newTargetFolderNodePath);

               if(null == newTargetFolderNode) {
                    _message = "Failed to create folder node " + _newTargetFolderNodePath;
                    return true;
               }
               WSNode localeNode = wsContext.getAisManager().getNode(_newTargetLocaleFolderNodePath);
               if(null == localeNode) {
                   _message = "Failed to locate locale folder node " + _newTargetLocaleFolderNodePath;
                   return true;
               }
               localeNode.setProperty(WSSystemPropertyKey.LOCALE, newTargetLocale);
               newTargetFolderNode.setProperty(WSSystemPropertyKey.FILTER_GROUP, fg);
               ProjectCreationUtilities.copyAISProperties(oldTargetFolderNode,newTargetFolderNode);
               if(null == wsContext.getLinkManager().getLink(oldTargetFolderNode.getPath(), newTargetFolderNode.getPath())) {
                 wsContext.getLinkManager().createLink(oldTargetFolderNode.getPath(),
                                                     newTargetFolderNode.getPath(),
                                                     true);
               }
            }

        } catch (WSAisException e) {
            _message = e.getLocalizedMessage();
        }
        catch (NullPointerException e){
            _message = e.getLocalizedMessage();

        } catch (Exception e) {
            _message = e.getLocalizedMessage();
        }
        return true;
    }

    /**
     * Runs translation in a new context
     * @param wsContext - WS context
     * @return true
     */
    public boolean run(WSContext wsContext) {
        return createNode(wsContext);
    }

}