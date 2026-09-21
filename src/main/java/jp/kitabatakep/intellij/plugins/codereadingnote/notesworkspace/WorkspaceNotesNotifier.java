package jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace;

import com.intellij.util.messages.Topic;

public interface WorkspaceNotesNotifier {
    Topic<WorkspaceNotesNotifier> TOPIC = Topic.create("workspace notes changed", WorkspaceNotesNotifier.class);
    void changed(NoteProjectContext source);
}
