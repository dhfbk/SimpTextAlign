package Preprocessing;

import eu.fbk.utils.core.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class ConvertSimpitiki {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertSimpitiki.class);

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./convert-simpitiki")
                    .withHeader(
                            "Convert Simpitiki corpus to TSV format")
                    .withOption("i", "input", "Input file (Simpitiki XML)", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFile = cmd.getOptionValue("input", File.class);
            File outputFile = cmd.getOptionValue("output", File.class);

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();

            XPathExpression expr;
            NodeList nl;

            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            expr = xpath.compile("/resource/simplifications/simplification");
            nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i++) {
                Node item = nl.item(i);
//                Element element = (Element) item;
                NodeList children = item.getChildNodes();

                String before = null;
                String after = null;

                for (int j = 0; j < children.getLength(); j++) {
                    Node child = children.item(j);
                    if (child.getNodeName().equals("before")) {
                        before = child.getTextContent().replaceAll("</?del>", "");
                    }
                    if (child.getNodeName().equals("after")) {
                        after = child.getTextContent().replaceAll("</?ins>", "");
                    }
                }

                if (before == null || after == null) {
                    LOGGER.warn("Something is null");
                    continue;
                }

                before = before.replace('\t', ' ').trim();
                after = after.replace('\t', ' ').trim();
                writer.append(before).append("\t").append(after).append("\n");
            }

            writer.close();

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
