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
    Double thisEstimate = 0
    Double subsEstimate = 0
    Double compoundEstimate = 0
    
    // avoiding circularity
    if (circularityCache.contains(issue) == false) {
        circularityCache.add(issue)
        
        def resolution = issue.getResolution();
        if (resolution == null) {
            // getting remaining estimate
            def status = issue.getStatus().getName();
    		if ("Blocked".equals(status)) {
                def estimate = issue.getEstimate()
                if (estimate > 0) {
                    thisEstimate  = (double) estimate / (8 * 3600)
                }
            }
        
            // traversing direct children
            issueLinkManager.getOutwardLinks(issue.id).each {
                issueLink ->
                if (issueLink.issueLinkType.name == "Hierarchy"
                    || issueLink.issueLinkType.name == "Epic-Story Link"
                    || issueLink.issueLinkType.isSubTaskLinkType() == true) { 

                    // reading this custom - scripted - field on child (hopefully triggering deep calculation)
                    Double childEstimate
                    Issue childIssue = issueLink.getDestinationObject()
                    def customEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Blocked Estimate");
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
    }
    
    // tree compound wins over issue estimate (if issue is not resolved)
    compoundEstimate = (subsEstimate > 0) ? subsEstimate : thisEstimate

    return compoundEstimate;
}

return (Double)calculateEstimate(issue,circularityCache,issueLinkManager,customFieldManager,log)