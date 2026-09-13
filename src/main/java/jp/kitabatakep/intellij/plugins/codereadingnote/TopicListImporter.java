package jp.kitabatakep.intellij.plugins.codereadingnote;

import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.NoteProjectContext;
import org.jdom.Element;

import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class TopicListImporter {
    /** Legacy callers (including GitHub sync) continue to import into the root project. */
    public static ArrayList<Topic> importElement(Project project, Element element) throws FormatException {
        return importElement(project, new NoteProjectContext(Path.of(project.getBasePath())), element);
    }

    public static ArrayList<Topic> importElement(Project project, NoteProjectContext context, Element element) throws FormatException {
        if (element == null || !"topics".equals(element.getName())) throw new FormatException("Missing topics");
        ArrayList<Topic> topics = new ArrayList<>();
        try {
            context.loading(() -> {
                for (Element node : element.getChildren("topic")) {
                    Date date = date(required(node, "updatedAt"));
                    Topic topic = new Topic(project, context, required(node, "name"), date, topics.size());
                    topic.xmlTemplate.set(node);
                    if (Boolean.parseBoolean(node.getChildText("hasGroups"))) {
                        ArrayList<TopicGroup> groups = new ArrayList<>();
                        Element groupNodes = node.getChild("groups");
                        if (groupNodes != null) for (Element groupNode : groupNodes.getChildren("group")) {
                            TopicGroup group = new TopicGroup(project, topic, required(groupNode, "name"), date(required(groupNode, "createdAt")));
                            group.xmlTemplate.set(groupNode);
                            group.setLines(lines(project, topic, groupNode.getChild("topicLines")));
                            group.setExpanded(Boolean.parseBoolean(groupNode.getChildText("expanded")));
                            group.restoreMetadata(text(groupNode, "note"), date(required(groupNode, "updatedAt")));
                            groups.add(group);
                        }
                        topic.setGroups(groups);
                        topic.setUngroupedLines(lines(project, topic, node.getChild("ungroupedLines")));
                    } else topic.setLines(lines(project, topic, node.getChild("topicLines")));
                    topic.restoreMetadata(text(node, "note"), date);
                    topics.add(topic);
                }
            });
        } catch (IllegalArgumentException error) { throw new FormatException(error.getMessage()); }
        return topics;
    }

    public static ArrayList<TrashedLine> importTrashedLines(Project project, Element element) {
        return importTrashedLines(project, new NoteProjectContext(Path.of(project.getBasePath())), element);
    }
    public static ArrayList<TrashedLine> importTrashedLines(Project project, NoteProjectContext context, Element element) {
        ArrayList<TrashedLine> result = new ArrayList<>();
        if (element == null || element.getChild("trash") == null) return result;
        Topic dummy = new Topic(project, context, "_trash_", new Date(), 0);
        for (Element entry : element.getChild("trash").getChildren("trashedLine")) {
            TrashedLine trash = new TrashedLine(line(project, dummy, entry.getChild("topicLine")),
                    required(entry, "originalTopic"), date(required(entry, "trashedAt")));
            trash.xmlTemplate.set(entry);
            result.add(trash);
        }
        return result;
    }
    private static ArrayList<TopicLine> lines(Project project, Topic topic, Element container) {
        ArrayList<TopicLine> result = new ArrayList<>();
        if (container != null) for (Element node : container.getChildren("topicLine")) result.add(line(project, topic, node));
        return result;
    }
    private static TopicLine line(Project project, Topic topic, Element node) {
        String inProject = required(node, "inProject");
        if (!inProject.equals("true") && !inProject.equals("false")) throw new IllegalArgumentException("Invalid inProject");
        TopicLine line = TopicLine.createByImport(project, topic, required(node, "url"),
                Integer.parseInt(required(node, "line")), text(node, "note"), Boolean.parseBoolean(inProject),
                required(node, "relativePath").replace('\\', '/'), text(node, "bookmarkUid"));
        line.xmlTemplate.set(node);
        return line;
    }
    private static String required(Element element, String name) {
        if (element == null || element.getChild(name) == null) throw new IllegalArgumentException("Missing " + name);
        return element.getChildText(name);
    }
    private static String text(Element element, String name) {
        String value = element.getChildText(name);
        return value == null ? "" : value;
    }
    private static Date date(String text) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            format.setLenient(false);
            return format.parse(text);
        } catch (ParseException error) { throw new IllegalArgumentException("Invalid date", error); }
    }
    public static class FormatException extends Exception {
        public FormatException(String message) { super(message); }
    }
}
