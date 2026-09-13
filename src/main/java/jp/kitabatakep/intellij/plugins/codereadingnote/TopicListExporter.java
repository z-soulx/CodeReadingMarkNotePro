package jp.kitabatakep.intellij.plugins.codereadingnote;

import org.jdom.Element;

import java.text.SimpleDateFormat;
import java.util.Iterator;

import java.util.ArrayList;

public class TopicListExporter
{
    /** Complete sync/local payload, including explicit empty trash and unknown list extensions. */
    public static Element export(TopicList list) {
        Element result = list.xmlTemplate();
        result.removeChildren("topic"); result.removeChildren("trash");
        Element known = export(list.iterator(), list.getTrashedLines());
        for (Element child : known.getChildren()) result.addContent(child.clone());
        if (result.getChild("trash") == null) result.addContent(new Element("trash"));
        return result;
    }
    public static Element export(Iterator<Topic> iterator) {
        return export(iterator, new ArrayList<>());
    }

    public static Element export(Iterator<Topic> iterator, ArrayList<TrashedLine> trashedLines)
    {
        Element topicsElement = new Element("topics");
        while (iterator.hasNext()) {
            Topic topic = iterator.next();
            Element topicElement = topic.xmlTemplate.create("topic", "name", "note", "updatedAt", "hasGroups", "groups", "ungroupedLines", "topicLines");
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
                Element groupsElement = topic.xmlTemplate.child("groups", "group");
                for (TopicGroup group : topic.getGroups()) {
                    Element groupElement = group.xmlTemplate.create("group", "name", "note", "expanded", "createdAt", "updatedAt", "topicLines");
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
                    Element groupLinesElement = group.xmlTemplate.child("topicLines", "topicLine");
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
                    Element ungroupedLinesElement = topic.xmlTemplate.child("ungroupedLines", "topicLine");
                    for (TopicLine topicLine : topic.getUngroupedLines()) {
                        Element topicLineElement = createTopicLineElement(topicLine);
                        ungroupedLinesElement.addContent(topicLineElement);
                    }
                    topicElement.addContent(ungroupedLinesElement);
                }
            } else {
                // Legacy mode - export lines directly
                Element topicLinesElement = topic.xmlTemplate.child("topicLines", "topicLine");
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
                Element entry = tl.xmlTemplate.create("trashedLine", "originalTopic", "trashedAt", "topicLine");
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
        Element topicLineElement = topicLine.xmlTemplate.create("topicLine", "line", "inProject", "url", "note", "bookmarkUid", "relativePath");
        topicLineElement.addContent(new Element("line").addContent(String.valueOf(topicLine.line())));
        topicLineElement.addContent(new Element("inProject").addContent(String.valueOf(topicLine.inProject())));
        topicLineElement.addContent(new Element("url").addContent(topicLine.url()));
        topicLineElement.addContent(new Element("note").addContent(topicLine.note()));
        topicLineElement.addContent(new Element("bookmarkUid").addContent(topicLine.getBookmarkUid() == null ? "" : topicLine.getBookmarkUid()));
        topicLineElement.addContent(
            new Element("relativePath").addContent(topicLine.inProject() ? topicLine.relativePath() : "")
        );
        return topicLineElement;
    }
}
