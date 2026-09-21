package jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace;

import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** IDE-free wire/storage utilities. Never discard extension elements or reorder business data. */
public final class NotesSyncXml {
    private NotesSyncXml() {}
    public static Element parse(String xml) throws IOException {
        SAXBuilder builder = new SAXBuilder();
        builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        builder.setFeature("http://xml.org/sax/features/external-general-entities", false);
        builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        try { return builder.build(new StringReader(xml)).getRootElement().clone(); }
        catch (JDOMException | RuntimeException e) { throw new IOException("Invalid XML", e); }
    }
    public static Element topics(String xml) throws IOException {
        Element e = parse(xml);
        if (!"topics".equals(e.getName())) throw new IOException("Invalid notes root");
        return e;
    }
    public static String write(Element e) { return new XMLOutputter(Format.getRawFormat()).outputString(e); }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public static String digest(Element element) {
        StringBuilder out = new StringBuilder(); canonical(element, out); return hash(out.toString());
    }
    private static void part(StringBuilder out, String value) { out.append(value.length()).append(':').append(value); }
    private static void canonical(Element e, StringBuilder out) {
        out.append('E'); part(out, e.getNamespaceURI()); part(out, e.getName());
        List<Attribute> attrs = new ArrayList<>(e.getAttributes());
        attrs.sort(Comparator.comparing(a -> a.getNamespaceURI() + ":" + a.getName()));
        for (Attribute a : attrs) { out.append('A'); part(out, a.getNamespaceURI()); part(out, a.getName()); part(out, a.getValue()); }
        for (Content c : e.getContent()) {
            if (c instanceof Element child) canonical(child, out);
            else if (c instanceof Text t && !t.getText().isEmpty() && (e.getChildren().isEmpty() || !t.getText().isBlank())) { out.append('T'); part(out, t.getText().replace("\r\n", "\n")); }
            else if (!(c instanceof Text)) { out.append('X'); part(out, c.toString()); }
        }
        out.append('/');
    }
    public static void atomicWrite(Path path, String xml) throws IOException {
        Files.createDirectories(path.getParent());
        if (Files.isSymbolicLink(path) || Files.isSymbolicLink(path.getParent())) throw new IOException("Linked storage");
        Path temp = Files.createTempFile(path.getParent(), ".notes-sync-", ".tmp");
        try {
            Files.writeString(temp, xml, StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) { channel.force(true); }
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
}
