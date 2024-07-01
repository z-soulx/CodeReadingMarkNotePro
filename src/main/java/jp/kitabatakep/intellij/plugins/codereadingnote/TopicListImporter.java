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
                    Element bookmarkUid = topicLineElement.getChild("bookmarkUid");
                    TopicLine topicLine = TopicLine.createByImport(
                        project,
                        topic,
                        topicLineElement.getChild("url").getText(),
                        Integer.parseInt(lineString),
                        topicLineElement.getChild("note").getText(),
                        inProject.equals("true"),
                        topicLineElement.getChild("relativePath").getText(),
                        bookmarkUid != null ? bookmarkUid.getText() : null
                    );
                    topicLines.add(topicLine);
                }
                topic.setLines(topicLines);
                topics.add(topic);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
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
