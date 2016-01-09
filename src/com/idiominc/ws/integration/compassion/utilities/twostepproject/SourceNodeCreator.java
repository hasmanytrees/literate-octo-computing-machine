package com.idiominc.ws.integration.compassion.utilities.twostepproject;

//sdk
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSRunnable;
import com.idiominc.wssdk.ais.WSAisException;
import com.idiominc.wssdk.ais.WSNode;

//profserv
import com.idiominc.ws.integration.profserv.commons.wssdk.WSAisUtils;
import com.idiominc.ws.integration.profserv.commons.FileUtils;

//apache
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

//java
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Used by the projeoct creation classes to help create source files in WorldServer
 *
 * @author SDL Professional Services
 */
public class SourceNodeCreator implements WSRunnable {

    //log
    private static Logger log = Logger.getLogger(SourceNodeCreator.class);
    static {
        log.setLevel(Level.INFO);
    }


    private String            _frmFolderNodePath;
    private String            _toFolderNodePath;
    private String            _frmFullPath;
    private String            _newPath;
    private String            _message;

    /**
     * Get state of the object
     * @return Object's state
     */
    public String toString() {
      return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    /**
     * Check status of the node creation
     * @throws TwoStepProjectException - the new node was not created
     */
    public void checkStatus() throws TwoStepProjectException {
        if(_message != null) {
            throw new TwoStepProjectException(_message + " " + this.toString());
        }
        if(_newPath == null) {
            throw new TwoStepProjectException("Could not create new node " + _newPath);
        }
    }

    /**
     * Get path to the new source node
     * @return path to the new source node
     */
    public String getPath() {
        return _newPath;
    }

    /**
     *  Conversion constructor
     * @param frmFolderNodePath - full path to folder that has the translated document (stage 1)
     * @param frmBaseLocaleNodePath  - full path to locale folder for transled document (stage 1)
     * @param toBaseLocaleNodePath  -  full path to locale folder for new source
     * @param frmFullPath - full path to the original translated document
     */
    public SourceNodeCreator(String frmFolderNodePath,
                             String frmBaseLocaleNodePath,
                             String toBaseLocaleNodePath,
                             String frmFullPath
                             ) {
        _frmFullPath = frmFullPath;
        _frmFolderNodePath = frmFolderNodePath;
        String tail = "";
        if (frmFolderNodePath.length() > frmBaseLocaleNodePath.length()) {
          tail = frmFolderNodePath.substring(frmBaseLocaleNodePath.length());
        }
        if(tail.length() > 0 && !tail.startsWith("/") && !(toBaseLocaleNodePath.endsWith("/"))) {
            tail = "/" + tail;
        }
        _toFolderNodePath = toBaseLocaleNodePath + tail;
        log.info("_toFolderNodePath ==> " + _toFolderNodePath);
        _message = null;
        _newPath = null;
    }


    /**
     *  Creates new source node
     * @param wsContext -WS Context
     * @return true
     */
    private boolean createNode(WSContext wsContext) {

        try{

            WSNode frmFolderNode   = wsContext.getAisManager().getNode(_frmFolderNodePath);

            if(null == frmFolderNode) {
                _message = "Can't access " + _frmFolderNodePath;
                return true;
            }

            WSNode toFolderNode = wsContext.getAisManager().getNode(_toFolderNodePath);

            if(null == toFolderNode) {
               if(_toFolderNodePath.endsWith("/")) {
                   WSAisUtils.createPath(wsContext, frmFolderNode, _toFolderNodePath);
               } else {
                   _toFolderNodePath += "/";
                   WSAisUtils.createPath(wsContext, frmFolderNode, _toFolderNodePath);
               }
               toFolderNode = wsContext.getAisManager().getNode(_toFolderNodePath);
               if(null == toFolderNode) {
                    _message = "Failed to create or access folder node " + _toFolderNodePath;
                    return true;
               }
            }
            WSNode origSourceNode = wsContext.getAisManager().getNode(_frmFullPath);
            if(null == origSourceNode) {
                _message = "Can't access " + _frmFullPath;
                return true;
            }
            _newPath = _toFolderNodePath +
                       ((_toFolderNodePath.endsWith("/"))? "": "/") +
                       FileUtils.getFileName(_frmFullPath);
            log.info("_newPath ==> " + _newPath);
            WSNode newNode = wsContext.getAisManager().create(_newPath, origSourceNode);
            if(newNode == null) {
              _message = "Failed to create node under the path " + _newPath;
              return true;
            }
            InputStream in = origSourceNode.getInputStream();
            OutputStream out = newNode.getOutputStream();
            FileUtils.copyStream(in, out);
            FileUtils.close(in);
            FileUtils.close(out);
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
     * Runs transaction in a new context
     * @param wsContext - WS context
     * @return true
     */
    public boolean run(WSContext wsContext) {
        return createNode(wsContext);
    }

}
