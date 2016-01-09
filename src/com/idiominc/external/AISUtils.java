package com.idiominc.external;

import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSContextManager;
import com.idiominc.wssdk.WSRunnable;
import com.idiominc.wssdk.WSRuntimeException;
import com.idiominc.wssdk.ais.WSAisException;
import com.idiominc.wssdk.ais.WSAisManager;
import com.idiominc.wssdk.ais.WSNode;
import com.idiominc.wssdk.ais.WSNodeType;
import com.idiominc.wssdk.ais.WSSystemPropertyKey;
import com.idiominc.wssdk.linkage.WSLink;
import com.idiominc.wssdk.linkage.WSLinkManager;
import com.idiominc.wssdk.user.WSLocale;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Generic shared Utility class for WorldServer AIS framework
 *
 * @author SDL Professional Services
 */
public class AISUtils {

    private static final String _AIS_ROOT = "/Remote Content/";

    private static final Map<String, WSSystemPropertyKey> systemPropertyKeys = new HashMap<String, WSSystemPropertyKey>();


    static {
        systemPropertyKeys.put(WSSystemPropertyKey.CONTEXT_URL.toString(), WSSystemPropertyKey.CONTEXT_URL);
        systemPropertyKeys.put(WSSystemPropertyKey.ENCODING.toString(), WSSystemPropertyKey.ENCODING);
        systemPropertyKeys.put(WSSystemPropertyKey.FILTER_CONFIGURATION.toString(), WSSystemPropertyKey.FILTER_CONFIGURATION);
        systemPropertyKeys.put(WSSystemPropertyKey.FILTER_GROUP.toString(), WSSystemPropertyKey.FILTER_GROUP);
        systemPropertyKeys.put(WSSystemPropertyKey.LOCALE.toString(), WSSystemPropertyKey.LOCALE);
        systemPropertyKeys.put(WSSystemPropertyKey.MT_ADAPTER.toString(), WSSystemPropertyKey.MT_ADAPTER);
        systemPropertyKeys.put(WSSystemPropertyKey.TERMINOLOGY_DATABASE.toString(), WSSystemPropertyKey.TERMINOLOGY_DATABASE);
        systemPropertyKeys.put(WSSystemPropertyKey.TRANSLATION_MEMORY.toString(), WSSystemPropertyKey.TRANSLATION_MEMORY);
    }


    public static final char[] illegalChars = "?<>:*|\"".toCharArray();
    private static final char[] illegalNonContentChars = "\\/".toCharArray();


    private static Logger log = Logger.getLogger(AISUtils.class);

    /**
     * Try to guess content node type from names
     *
     * @param aisManager WSAisManager instance
     * @param aisPath    AIS path to get content
     * @return WSNodeType which name looks like a content, otherwise <code>null</code>
     * @throws com.idiominc.wssdk.ais.WSAisException on AIS operations failure
     */
    public static WSNodeType getContentType(WSAisManager aisManager, String aisPath)
            throws WSAisException {
        WSNodeType[] possibleTypes = aisManager.getPossibleChildTypes(aisPath);
        for (int n = 0; n < possibleTypes.length; ++n) {
            String tName = possibleTypes[n].getName(Locale.getDefault());
            if (tName.indexOf("directory") >= 0)
                continue;

            return possibleTypes[n];
        }

        return null;
    }


    /**
     * Create AIS content node, Node type is obtained with
     * {@link #getContentType(com.idiominc.wssdk.ais.WSAisManager, String)}
     *
     * @param aisManager WSAisManager instance
     * @param base       Dir to get {@link WSNodeType} from
     * @param aisPath    AIS path to node to create
     * @return Created node, or <code>null</code> if Content NodeType cannot be found or node
     * cannot be created
     * @throws WSAisException on AIS operations failure
     */
    public static WSNode createContentNode(WSAisManager aisManager, String base, String aisPath)
            throws WSAisException {

        for (char c : illegalChars) {
            aisPath = aisPath.replace(c + "", "");
        }
        WSNodeType type = getContentType(aisManager, base);
        if (type == null) {
            log.error("Content node type not found for base: " + base);
            return null;
        }

        int slashPos = aisPath.lastIndexOf('/');
        if (slashPos > 0) {
            WSNode dir = createDirectoryRecurse(aisManager, aisPath.substring(0, slashPos));
            if (dir == null) {
                log.error("Parent directory creation failed: " + aisPath);
                return null;
            }
        }

        return aisManager.create(aisPath, type);
    }

