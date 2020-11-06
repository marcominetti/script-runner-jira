// Number|Number
enableCache = { ->false }

import com.atlassian.jira.ComponentManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger
import org.apache.log4j.Level
def log = Logger.getLogger("SCRIPTED")

def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def circularityCache = []

def customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Commercial Estimate");
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

Double calculateEstimate(Issue issue, List circularityCache, IssueLinkManager issueLinkManager, CustomField customField, Logger log, Integer level) {
  def pad = StringUtils.repeat(" ", level)
  log.info(String.format("%sbegin calculate %s for %s", pad, customField.getName(), issue.getKey()))
  
  Double thisEstimate = 0
  Double subsEstimate = 0
  
  // avoiding circularity
  if (circularityCache.contains(issue) == false) {
    circularityCache.add(issue)

    // checking issue type
    if (issue.getIssueType().getName() == "Milestone" || issue.getIssueType().getName() == "Epic") {
      // getting original estimate
      def estimate = issue.getOriginalEstimate()
      if (estimate > 0) {
        thisEstimate = (double) estimate / (8 * 3600)
      }
      log.info(String.format("%sthis compound commercial estimate for %s: %s", pad, issue.getKey(), thisEstimate))
      if (thisEstimate == 0 && issue.getIssueType().getName() == "Milestone") {
        // traversing direct children
        issueLinkManager.getOutwardLinks(issue.id).each {
          issueLink ->
          Issue childIssue = issueLink.getDestinationObject()
          log.info(String.format("%sprocessing child of %s: %s", pad, issue.getKey(), childIssue.get
