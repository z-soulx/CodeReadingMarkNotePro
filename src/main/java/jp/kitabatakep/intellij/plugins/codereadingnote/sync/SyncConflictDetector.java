package jp.kitabatakep.intellij.plugins.codereadingnote.sync;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicList;
import org.jetbrains.annotations.NotNull;

/**
 * Detector for sync conflicts
 * Checks if remote data is newer than local data
 */
@Service(Service.Level.PROJECT)
public final class SyncConflictDetector {
    
    private static final Logger LOG = Logger.getInstance(SyncConflictDetector.class);
    
    private final Project project;
    
    public SyncConflictDetector(@NotNull Project project) {
        this.project = project;
    }
    
    @NotNull
    public static SyncConflictDetector getInstance(@NotNull Project project) {
        return project.getService(SyncConflictDetector.class);
    }
    
    /**
     * Check for remote updates and detect conflicts
     * @return ConflictDetectionResult indicating if there's a conflict
     */
    @NotNull
    public ConflictDetectionResult checkRemoteUpdate() {
        try {
            // Get sync configuration
            SyncConfig config = SyncSettings.getInstance().getSyncConfig();
            
            if (!config.isEnabled()) {
                return ConflictDetectionResult.noConflict("Sync not enabled");
            }
            
            // Get sync provider
            SyncProvider provider = SyncProviderFactory.getProvider(config);
            if (provider == null) {
                return ConflictDetectionResult.error("Unsupported sync provider");
            }
            
            // Get local data info - use effective timestamp (lastSync or latest Topic updatedAt)
            SyncService syncService = SyncService.getInstance(project);
            long localLastSyncTime = syncService.getEffectiveLocalTimestamp();
            
            // Get local topic count and topicline count
            CodeReadingNoteService noteService = CodeReadingNoteService.getInstance(project);
            TopicList topicList = noteService.getTopicList();
            int localTopicCount = topicList != null ? topicList.getTopics().size() : 0;
            int localTopicLineCount = 0;
            if (topicList != null) {
                for (Topic topic : topicList.getTopics()) {
                    localTopicLineCount += topic.getTotalLineCount();
                }
            }
            
            // Get project identifier
            String projectIdentifier = project.getName()
                .replace('\\', '_')
                .replace('/', '_')
                .replace(':', '_')
                .replace('*', '_')
                .replace('?', '_')
                .replace('"', '_')
                .replace('<', '_')
                .replace('>', '_')
                .replace('|', '_');
            
            // Get remote last modified timestamp
            long remoteTimestamp = provider.getRemoteLastModifiedTime(project, config, projectIdentifier);
            
            // Calculate current local data MD5
            String currentLocalMd5 = syncService.calculateLocalDataMd5();
            String lastSyncedMd5 = SyncStatusService.getInstance(project).getLastLocalDataMd5();
            boolean localModified = !currentLocalMd5.equals(lastSyncedMd5);
            
            if (remoteTimestamp == 0) {
                // No remote data exists yet
                if (localModified) {
                    return ConflictDetectionResult.localModified(
                        localLastSyncTime, localTopicCount, localTopicLineCount);
                } else {
                    return ConflictDetectionResult.noConflict("No remote data, local synced");
                }
            }
            
            // Check time difference (allow 5 second tolerance for clock differences)
            boolean remoteNewer = remoteTimestamp > localLastSyncTime + 5000;
            
            // Decision matrix:
            // 1. Remote newer + Local modified = BOTH_MODIFIED (conflict, need user decision)
            // 2. Remote newer + Local unchanged = REMOTE_UPDATED (can auto-pull)
            // 3. Remote not newer + Local modified = LOCAL_MODIFIED (can auto-push)
            // 4. Remote not newer + Local unchanged = NO_CONFLICT (synced)
            
            if (remoteNewer && localModified) {
                // Both sides have changes - need user decision
                LOG.warn("Conflict detected: both remote and local have changes");
                
                // Fetch remote counts for display
                int remoteTopicCount = -1;
                int remoteTopicLineCount = -1;
                try {
                    SyncResult pullResult = provider.pull(project, config, projectIdentifier);
                    if (pullResult.isSuccess() && pullResult.getData() != null) {
                        String data = pullResult.getData();
                        remoteTopicCount = countTopicsInXml(data);
                        remoteTopicLineCount = countTopicLinesInXml(data);
                    }
                } catch (Exception e) {
                    LOG.warn("Could not get remote counts", e);
                }
                
                return ConflictDetectionResult.bothModified(
                    remoteTimestamp, localLastSyncTime,
                    remoteTopicCount, localTopicCount,
                    remoteTopicLineCount, localTopicLineCount
                );
                
            } else if (remoteNewer && !localModified) {
                // Remote has updates, local unchanged - can auto-pull
                
                // Fetch remote counts for display
                int remoteTopicCount = -1;
                int remoteTopicLineCount = -1;
                try {
                    SyncResult pullResult = provider.pull(project, config, projectIdentifier);
                    if (pullResult.isSuccess() && pullResult.getData() != null) {
                        String data = pullResult.getData();
                        remoteTopicCount = countTopicsInXml(data);
                        remoteTopicLineCount = countTopicLinesInXml(data);
                    }
                } catch (Exception e) {
                    LOG.warn("Could not get remote counts", e);
                }
                
                return ConflictDetectionResult.remoteUpdated(
                    remoteTimestamp, localLastSyncTime,
                    remoteTopicCount, localTopicCount,
                    remoteTopicLineCount, localTopicLineCount
                );
                
            } else if (!remoteNewer && localModified) {
                // Local has changes, remote not newer - can auto-push
                return ConflictDetectionResult.localModified(
                    localLastSyncTime, localTopicCount, localTopicLineCount
                );
                
            } else {
                // Both synced
                return ConflictDetectionResult.noConflict("Synced");
            }
            
        } catch (Exception e) {
            LOG.warn("Failed to check for remote updates", e);
            return ConflictDetectionResult.error("Failed to check remote: " + e.getMessage());
        }
    }
    