    /**
     * Create AIS directory node, all needed nodes are created as well
     *
     * @param aisManager WSAisManager instance
     * @param aisPath    AIS directory path
     * @return WSNode created, or <code>null</code> if not existing base mount specified
     * @throws WSAisException on AIS operations failure
     */
    public static WSNode createDirectoryRecurse(WSAisManager aisManager, String aisPath)
            throws WSAisException {

        for (char c : illegalChars) {
            aisPath = aisPath.replace(c + "", "");
        }
        // return WSNode if already exist
        WSNode nodeAttemp = aisManager.getNode(aisPath);
        if (nodeAttemp != null)
            return nodeAttemp;

        String[] pathSplitted = aisPath.split("/");
        StringBuffer buf = new StringBuffer();
        WSNode parent = null;
        for (int i = aisPath.startsWith("/") ? 1 : 0; i < pathSplitted.length; ++i) {
            // try to obtain next child node
            String path = buf.append("/").append(pathSplitted[i]).toString();

            if (log.isDebugEnabled())
                log.debug("DirCreate: " + path);

            WSNode dirNode = aisManager.getNode(path);
            if (dirNode == null) {
                // not found, create it
                if (parent == null)
                    return null;
                dirNode = aisManager.create(path, parent);
            }
            parent = dirNode;
        }

        return parent;
    }


    public static WSNode createContentInAIS(WSContext externalContext, final WSLocale extneralSrcLocale, final WSLocale[] tgtLocales, final String id, final File xmlPayload) {

        final Object[] retRef = new Object[1];

        WSContextManager.run(externalContext, new WSRunnable() {

            public boolean run(WSContext context) {

                try {

                    WSLocale srcLocale = context.getUserManager().getLocale(extneralSrcLocale.getId());
                    //String rootPath = getAisRoot(srcLocale, context.getUser().getName());

                    String name = xmlPayload.getName();
                    for (char c : illegalChars) {
                        name = name.replace(c + "", "");
                    }
                    name = name.replace("\\", "/");
                    WSNode node = getNode(context, id, srcLocale, name);
                    if (node == null) {
                        String msg = "Node obtaining failed: " + name;
                        log.error(msg);
                        throw new WSAisException(msg);
                    }
                    copyContents(node, xmlPayload);

                    retRef[0] = node;

                    return true;
                } catch (Exception e) {
                    throw new WSRuntimeException(e);
                }
            }
        });

        return (WSNode) retRef[0];

    }


    private static void copyContents(WSNode node, File data) throws Exception {
        node.copyFrom(data);
    }


    private static WSNode getNode(WSContext context, String id, WSLocale locale, String path) throws WSAisException {
        WSAisManager aisManager = context.getAisManager();

        String dirPath = getAisRoot(locale, id);
        String targetPath = getAisPath(locale, path, id);
        WSNode node = aisManager.getNode(targetPath);
        if (node != null)
            return node;

        WSNodeType[] types = aisManager.getPossibleChildTypes(dirPath);
        WSNodeType type = getContentNodeType(types);
        if (type == null)
            throw new WSAisException("Cannot identify content node type for AIS path " + dirPath);

        if (log.isDebugEnabled())
            log.debug("Target path: " + targetPath);

        return AISUtils.createContentNode(aisManager, dirPath, targetPath);
    }


    private static WSNodeType getContentNodeType(WSNodeType[] types) {
        for (int n = 0; n < types.length; ++n) {
            WSNodeType t = types[n];
            String typeName = t.getName(Locale.getDefault());
            typeName = typeName.toLowerCase();
            if (typeName.indexOf("directory") == -1 ||
                    typeName.indexOf("container") == -1) {
                return t;
            }
        }

        return null;
    }

    public static WSNode getBaseLocaleNode(WSContext context, WSNode contentNode, WSLocale locale) throws WSAisException {

        WSNode parentNode = contentNode.getParent();
        WSLocale parentLocale = getLocaleValue(context, parentNode);

        if (!locale.equals(parentLocale) && contentNode.isContainer()) {
            return contentNode;
        } else {
            return getBaseLocaleNode(context, parentNode, locale);
        }
    }

    public static WSNode getBaseLocaleNode(WSContext context, WSNode contentNode) throws WSAisException {

        WSNode parentNode = contentNode.getParent();
        WSLocale parentLocale = getLocaleValue(context, parentNode);

        // PZA: 2/12/2014: will it fix the issue? if (parentLocale != null && contentNode.isContainer()) {
        if (parentLocale != null && parentNode.isContainer()) {
            log.error("returning value");
            return parentNode;
        } else {
            return getBaseLocaleNode(context, parentNode);
        }
    }

