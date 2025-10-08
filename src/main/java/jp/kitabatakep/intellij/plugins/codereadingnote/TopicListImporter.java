package jp.kitabatakep.intellij.plugins.codereadingnote;

import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.w3c.dom.NodeList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

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
                
                // Check if topic has groups
                Element hasGroupsElement = topicElement.getChild("hasGroups");
                boolean hasGroups = hasGroupsElement != null && Boolean.parseBoolean(hasGroupsElement.getText());

                if (hasGroups) {
                    // Import groups
                    Element groupsElement = topicElement.getChild("groups");
                    if (groupsElement != null) {
                        ArrayList<TopicGroup> groups = new ArrayList<>();
                        for (Element groupElement : groupsElement.getChildren("group")) {
                            String groupName = groupElement.getChild("name").getText();
                            String createdAtString = groupElement.getChild("createdAt").getText();
                            String groupUpdatedAtString = groupElement.getChild("updatedAt").getText();
                            boolean expanded = Boolean.parseBoolean(groupElement.getChild("expanded").getText());
                            
                            Date createdAt;
                            try {
                                createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(createdAtString);
                            } catch (ParseException e) {
                                createdAt = new Date(); // fallback to current date
                            }
                            
                            TopicGroup group = new TopicGroup(project, topic, groupName, createdAt);
                            group.setNote(groupElement.getChild("note").getText());
                            group.setExpanded(expanded);
                            
                            // Import lines in group
                            Element groupLinesElement = groupElement.getChild("topicLines");
                            if (groupLinesElement != null) {
                                ArrayList<TopicLine> groupLines = importTopicLines(project, topic, groupLinesElement);
                                group.setLines(groupLines);
                            }
                            
                            groups.add(group);
                        }
                        topic.setGroups(groups);
                    }
                    
                    // Import ungrouped lines
                    Element ungroupedLinesElement = topicElement.getChild("ungroupedLines");
                    if (ungroupedLinesElement != null) {
                        ArrayList<TopicLine> ungroupedLines = importTopicLines(project, topic, ungroupedLinesElement);
                        topic.setUngroupedLines(ungroupedLines);
                    }
                } else {
                    // Legacy mode - import lines directly
                    Element topicLinesElement = topicElement.getChild("topicLines");
                    if (topicLinesElement != null) {
                        ArrayList<TopicLine> topicLines = importTopicLines(project, topic, topicLinesElement);
                        topic.setLines(topicLines);
                    }
                }
                
                topics.add(topic);
            }
        } catch (NullPointerException e) {
            throw new FormatException(e.getMessage());
        }

        Collections.sort(topics);
        return topics;
    }
    
    private static ArrayList<TopicLine> importTopicLines(Project project, Topic topic, Element topicLinesElement) throws FormatException {
        ArrayList<TopicLine> topicLines = new ArrayList<>();
        for (Element topicLineElement : topicLinesElement.getChildren("topicLine")) {
            String lineString = topicLineElement.getChild("line").getText();
            String inProject = topicLineElement.getChild("inProject").getText();
            
            // Handle optional bookmarkUid for backward compatibility
            String bookmarkUid = "";
            Element bookmarkUidElement = topicLineElement.getChild("bookmarkUid");
            if (bookmarkUidElement != null) {
                bookmarkUid = bookmarkUidElement.getText();
            }
            
            TopicLine topicLine = TopicLine.createByImport(
                project,
                topic,
                topicLineElement.getChild("url").getText(),
                Integer.parseInt(lineString),
                topicLineElement.getChild("note").getText(),
                inProject.equals("true"),
                topicLineElement.getChild("relativePath").getText(),
                bookmarkUid
            );
            topicLines.add(topicLine);
        }
        return topicLines;
    }

    public static class FormatException extends Exception {
        public FormatException(String errorMessage) {
            super(errorMessage);
        }
    }
}
