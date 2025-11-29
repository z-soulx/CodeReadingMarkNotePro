package jp.kitabatakep.intellij.plugins.codereadingnote;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.pom.Navigatable;

import java.io.File;

public class TopicLine implements Navigatable
{
    private int line;
    private VirtualFile file;
    private String note;
    private Project project;
    private Topic topic;
    private boolean inProject;
    private String relativePath;
    private String url;
    @Deprecated
    private int bookmarkHash;
    private String bookmarkUid;
    
    // 新增：分组引用
    private TopicGroup group;

    public static TopicLine createByAction(Project project, Topic topic, VirtualFile file, int line, String note)
    {
        VirtualFile projectBase = LocalFileSystem.getInstance().findFileByPath(project.getBasePath());
        boolean inProject = VfsUtilCore.isAncestor(projectBase, file, true);

        return new TopicLine(project, topic, file, line, note, inProject,
            VfsUtilCore.getRelativePath(file, projectBase), file.getUrl());
    }

    public static TopicLine createByImport(Project project, Topic topic, String url, int line, String note, boolean inProject, String relativePath, String bookmarkUid)
    {
        VirtualFile file;
        String projectBase = project.getBasePath();
        if (inProject) {
            file = LocalFileSystem.getInstance().findFileByPath(projectBase + File.separator + relativePath);
        } else {
            file = VirtualFileManager.getInstance().findFileByUrl(url);
        }
        return new TopicLine(project, topic, file, line, note, inProject, relativePath, url,bookmarkUid);
    }

    private TopicLine(Project project, Topic topic, VirtualFile file, int line, String note, boolean inProject, String relativePath, String url)
    {
        this.project = project;
        this.topic = topic;
        this.line = line;
        this.note = note;
        this.file = file;
        this.inProject = inProject;
        this.relativePath = relativePath;
        this.url = url;
    }
    private TopicLine(Project project, Topic topic, VirtualFile file, int line, String note, boolean inProject, String relativePath, String url,String bookmarkUid)
    {
        this.project = project;
        this.topic = topic;
        this.line = line;
        this.note = note;
        this.file = file;
        this.inProject = inProject;
        this.relativePath = relativePath;
        this.url = url;
        this.bookmarkUid = bookmarkUid;
    }

    public VirtualFile file()
    {
        return file;
    }

    public int line() { return line; }
    public int bookmarkHash() { return bookmarkHash; }
    public void setBookmarkHash(int hash) {  bookmarkHash = hash;}

    public String getBookmarkUid() {
        return bookmarkUid;
    }

    public void setBookmarkUid(String bookmarkUid) {
        this.bookmarkUid = bookmarkUid;
    }

    public void modifyLine(int newLine) {  line = newLine; }

    public String relativePath() { return relativePath; }

    public String note() { return note != null ? note : ""; }

    public void setNote(String note)
    {
        this.note = note;
        topic.touch();
    }

    public String url() { return url; }

    public String pathForDisplay()
    {
        if (inProject) {
            return relativePath;
        } else if (isValid()) {
            return file.getPath();
        } else {
            return url;
        }
    }

    public Topic topic() { return topic; }
    
    // 新增：分组相关方法
    public TopicGroup getGroup() { 
        return group; 
    }
    
    public void setGroup(TopicGroup group) { 
        this.group = group; 
    }
    
    public boolean hasGroup() {
        return group != null;
    }
    
    public String getGroupName() {
        return group != null ? group.name() : null;
    }

    public boolean inProject() { return inProject; }

    /**
     * Check if the file is valid.
     * Note: This checks the current cached file reference. 
     * Use refreshFile() first if you need to re-check after branch switch.
     */
    public boolean isValid() {
        return file != null && file.isValid();
    }
    
    /**
     * Try to refresh the file reference by re-looking up from the stored path.
     * This is useful when the file might have become available again (e.g., after switching branches).
     * @return true if the file was successfully refreshed and is now valid
     */
    public boolean refreshFile() {
        // If already valid, no need to refresh
        if (file != null && file.isValid()) {
            return true;
        }
        
        // Try to re-lookup the file
        VirtualFile newFile = null;
        String projectBase = project.getBasePath();
        
        if (inProject && relativePath != null && projectBase != null) {
            // For in-project files, use relative path
            newFile = LocalFileSystem.getInstance().findFileByPath(projectBase + File.separator + relativePath);
        }
        
        if (newFile == null && url != null) {
            // Fallback to URL lookup
            newFile = VirtualFileManager.getInstance().findFileByUrl(url);
        }
        
        if (newFile != null && newFile.isValid()) {
            this.file = newFile;
            return true;
        }
        
        return false;
    }
    
    /**
     * Check validity with auto-refresh attempt.
     * This will try to refresh the file reference if it's currently invalid.
     * @return true if file is valid (either already valid or successfully refreshed)
     */
    public boolean isValidWithRefresh() {
        if (isValid()) {
            return true;
        }
        // Try to refresh and check again
        return refreshFile();
    }

    public OpenFileDescriptor openFileDescriptor()
    {
        return new OpenFileDescriptor(project, file, line, -1, true);
    }

    @Override
    public boolean canNavigate() {
        // Auto-refresh before checking navigation capability
        if (!isValid()) {
            refreshFile();
        }
        return isValid() && openFileDescriptor().canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        // Auto-refresh before checking
        if (!isValid()) {
            refreshFile();
        }
        return isValid() && openFileDescriptor().canNavigateToSource();
    }

    @Override
    public void navigate(boolean requestFocus) {
        // Auto-refresh before navigation
        if (!isValid()) {
            refreshFile();
        }
        if (isValid() && openFileDescriptor().canNavigate()) {
            openFileDescriptor().navigate(requestFocus);
        }
    }
}
