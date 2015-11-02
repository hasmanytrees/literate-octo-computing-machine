package com.idiominc.ws.integration.compassion.utilities.twostepproject;

import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.apache.xml.serialize.OutputFormat;

import java.io.Writer;
import java.io.IOException;

/**
 * Helper class to serailize xml file
 */
public class XMLSerializer {

    public static void removeChildren(Node node) {
      while (node.hasChildNodes()) node.removeChild(node.getFirstChild());
    }

    public static void convertXMLToWriter(Element assetXML, Writer writer) throws IOException {
        if(null == assetXML || null == assetXML.getOwnerDocument()) {
            throw new IOException("Asset is null or owner document is null!");
        }
        serializeXmlToStream(writer, assetXML.getOwnerDocument());
    }

    public static boolean hasCDATANode(Element source) {
       if(!source.hasChildNodes()) return false;
       NodeList children = source.getChildNodes();
       for(int j = 0; null != children && j < children.getLength(); j++) {
          if(children.item(j).getNodeType() == Node.CDATA_SECTION_NODE) {
              return true;
          }
       }
       return false;
    }

    private static void serializeXmlToStream(Writer writer, Document doc) throws IOException {
        OutputFormat format = new OutputFormat();
        format.setIndenting(true);
        format.setLineWidth(0);
        format.setPreserveSpace(true);
        format.setStandalone(false);
        org.apache.xml.serialize.XMLSerializer serializer = new org.apache.xml.serialize.XMLSerializer(writer, format);
        serializer.asDOMSerializer();
        serializer.serialize(doc.getDocumentElement());
        writer.close();
    }

}
