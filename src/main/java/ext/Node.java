package ext;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;

public class Node {
    private final OpenFileDescriptor openFileDescriptor;

    /**
     * OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, offset);
     *         Node node = new Node(descriptor);
     * @param openFileDescriptor
     */
    public Node(OpenFileDescriptor openFileDescriptor) {
        this.openFileDescriptor = openFileDescriptor;
    }

    public OpenFileDescriptor getOpenFileDescriptor() {
        return openFileDescriptor;
    }
}
