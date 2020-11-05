// Number|Number
enableCache = { ->true }

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

  // avoiding circularity
  if (circularityCache.contains(issue) == false) {
    circularityCache.add(issue)

    // getting original estimate
    def estimate = issue.getOriginalEstimate()
    if (estimate > 0) {
      thisEstimate = (double) estimate / (8 * 3600)
    }

    // checking issue type
    if (issue.getIssueType().getName() == "Milestone" || issue.getIssueType().getName() == "Epic") {
      if (thisEstimate > 0) {
        return thisEstimate
      } else if (issue.getIssueType().getName() == "Milestone") {
        Double subsEstimate = 0
        // traversing direct children
        issueLinkManager.getOutwardLinks(issue.id).each {
          issueLink ->
          if (issueLink.issueLinkType.name == "Hierarchy") {
            Issue childIssue = issueLink.getDestinationObject()
            if (childIssue.getIssueType().getName() == "Epic") {
              // reading this custom - scripted - field on child (hopefully triggering deep calculation)
              Double childEstimate

              def customEstimateField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Commercial Estimate");
              def customEstimate
              if (customEstimateField != null) {
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
        return subsEstimate
      }
    }
  }
  return 0
}

return (Double) calculateEstimate(issue, circularityCache, issueLinkManager, customFieldManager, log)