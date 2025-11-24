package jp.kitabatakep.intellij.plugins.codereadingnote.operations;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.messages.MessageBus;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicGroup;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicNotifier;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Service for handling batch operations on TopicLines
 * Including moving, reordering, etc.
 */
@Service(Service.Level.PROJECT)
public final class TopicLineOperationService {
    
    private static final Logger LOG = Logger.getInstance(TopicLineOperationService.class);
    
    private final Project project;
    
    public TopicLineOperationService(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * Move multiple TopicLines to target Topic
     * 
     * @param lines TopicLines to move
     * @param targetTopic Target Topic
     * @param insertIndex Insert position (-1 for end)
     * @return Success status
     */
    public boolean moveLines(@NotNull List<TopicLine> lines, 
                            @NotNull Topic targetTopic, 
                            int insertIndex) {
        if (lines.isEmpty()) {
            LOG.warn("Attempt to move empty list of lines");
            return false;
        }
        
        try {
            Topic sourceTopic = lines.get(0).topic();
            if (sourceTopic == null) {
                LOG.warn("Source topic is null");
                return false;
            }
            
            LOG.info(String.format("Moving %d lines from '%s' to '%s'", 
                lines.size(), sourceTopic.name(), targetTopic.name()));
            
            // Remove from source topic
            if (!sourceTopic.equals(targetTopic)) {
                for (TopicLine line : lines) {
                    sourceTopic.removeLine(line);
                }
            } else {
                for (TopicLine line : lines) {
                    sourceTopic.getLines().remove(line);
                }
            }
            
            // Add to target topic
            targetTopic.insertLines(lines, insertIndex);
            
            // Update bookmark groups
            updateBookmarkGroups(lines, targetTopic);
            
            // Notify changes
            notifyLinesMoved(lines, sourceTopic, targetTopic);
            
            return true;
        } catch (Exception e) {
            LOG.error("Failed to move lines", e);
            return false;
        }
    }
    
    /**
     * Move TopicLines between groups
     * Properly updates all internal structures
     */
    public boolean moveBetweenGroups(@NotNull List<TopicLine> lines, 
                                     @NotNull Topic topic,
                                     @Nullable TopicGroup targetGroup) {
        try {
            String targetGroupName = targetGroup != null ? targetGroup.name() : "Ungrouped";
            LOG.info(String.format("Moving %d lines to group '%s'", lines.size(), targetGroupName));
            
            for (TopicLine line : lines) {
                // 1. Remove from source group or ungrouped
                TopicGroup sourceGroup = line.getGroup();
                if (sourceGroup != null) {
                    sourceGroup.getLines().remove(line);
                    LOG.info("Removed line from source group: " + sourceGroup.name());
                } else {
                    topic.getUngroupedLines().remove(line);
                    LOG.info("Removed line from ungrouped");
                }
                
                // 2. Add to target group or ungrouped
                if (targetGroup != null) {
                    if (!topic.getGroups().contains(targetGroup)) {
                        topic.getGroups().add(targetGroup);
                    }
                    targetGroup.getLines().add(line);
                    LOG.info("Added line to target group: " + targetGroup.name());
                } else {
                    topic.getUngroupedLines().add(line);
                    LOG.info("Added line to ungrouped");
                }
                
                // 3. Update line's group reference
                line.setGroup(targetGroup);
            }
            
            topic.touch();
            notifyGroupChanged(lines, targetGroup);
            
            LOG.info("Successfully moved " + lines.size() + " lines to " + targetGroupName);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to move lines between groups", e);
            return false;
        }
    }
    
    /**
     * Reorder a TopicLine within the same Topic
     */
    public boolean reorderLine(@NotNull Topic topic, 
                              @NotNull TopicLine line, 
                              int newIndex) {
        try {
            topic.reorderLine(line, newIndex);
            LOG.info(String.format("Reordered line in topic '%s' to index %d", 
                topic.name(), newIndex));
            return true;
        } catch (Exception e) {
            LOG.error("Failed to reorder line", e);
            return false;
        }
    }
    
    /**
     * Batch remove TopicLines
     */
    public boolean removeLines(@NotNull List<TopicLine> lines) {
        if (lines.isEmpty()) {
            return false;
        }
        
        try {
            LOG.info(String.format("Removing %d lines", lines.size()));
            
            for (TopicLine line : lines) {
                Topic topic = line.topic();
                if (topic != null) {
                    topic.removeLine(line);
                }
            }
            
            return true;
        } catch (Exception e) {
            LOG.error("Failed to remove lines", e);
            return false;
        }
    }
    
    /**
     * Update bookmark group assignments
     */
    private void updateBookmarkGroups(@NotNull List<TopicLine> lines, 
                                     @NotNull Topic targetTopic) {
        for (TopicLine line : lines) {
            String uuid = line.getBookmarkUid();
            if (!StringUtil.isEmpty(uuid)) {
                BookmarkUtils.updateBookmarkDescription(
                    project, 
                    uuid, 
                    String.format("[%s] %s", targetTopic.name(), 
                        StringUtil.notNullize(line.note()))
                );
            }
        }
    }
    
    /**
     * Notify TopicLine move events
     */
    private void notifyLinesMoved(@NotNull List<TopicLine> lines,
                                 @NotNull Topic sourceTopic,
                                 @NotNull Topic targetTopic) {
        MessageBus messageBus = project.getMessageBus();
        TopicNotifier publisher = messageBus.syncPublisher(TopicNotifier.TOPIC_NOTIFIER_TOPIC);
        
        if (!sourceTopic.equals(targetTopic)) {
            for (TopicLine line : lines) {
                publisher.lineRemoved(sourceTopic, line);
            }
        }
        
        for (TopicLine line : lines) {
            publisher.lineAdded(targetTopic, line);
        }
    }
    
    /**
     * Notify group change events
     */
    private void notifyGroupChanged(@NotNull List<TopicLine> lines,
                                   @Nullable TopicGroup targetGroup) {
        MessageBus messageBus = project.getMessageBus();
        TopicNotifier publisher = messageBus.syncPublisher(TopicNotifier.TOPIC_NOTIFIER_TOPIC);
        
        for (TopicLine line : lines) {
            Topic topic = line.topic();
            if (topic != null) {
                publisher.lineAdded(topic, line);
            }
        }
    }
    
    public static TopicLineOperationService getInstance(@NotNull Project project) {
        return project.getService(TopicLineOperationService.class);
    }
}

