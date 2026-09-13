package jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Arrays;
import java.util.UUID;

/** One serial writer per canonical project root, coordinated by WorkspaceNotesCoordinator. */
public final class WorkspaceXmlStore {
    private final Path path;
    private final Path recovery;
    private byte[] baseline;
    private Document document;
    private boolean loaded;

    public WorkspaceXmlStore(Path path) { this(path, path.resolveSibling("CodeReadingNote.workspace-pending.xml")); }
    public WorkspaceXmlStore(Path path, Path recovery) { this.path = path; this.recovery = recovery; }
    public Path path() { return path; }
    public Path pendingPath() { return recovery; }

    public synchronized Element load() throws IOException {
        validatePath();
        byte[] bytes = readBytes();
        Document parsed = bytes == null ? new Document(new Element("project").setAttribute("version", "4")) : parse(bytes);
        Element component = component(parsed, false);
        Element topics = component == null ? new Element("topics") : component.getChild("topics");
        if (topics == null) throw new IOException("Missing topics element");
        document = parsed;
        baseline = bytes;
        loaded = true;
        return topics.clone();
    }

    public synchronized boolean externallyChanged() throws IOException {
        return !Arrays.equals(baseline, readBytes());
    }

    public synchronized void save(Element topics, boolean backupDisk) throws IOException {
        validatePath();
        if (!loaded) throw new IOException("Storage has not been successfully loaded");
        if (!Files.isDirectory(path.getParent())) throw new IOException("Project metadata directory is missing");
        byte[] current = readBytes();
        if (!Arrays.equals(baseline, current) && !backupDisk) throw new ConflictException(path);
        Document output = document.clone();
        if (backupDisk && current != null) {
            Path backup = path.resolveSibling(path.getFileName() + "." + UUID.randomUUID() + ".bak");
            Files.write(backup, current, StandardOpenOption.CREATE_NEW);
            // Parseable external changes to unrelated components must survive conflict resolution too.
            try { output = parse(current); } catch (IOException corrupt) {
                // Explicit backup-and-save permits replacement of corrupt disk XML; backup contains exact bytes.
            }
        }
        Element component = component(output, true);
        Element replacement = topics.clone();
        Element old = component.getChild("topics");
        if (old != null) {
            for (org.jdom.Attribute attr : old.getAttributes()) {
                if (replacement.getAttribute(attr.getName()) == null) replacement.setAttribute(attr.clone());
            }
            java.util.Set<String> supplied = new java.util.HashSet<>();
            for (Element child : replacement.getChildren()) supplied.add(child.getQualifiedName());
            for (Element child : old.getChildren()) {
                if (!child.getName().equals("topic") && !child.getName().equals("trash") && !supplied.contains(child.getQualifiedName())) replacement.addContent(child.clone());
            }
            int index = component.indexOf(old);
            component.removeContent(old);
            component.addContent(index, replacement);
        } else component.addContent(replacement);
        byte[] bytes = serialize(output);
        // Check again after building/backup, before replacement.
        if (!Arrays.equals(current, readBytes())) throw new ConflictException(path);
        atomicWrite(path, bytes);
        document = output;
        baseline = bytes;
        Files.deleteIfExists(pendingPath());
    }

    public synchronized void preservePending(Element topics) throws IOException {
        atomicWrite(pendingPath(), serialize(new Document(topics.clone())));
    }
    public Element readPending() throws IOException { return parse(Files.readAllBytes(pendingPath())).getRootElement().clone(); }
    public void discardPending() throws IOException { Files.deleteIfExists(pendingPath()); }

    private byte[] readBytes() throws IOException {
        try { return Files.readAllBytes(path); }
        catch (NoSuchFileException missing) { return null; }
    }
    private void validatePath() throws IOException {
        for (Path candidate : new Path[]{path, path.getParent(), path.getParent().getParent()}) {
            if (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS) && WorkspaceDiscovery.isLink(candidate)) {
                throw new IOException("Refusing linked notes storage: " + candidate);
            }
        }
    }
    private static Document parse(byte[] bytes) throws IOException {
        SAXBuilder builder = new SAXBuilder();
        builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        builder.setFeature("http://xml.org/sax/features/external-general-entities", false);
        builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        try { return builder.build(new ByteArrayInputStream(bytes)); }
        catch (JDOMException error) { throw new IOException("Invalid notes XML", error); }
    }
    private static Element component(Document document, boolean create) throws IOException {
        Element root = document.getRootElement();
        if (root.getName().equals("CodeReadingNote") || root.getName().equals("component")
                && "CodeReadingNote".equals(root.getAttributeValue("name"))) return root;
        if (!root.getName().equals("project")) throw new IOException("Invalid notes document root");
        for (Element child : root.getChildren("component")) {
            if ("CodeReadingNote".equals(child.getAttributeValue("name"))) return child;
        }
        if (!create) return null;
        Element component = new Element("component").setAttribute("name", "CodeReadingNote");
        root.addContent(component);
        return component;
    }
    private static byte[] serialize(Document document) {
        return new XMLOutputter(Format.getPrettyFormat().setEncoding("UTF-8").setTextMode(Format.TextMode.PRESERVE))
                .outputString(document).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    private static void atomicWrite(Path target, byte[] bytes) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".notes-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
            // Fail instead of silently falling back to a non-atomic replacement.
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
    public static final class ConflictException extends IOException {
        public ConflictException(Path path) { super(path.toString()); }
    }
}
