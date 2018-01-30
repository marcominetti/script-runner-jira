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
    double thisCompoundRemainingEstimate = 0
    double thisCompoundOriginalEstimate = 0
    def compoundProgress = 0
    
    def resolution = issue.getResolution();
    if (resolution != null) {
        return 1;
    }
    
    def customRemainingEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Remaining Estimate");
    def customRemainingEstimate
    if(customRemainingEstimateField != null) {
        customRemainingEstimate = issue.getCustomFieldValue(customRemainingEstimateField);
    } 
    if (customRemainingEstimate != null) {
        thisCompoundRemainingEstimate = (double) customRemainingEstimate
    }
    
    def customOriginalEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Original Estimate");
    def customOriginalEstimate
    if(customOriginalEstimateField != null) {
        customOriginalEstimate = issue.getCustomFieldValue(customOriginalEstimateField);
    } 
    if (customOriginalEstimate != null) {
        thisCompoundOriginalEstimate = (double) customOriginalEstimate
    }

    if (thisCompoundOriginalEstimate > 0) {
    	compoundProgress = 1 - (thisCompoundRemainingEstimate / thisCompoundOriginalEstimate)
    }
    
    return compoundProgress;
}

return (Double)calculateProgress(issue,issueLinkManager,customFieldManager,log)