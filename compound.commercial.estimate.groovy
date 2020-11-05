// Number|Number
enableCache = { ->true }
def customFieldName = "Compound Commercial Estimate"

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

def log = Logger.getLogger("SCRIPTED")
log.setLevel(Level.DEBUG)

def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def customEstimateField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(customFieldName);
Double getCustomFieldValue(Issue issue, CustomField customField) {
  def customValue
  if (customField != null) {
    customValue = issue.getCustomFieldValue(customField);
  }
  if (customValue != null) {
    return (double) customValue
  }
  return null
}

// getting original estimate
Double thisEstimate = 0
def estimate = issue.getOriginalEstimate()
if (estimate > 0) {
  thisEstimate = (double) estimate / (8 * 3600)
}
log.info(String.format("this compound commercial estimate for %s: %s", issue.getKey(), thisEstimate))

// checking issue type
if (issue.getIssueType().getName() == "Milestone" || issue.getIssueType().getName() == "Epic") {
  if (thisEstimate > 0) {
    return thisEstimate
  } else if (issue.getIssueType().getName() == "Milestone") {
    // traversing direct children
    Double subsEstimate = 0
    issueLinkManager.getOutwardLinks(issue.id).each {
      issueLink ->
        Issue childIssue = issueLink.getDestinationObject()
        log.info(String.format("child compound commercial estimate for %s: %s", issue.getKey(), childIssue.getKey()))
        if (issueLink.issueLinkType.name == "Hierarchy") {
          if (childIssue.getIssueType().getName() == "Epic") {
            // reading this custom - scripted - field on child
            Double childEstimate = getCustomFieldValue(childIssue, customEstimateField)
            log.info(String.format("child compound commercial estimate for %s: %s", childIssue.getKey(), childEstimate))
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
return 0
