// Number|Number
enableCache = {-> false}

import com.atlassian.jira.ComponentManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;

import org.apache.log4j.Logger
import org.apache.log4j.Level

def log = Logger.getLogger("COMPUTED")
log.setLevel(Level.DEBUG)

def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def circularityCache = []

def calculateEstimate(Issue issue, List circularityCache, IssueLinkManager issueLinkManager, CustomFieldManager customFieldManager, Logger log) {
    Double thisTimeSpent = 0
    Double subsTimeSpent = 0
    Double thisEstimate = 0
    Double subsEstimate = 0
    Double compoundEstimate = 0
    
    // avoiding circularity
    if (circularityCache.contains(issue) == false) {
        circularityCache.add(issue)
        
        // getting remaining estimate
        def estimate = issue.getEstimate()
        if (estimate > 0) {
            thisEstimate  = (double) estimate / (8 * 3600)
        }

        // getting time spent
        def timespent = issue.getTimeSpent()
        if (timespent > 0) {
            thisTimeSpent  = (double) timespent / (8 * 3600)
        }

        // traversing direct children
        issueLinkManager.getOutwardLinks(issue.id).each {
            issueLink ->
            if (issueLink.issueLinkType.name == "Hierarchy"
                || issueLink.issueLinkType.name == "Epic-Story Link"
                || issueLink.issueLinkType.isSubTaskLinkType() == true) { 

                Issue childIssue = issueLink.getDestinationObject()

                // reading time spent
                Double childTimeSpent
                def customTimeSpentField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Time Spent");
                def customTimeSpent
                if(customTimeSpentField != null) {
                    customTimeSpent = childIssue.getCustomFieldValue(customTimeSpentField);
                } 
                if (customTimeSpent != null) {
                    childTimeSpent = (double) customTimeSpent
                }

                // adding each child estimate
                if (childTimeSpent != null) {
                    subsTimeSpent += childTimeSpent
                }

                // reading remaining estimate
                Double childEstimate
                def customEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Remaining Estimate");
                def customEstimate
                if(customEstimateField != null) {
                    customEstimate = childIssue.getCustomFieldValue(customEstimateField);
                } 
                if (customEstimate != null) {
                    childEstimate = (double) customEstimate
                }

                // adding each child estimate
                if (childEstimate != null) {
                    subsEstimate += childEstimate
                }
            }
        }
    }
    
    // tree compound wins over issue estimate (if issue is not resolved)
    compoundEstimate += (subsEstimate > 0) ? subsEstimate : thisEstimate
    compoundEstimate += (subsTimeSpent > 0) ? subsTimeSpent : thisTimeSpent
    
    return compoundEstimate;
}

return (Double)calculateEstimate(issue,circularityCache,issueLinkManager,customFieldManager,log)