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
    double thisCompoundProjectedEstimate = 0
    double thisProgress = 0
    double compoundProjectedEstimate = 0
    Double compoundProgress = null
        
    def resolution = issue.getResolution();
    if (resolution != null) {
        return 1;
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
        
    def customProjectedEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Projected Estimate");
    def customProjectedEstimate
    if(customProjectedEstimateField != null) {
        customProjectedEstimate = issue.getCustomFieldValue(customProjectedEstimateField);
    } 
    if (customProjectedEstimate != null) {
        thisCompoundProjectedEstimate = (double) customProjectedEstimate
    }
    
    // traversing direct children
    issueLinkManager.getOutwardLinks(issue.id).each {
        issueLink ->
        if (issueLink.issueLinkType.name == "Hierarchy"
            || issueLink.issueLinkType.name == "Epic-Story Link"
            || issueLink.issueLinkType.isSubTaskLinkType() == true) { 

            Issue childIssue = issueLink.getDestinationObject()
            
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
            if (childOriginalEstimate == null || childOriginalEstimate == 0) {
                return
            }
            
            // reading remaining estimate custom - scripted - field on child
            def childEstimate = 0
            def customEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Projected Estimate");
            def customEstimate
            if(customEstimateField != null) {
                customEstimate = childIssue.getCustomFieldValue(customEstimateField);
            } 
            if (customEstimate != null) {
                childEstimate = (double) customEstimate
            }
            
            // reading this custom - scripted - field on child
            def childProgress = 0
            def customProgressField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Progress");
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
            
            compoundProgress += childProgress * childEstimate
            compoundProjectedEstimate += childEstimate
        }
    }
    
    if (compoundProgress == null) {
        if (thisCompoundOriginalEstimate > 0 && thisCompoundProjectedEstimate > 0) {
        	return thisCompoundTimeSpent / thisCompoundProjectedEstimate
        } else {
            return null
        }
    } else {
        if (compoundProjectedEstimate > 0) {
        	return compoundProgress /= compoundProjectedEstimate
        } else {
            return null;
        }
    }
}

return (Double)calculateProgress(issue,issueLinkManager,customFieldManager,log)