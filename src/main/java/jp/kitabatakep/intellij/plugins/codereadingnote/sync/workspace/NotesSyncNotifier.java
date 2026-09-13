package jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace;

import com.intellij.util.messages.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.NoteProjectContext;

public interface NotesSyncNotifier {
    Topic<NotesSyncNotifier> TOPIC = Topic.create("Workspace notes sync", NotesSyncNotifier.class);
    void changed(NoteProjectContext context);
}
