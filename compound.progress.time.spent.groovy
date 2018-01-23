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

def calculateProgress(Issue issue, IssueLinkManager issueLinkManager, CustomFieldManager customFieldManager, Logger log) {
    double thisCompoundOriginalEstimate = 0
    double thisCompoundTimeSpent = 0
    double thisCompoundRemainingEstimate = 0
    double thisProgress = 0
    double compoundTimeSpentAndEstimate = 0
    Double compoundProgress = null
        
    def resolution = issue.getResolution();
    if (resolution != null) {
        return 1;
    }
    
    def status = issue.getStatus().getName();
    if ("Backlog".equals(status) && !("Epic".equals(issue.getIssueType().getName()))) {
        return 0;
    }
    def customOriginalEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Original Estimate");
    def customOriginalEstimate
    if(customOriginalEstimateField != null) {
        customOriginalEstimate = issue.getCustomFieldValue(customOriginalEstimateField);
    } 
    if (customOriginalEstimate != null) {
        thisCompoundOriginalEstimate = (double) customOriginalEstimate
    }
    
    def customTimeSpentField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Time Spent");
    def customTimeSpent
    if(customTimeSpentField != null) {
        customTimeSpent = issue.getCustomFieldValue(customTimeSpentField);
    }
    if (customTimeSpent != null) {
        thisCompoundTimeSpent = (double) customTimeSpent
    }
        
    def customRemainingEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Remaining Estimate");
    def customRemainingEstimate
    if(customRemainingEstimateField != null) {
        customRemainingEstimate = issue.getCustomFieldValue(customRemainingEstimateField);
    } 
    if (customRemainingEstimate != null) {
        thisCompoundRemainingEstimate = (double) customRemainingEstimate
    }
    
    // traversing direct children
    issueLinkManager.getOutwardLinks(issue.id).each {
        issueLink ->
        if (issueLink.issueLinkType.name == "Hierarchy"
            || issueLink.issueLinkType.name == "Epic-Story Link"
            || issueLink.issueLinkType.isSubTaskLinkType() == true) { 

            Issue childIssue = issueLink.getDestinationObject()
            
            def childStatus = childIssue.getStatus().getName();
            if ("Backlog".equals(childStatus) && !("Epic".equals(childIssue.getIssueType().getName()))) {
                return
            }

            // reading time spent custom - scripted - field on child
            def childOriginalEstimate = 0
            def customChildOriginalEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Original Estimate");
            def customChildOriginalEstimate
            if(customChildOriginalEstimateField != null) {
                customChildOriginalEstimate = childIssue.getCustomFieldValue(customChildOriginalEstimateField);
            } 
            if (customChildOriginalEstimate != null) {
                childOriginalEstimate = (double) customChildOriginalEstimate
            }
            
            // skipping unestimated issues
            if (childOriginalEstimate == 0) {
                return
            }
        
            // reading time spent custom - scripted - field on child
            def childTimeSpent = 0
            def customChildTimeSpentField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Time Spent");
            def customChildTimeSpent
            if(customChildTimeSpentField != null) {
                customChildTimeSpent = childIssue.getCustomFieldValue(customChildTimeSpentField);
            } 
            if (customChildTimeSpent != null) {
                childTimeSpent = (double) customChildTimeSpent
            }
            
            // reading remaining estimate custom - scripted - field on child
            def childEstimate = 0
            def customEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Remaining Estimate");
            def customEstimate
            if(customEstimateField != null) {
                customEstimate = childIssue.getCustomFieldValue(customEstimateField);
            } 
            if (customEstimate != null) {
                childEstimate = (double) customEstimate
            }
            
            // reading this custom - scripted - field on child
            def childProgress = 0
            def customProgressField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Progress (Time Spent)");
            def customProgress
            if(customProgressField != null) {
                customProgress = childIssue.getCustomFieldValue(customProgressField);
            } 
            if (customProgress != null) {
                childProgress = (double) customProgress
            }

            // cumulating child progress with original estimate weight
            if (compoundProgress == null) {
                compoundProgress = 0
            }
            
            //log.info(issue.getKey() + "progress: " + childProgress)
            //log.info(issue.getKey() + "time spent: " + childTimeSpent)
            //log.info(issue.getKey() + "remaining: " + childEstimate)
            compoundProgress += childProgress * (childTimeSpent + childEstimate)
            compoundTimeSpentAndEstimate += (childTimeSpent + childEstimate)
        }
    }
    
    if (compoundProgress == null) {
        //log.info("time spent: " + thisCompoundTimeSpent)
        //log.info("estimate: " + thisCompoundRemainingEstimate)
        if (thisCompoundOriginalEstimate > 0 && (thisCompoundTimeSpent + thisCompoundRemainingEstimate) > 0) {
        	return thisCompoundTimeSpent / (thisCompoundTimeSpent + thisCompoundRemainingEstimate)
        } else {
            return null
        }
    } else {
        //log.info("progress: " + compoundProgress)
        //log.info("estimate: " + compoundTimeSpentAndEstimate)
        if (compoundTimeSpentAndEstimate > 0) {
        	return compoundProgress /= compoundTimeSpentAndEstimate
        } else {
            return null;
        }
    }
}

return (Double)calculateProgress(issue,issueLinkManager,customFieldManager,log)