package jp.kitabatakep.intellij.plugins.codereadingnote;

import org.jdom.Element;

import java.text.SimpleDateFormat;
import java.util.Iterator;

import java.util.ArrayList;

public class TopicListExporter
{
    public static Element export(Iterator<Topic> iterator) {
        return export(iterator, new ArrayList<>());
    }

    public static Element export(Iterator<Topic> iterator, ArrayList<TrashedLine> trashedLines)
    {
        Element topicsElement = new Element("topics");
        while (iterator.hasNext()) {
            Topic topic = iterator.next();
            Element topicElement = new Element("topic");
            topicElement.addContent(new Element("name").addContent(topic.name()));
            topicElement.addContent(new Element("note").addContent(topic.note()));
            topicElement.addContent(
                new Element("updatedAt").
                    addContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(topic.updatedAt()))
            );
            
            // Add group support flag
            topicElement.addContent(new Element("hasGroups").addContent(String.valueOf(!topic.getGroups().isEmpty())));

            topicsElement.addContent(topicElement);

            if (!topic.getGroups().isEmpty()) {
                // Export groups
                Element groupsElement = new Element("groups");
                for (TopicGroup group : topic.getGroups()) {
                    Element groupElement = new Element("group");
                    groupElement.addContent(new Element("name").addContent(group.name()));
                    groupElement.addContent(new Element("note").addContent(group.note()));
                    groupElement.addContent(new Element("expanded").addContent(String.valueOf(group.isExpanded())));
                    groupElement.addContent(
                        new Element("createdAt").
                            addContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(group.createdAt()))
                    );
                    groupElement.addContent(
                        new Element("updatedAt").
                            addContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(group.updatedAt()))
                    );
                    
                    // Export lines in group
                    Element groupLinesElement = new Element("topicLines");
                    Iterator<TopicLine> groupLinesIterator = group.linesIterator();
                    while (groupLinesIterator.hasNext()) {
                        TopicLine topicLine = groupLinesIterator.next();
                        Element topicLineElement = createTopicLineElement(topicLine);
                        groupLinesElement.addContent(topicLineElement);
                    }
                    groupElement.addContent(groupLinesElement);
                    groupsElement.addContent(groupElement);
                }
                topicElement.addContent(groupsElement);
                
                // Export ungrouped lines
                if (!topic.getUngroupedLines().isEmpty()) {
                    Element ungroupedLinesElement = new Element("ungroupedLines");
                    for (TopicLine topicLine : topic.getUngroupedLines()) {
                        Element topicLineElement = createTopicLineElement(topicLine);
                        ungroupedLinesElement.addContent(topicLineElement);
                    }
                    topicElement.addContent(ungroupedLinesElement);
                }
            } else {
                // Legacy mode - export lines directly
                Element topicLinesElement = new Element("topicLines");
                Iterator<TopicLine> linesIterator = topic.linesIterator();
                while (linesIterator.hasNext()) {
                    TopicLine topicLine = linesIterator.next();
                    Element topicLineElement = createTopicLineElement(topicLine);
                    topicLinesElement.addContent(topicLineElement);
                }
                topicElement.addContent(topicLinesElement);
            }
        }

        if (trashedLines != null && !trashedLines.isEmpty()) {
            Element trashElement = new Element("trash");
            for (TrashedLine tl : trashedLines) {
                Element entry = new Element("trashedLine");
                entry.addContent(new Element("originalTopic").addContent(tl.getOriginalTopicName()));
                entry.addContent(new Element("trashedAt").addContent(
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(tl.getTrashedAt())));
                entry.addContent(createTopicLineElement(tl.getLine()));
                trashElement.addContent(entry);
            }
            topicsElement.addContent(trashElement);
        }

        return topicsElement;
    }
    
    private static Element createTopicLineElement(TopicLine topicLine) {
        Element topicLineElement = new Element("topicLine");
        topicLineElement.addContent(new Element("line").addContent(String.valueOf(topicLine.line())));
        topicLineElement.addContent(new Element("inProject").addContent(String.valueOf(topicLine.inProject())));
        topicLineElement.addContent(new Element("url").addContent(topicLine.url()));
        topicLineElement.addContent(new Element("note").addContent(topicLine.note()));
        topicLineElement.addContent(new Element("bookmarkUid").addContent(topicLine.getBookmarkUid()));
        topicLineElement.addContent(
            new Element("relativePath").addContent(topicLine.inProject() ? topicLine.relativePath() : "")
        );
        return topicLineElement;
    }
}
