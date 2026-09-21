package jp.kitabatakep.intellij.plugins.codereadingnote;

import org.jdom.Element;

/** Retains extension attributes/elements across edits of legacy notes. Runtime only. */
final class ElementTemplate {
    private Element original;
    void set(Element element) { original = element.clone(); }
    Element create(String name, String... knownChildren) {
        Element result = original == null ? new Element(name) : original.clone();
        for (String child : knownChildren) result.removeChildren(child);
        return result;
    }
    Element child(String name, String... knownChildren) {
        ElementTemplate child = new ElementTemplate();
        if (original != null && original.getChild(name) != null) child.set(original.getChild(name));
        return child.create(name, knownChildren);
    }
}