    public static WSLocale getLocaleValue(WSContext context, WSNode node) throws WSAisException {

        if (node == null)
            return null;

        WSLocale locale = null;

        WSLocale propertyValue = (WSLocale) node.getProperty(WSSystemPropertyKey.LOCALE);
        //log.error("DEBUG: "+propertyValue+"node"+node.getPath());

        locale = (WSLocale) node.getProperty(WSSystemPropertyKey.LOCALE);

        // locale = context.getUserManager().getLocale(node.getProperty((WSNode.PROPERTY_KEY_LOCALE)));

        if (locale == null) {
            try {
                locale = context.getUserManager().getLocale(Integer.parseInt(node.getProperty((WSNode.PROPERTY_KEY_LOCALE))));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return locale;
    }


    private static String getAisRoot(WSLocale locale, String name) {
        return _AIS_ROOT + getSafeLocaleName(locale) + "/" + name;
    }

    private static String getAisPath(WSLocale locale, String path, String name) {
        return getAisRoot(locale, name) + (path.startsWith("/") ? "" : "/") + path;
    }


    public static void setupTargetLocaleFolders(WSContext context, final WSLocale srcLocale,
                                                final WSLocale[] tgtLocales)
            throws WSAisException {

        createLocaleFolder(context, srcLocale);
        setContentLocale(context, srcLocale);

        for (WSLocale tgtLocale : tgtLocales) {
            createLocaleFolder(context, tgtLocale);
            setContentLocale(context, tgtLocale);
            if(srcLocale.getId() != tgtLocale.getId()) {
                setupLinkage(context, srcLocale, tgtLocale);
            }
        }

    }

    /**
     * Check if node for AIS path exists, if necessary create it and set it's locale property
     */

    private static String getSafeLocaleName(WSLocale locale) {
        String ret = locale.getName();

        //todo: replace in a single regex
        for (char c : illegalNonContentChars) {
            ret = ret.replace(c + "", "");
        }

        for (char c : illegalChars) {
            ret = ret.replace(c + "", "");
        }


        return ret;
    }

    private static void createLocaleFolder(WSContext context, final WSLocale locale)
            throws WSAisException {

        final WSAisException[] eRef = new WSAisException[1];
        final String localeName = getSafeLocaleName(locale);

        WSContextManager.run(context, new WSRunnable() {
            public boolean run(WSContext context) {
                try {
                    WSNode dir = context.getAisManager().getNode(_AIS_ROOT + localeName);

                    if (dir == null) {
                        // create node and setup it's locale
                        WSNode baseDir = context.getAisManager().getNode(_AIS_ROOT);
                        if (baseDir == null)
                            throw new WSAisException("AIS root does not exist! Please ensure the mount has been created! (" + _AIS_ROOT + ")");

                        //System.out.println("AISPATH=" + contentPath);


                        String toCreate = baseDir.getPath() + localeName;
                        for (char c : illegalChars) {
                            toCreate = toCreate.replace(c + "", "");
                        }

                        dir = context.getAisManager().create(toCreate, baseDir);

                    }
                    return true;
                } catch (WSAisException e) {
                    eRef[0] = e;
                    return false;
                }
            }
        });

        if (eRef[0] != null)
            throw eRef[0];

    }

    private static void setContentLocale(WSContext context, WSLocale locale)
            throws WSAisException {

        final WSAisException[] eRef = new WSAisException[1];
        final int localeId = locale.getId();


        WSContextManager.run(context, new WSRunnable() {
            public boolean run(WSContext context) {
                try {
                    WSLocale tgtLocale = context.getUserManager().getLocale(localeId);
                    String path = _AIS_ROOT + getSafeLocaleName(tgtLocale);
                    WSNode dir = context.getAisManager().getNode(path);
                    dir.setProperty(WSSystemPropertyKey.LOCALE, tgtLocale);
                    return true;
                } catch (WSAisException e) {
                    eRef[0] = e;
                    return false;
                }
            }
        });

        if (eRef[0] != null)
            throw eRef[0];

    }

    private static void setupLinkage(WSContext externalContext, WSLocale srcLocale, WSLocale tgtLocale) {

        final int srcLocaleId = srcLocale.getId();
        final int tgtLocaleId = tgtLocale.getId();


        WSContextManager.run(externalContext, new WSRunnable() {
            public boolean run(WSContext context) {
                WSLocale tgtLocale = context.getUserManager().getLocale(tgtLocaleId);
                WSLocale srcLocale = context.getUserManager().getLocale(srcLocaleId);


                WSLinkManager linkManager = context.getLinkManager();
                String srcPath = _AIS_ROOT + getSafeLocaleName(srcLocale);
                String tgtPath = _AIS_ROOT + getSafeLocaleName(tgtLocale);

                WSLink link = linkManager.getLink(srcPath, tgtPath);
                if (link == null)
                    linkManager.createLink(srcPath, tgtPath);
                return true;
            }
        });

    }
}

