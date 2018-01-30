// Number|Number
enableCache = {-> false}

import com.atlassian.jira.ComponentManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.resolution.Resolution
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
    double thisCompoundRemainingEstimate = 0
    Double childCompoundOriginalEstimate = null
    Double childCompoundRemainingEstimate = null
    
    def resolution = issue.getResolution();
    if (resolution != null) {
        return 1;
    }

    def status = issue.getStatus().getName();
    // backlog kept for 2017 scheme compatibility
    if ("Blocked".equals(status) || "Backlog".equals(status)) {
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
            // backlog kept for 2017 scheme compatibility
            if ("Blocked".equals(childStatus) || "Backlog".equals(childStatus)) {
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
            if (childOriginalEstimate == null || childOriginalEstimate == 0) {
                return
            }

            // reading remaining estimate custom - scripted - field on child
            def childEstimate = 0
            def customChildEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Remaining Estimate");
            def customChildEstimate
            if(customChildEstimateField != null) {
                customChildEstimate = childIssue.getCustomFieldValue(customChildEstimateField);
            } 
            if (customChildEstimate != null) {
                childEstimate = (double) customChildEstimate
            }
                       
            if (childCompoundOriginalEstimate == null) {
                childCompoundOriginalEstimate = 0;
            }
            childCompoundOriginalEstimate += childOriginalEstimate
            if (childCompoundRemainingEstimate == null) {
                childCompoundRemainingEstimate = 0;
            }
            childCompoundRemainingEstimate += childEstimate
        }
    }
    
    def compoundProgress = 0;
    if (childCompoundRemainingEstimate == null || childCompoundOriginalEstimate == null) {
        if (thisCompoundOriginalEstimate > 0) {
    	    compoundProgress = 1 - (thisCompoundRemainingEstimate / thisCompoundOriginalEstimate)
            if (compoundProgress < 0) {
                compoundProgress = 0
            }
        }
    } else {
        if (childCompoundOriginalEstimate > 0) {
    	    compoundProgress = 1 - (childCompoundRemainingEstimate / childCompoundOriginalEstimate)
            if (compoundProgress < 0) {
                compoundProgress = 0
            }
        }
    }
    
    return compoundProgress;
}

return (Double)calculateProgress(issue,issueLinkManager,customFieldManager,log)