    /**
     * Count topics in XML data using proper XML parsing
     */
    private int countTopicsInXml(String xmlData) {
        if (xmlData == null || xmlData.isEmpty()) {
            return 0;
        }
        
        try {
            // Parse XML properly
            org.jdom.Element topicsElement = com.intellij.openapi.util.JDOMUtil.load(
                new java.io.StringReader(xmlData)
            );
            
            // Count direct <topic> children of <topics> element
            java.util.List<org.jdom.Element> topicElements = topicsElement.getChildren("topic");
            return topicElements.size();
            
        } catch (Exception e) {
            LOG.debug("Failed to parse XML for topic count, using fallback", e);
            
            // Fallback: Use regex to match only <topic> tags (not <topicLine> or <topicLines>)
            // Match <topic> or <topic >, but not <topicLine> or <topicLines>
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<topic[\\s>]");
            java.util.regex.Matcher matcher = pattern.matcher(xmlData);
            
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            
            return count;
        }
    }
    
    /**
     * Count topiclines in XML data using proper XML parsing
     * Handles both grouped and ungrouped topic structures
     */
    private int countTopicLinesInXml(String xmlData) {
        if (xmlData == null || xmlData.isEmpty()) {
            return 0;
        }
        
        try {
            // Parse XML properly
            org.jdom.Element topicsElement = com.intellij.openapi.util.JDOMUtil.load(
                new java.io.StringReader(xmlData)
            );
            
            // Count all <topicLine> elements in all possible locations
            int count = 0;
            java.util.List<org.jdom.Element> topicElements = topicsElement.getChildren("topic");
            
            for (org.jdom.Element topicElement : topicElements) {
                // Check for groups (new structure with TopicGroup support)
                org.jdom.Element groupsElement = topicElement.getChild("groups");
                if (groupsElement != null) {
                    // Count lines in each group
                    java.util.List<org.jdom.Element> groupElements = groupsElement.getChildren("group");
                    for (org.jdom.Element groupElement : groupElements) {
                        org.jdom.Element groupLinesElement = groupElement.getChild("topicLines");
                        if (groupLinesElement != null) {
                            count += groupLinesElement.getChildren("topicLine").size();
                        }
                    }
                    
                    // Count ungrouped lines
                    org.jdom.Element ungroupedLinesElement = topicElement.getChild("ungroupedLines");
                    if (ungroupedLinesElement != null) {
                        count += ungroupedLinesElement.getChildren("topicLine").size();
                    }
                } else {
                    // Legacy structure (no groups) - lines directly under topicLines
                    org.jdom.Element topicLinesElement = topicElement.getChild("topicLines");
                    if (topicLinesElement != null) {
                        count += topicLinesElement.getChildren("topicLine").size();
                    }
                }
            }
            
            return count;
            
        } catch (Exception e) {
            LOG.warn("Failed to parse XML for topicline count", e);
            
            // Fallback: Use regex to match <topicLine> tags
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<topicLine[\\s/>]");
            java.util.regex.Matcher matcher = pattern.matcher(xmlData);
            
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            
            return count;
        }
    }
}

