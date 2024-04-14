package jp.kitabatakep.intellij.plugins.codereadingnote;

import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.w3c.dom.NodeList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;

public class TopicListImporter
{
    public static ArrayList<Topic> importElement(Project project, Element topicsElement) throws FormatException
    {
        ArrayList<Topic> topics = new ArrayList<>();
        try {
            for (Element topicElement : topicsElement.getChildren("topic")) {
                String name = topicElement.getChild("name").getText();
                String updatedAtString = topicElement.getChild("updatedAt").getText();

                Topic topic;
                try {
                    topic = new Topic(project, name, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(updatedAtString));
                } catch (ParseException e) {
                    throw new FormatException(e.getMessage());
                }
                topic.setNote(topicElement.getChild("note").getText());

                Element topicLinesElement = topicElement.getChild("topicLines");
                ArrayList<TopicLine> topicLines = new ArrayList<>();
                for (Element topicLineElement : topicLinesElement.getChildren("topicLine")) {
                    String lineString = topicLineElement.getChild("line").getText();
                    String inProject = topicLineElement.getChild("inProject").getText();
                    TopicLine topicLine = TopicLine.createByImport(
                        project,
                        topic,
                        topicLineElement.getChild("url").getText(),
                        Integer.parseInt(lineString),
                        topicLineElement.getChild("note").getText(),
                        inProject.equals("true"),
                        topicLineElement.getChild("relativePath").getText()
                    );
                    topicLines.add(topicLine);
                }
                topic.setLines(topicLines);
                topics.add(topic);
            }
        } catch (NullPointerException e) {
            throw new FormatException(e.getMessage());
        }

        Collections.sort(topics);
        return topics;
    }

    public static ArrayList<Topic> importElementNew(Project project, org.w3c.dom.Element topicsElement) throws FormatException
    {
        ArrayList<Topic> topics = new ArrayList<>();
        try {
            NodeList topicNodeList = topicsElement.getElementsByTagName("topic");
            for (int i = 0; i < topicNodeList.getLength(); i++) {
                org.w3c.dom.Element topicElement = (org.w3c.dom.Element) topicNodeList.item(i);
                String name = topicElement.getElementsByTagName("name").item(0).getTextContent();
                String updatedAtString = topicElement.getElementsByTagName("updatedAt").item(0).getTextContent();

                Topic topic;
                try {
                    topic = new Topic(project, name, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(updatedAtString));
                } catch (ParseException e) {
                    throw new FormatException(e.getMessage());
                }
                topic.setNote(topicElement.getElementsByTagName("note").item(0).getTextContent());

                org.w3c.dom.Element topicLinesElement = (org.w3c.dom.Element) topicElement.getElementsByTagName("topicLines").item(0);
                ArrayList<TopicLine> topicLines = new ArrayList<>();
                NodeList topicLineNodeList = topicLinesElement.getElementsByTagName("topicLine");
                for (int j = 0; j < topicLineNodeList.getLength(); j++) {
                    org.w3c.dom.Element topicLineElement = (org.w3c.dom.Element) topicLineNodeList.item(j);
                    String lineString = topicLineElement.getElementsByTagName("line").item(0).getTextContent();
                    String inProject = topicLineElement.getElementsByTagName("inProject").item(0).getTextContent();
                    TopicLine topicLine = TopicLine.createByImport(
                            project,
                            topic,
                            topicLineElement.getElementsByTagName("url").item(0).getTextContent(),
                            Integer.parseInt(lineString),
                            topicLineElement.getElementsByTagName("note").item(0).getTextContent(),
                            inProject.equals("true"),
                            topicLineElement.getElementsByTagName("relativePath").item(0).getTextContent()
                    );
                    topicLines.add(topicLine);
                }
                topic.setLines(topicLines);
                topics.add(topic);
            }
        } catch (NullPointerException e) {
            throw new FormatException(e.getMessage());
        }

        Collections.sort(topics);
        return topics;
    }


    public static class FormatException extends Exception {
        public FormatException(String errorMessage) {
            super(errorMessage);
        }
    }
}
