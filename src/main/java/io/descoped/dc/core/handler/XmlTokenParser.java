package io.descoped.dc.core.handler;

import io.descoped.dc.api.handler.DocumentParserFeature;
import io.descoped.dc.api.handler.QueryException;
import io.descoped.dc.api.handler.SupportHandler;
import io.descoped.dc.api.node.XmlToken;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.function.Consumer;

@SupportHandler(forClass = XmlToken.class, selectorClass = DocumentParserFeature.class)
public class XmlTokenParser implements DocumentParserFeature {

    public XmlTokenParser() {
    }

    /**
     * @param document ObjectNode (document, element or node)
     * @return byte array
     */
    @Override
    public byte[] serialize(Object document) {
        try (StringWriter writer = new StringWriter()) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            // Disable external DTDs and stylesheets
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(new DOMSource((Node) document), new StreamResult(writer));
            return writer.toString().getBytes();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new QueryException(e);
        }
    }

    static XMLReader createSAXFactory() {
        try {
            SAXParserFactory sax = SAXParserFactory.newInstance();
            // Disable external entities
            sax.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            sax.setNamespaceAware(false);
            return sax.newSAXParser().getXMLReader();
        } catch (SAXException | ParserConfigurationException e) {
            throw new QueryException(e);
        }
    }

    static DocumentBuilder createDocumentBuilder() {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

            // Disable external entities
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            documentBuilderFactory.setExpandEntityReferences(false);

            documentBuilderFactory.setNamespaceAware(false);
            return documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new QueryException(e);
        }
    }

    /**
     * @param source xml byte array
     * @return Document
     */
    @Override
    public Object deserialize(byte[] source) {
        try {
            XMLReader reader = createSAXFactory();
            try (ByteArrayInputStream bais = new ByteArrayInputStream(source)) {
                SAXSource saxSource = new SAXSource(reader, new InputSource(bais));
                DocumentBuilder documentBuilder = createDocumentBuilder();
                Document doc = documentBuilder.parse(saxSource.getInputSource());
                doc.normalizeDocument();
                return doc;
            }

        } catch (SAXException | IOException e) {
            throw new QueryException(new String(source), e);
        }
    }

    @Override
    public void tokenDeserializer(InputStream source, Consumer<Object> entryCallback) {

    }
}
