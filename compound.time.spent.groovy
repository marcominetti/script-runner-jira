// Number|Number
enableCache = { ->false
}

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
log.setLevel(Level.DEBUG)

def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Time Spent");
def circularityCache = []

Double getCustomFieldValue(Issue issue, CustomField customField) {
  def customValue
  if (customField != null) {
    customValue = issue.getCustomFieldValue(customField);
  }
  if (customValue != null) {
    return (double) customValue
  }
  return 0
}

Double calculateTimeSpent(Issue issue, List circularityCache, IssueLinkManager issueLinkManager, CustomField customField, Logger log, Integer level) {
  def pad = StringUtils.repeat(" ", level)
  log.info(String.format("%sbegin calculate %s for %s", pad, customField.getName(), issue.getKey()))

  Double result

  // avoiding circularity
  if (circularityCache.contains(issue) == false) {
    circularityCache.add(issue)

    // getting time spent
    Double thisTimeSpent = 0
    def timespent = issue.getTimeSpent()
    if (timespent > 0) {
      thisTimeSpent = (double) timespent / (8 * 3600)
    }

    log.info(String.format("%sthis compound time spent for %s: %s", pad, issue.getKey(), thisTimeSpent))

    // checking issue type
    String issueTypeName = issue.getIssueType().getName()
    switch (issueTypeName) {
    case "Milestone":
      if (thisTimeSpent == 0) {
        // traversing direct children
        Double subsTimeSpent = 0
        issueLinkManager.getOutwardLinks(issue.id).each {
          issueLink ->
          Issue childIssue = issueLink.getDestinationObject()
          String childIssueTypeName = childIssue.getIssueType().getName()
          log.info(String.format("%sprocessing child of %s: %s", pad, issue.getKey(), childIssue.getKey()))
          if (issueLink.issueLinkType.name == "Hierarchy") {
            if (childIssue.getIssueType().getName() == "Epic") {
              Double childTimeSpent = calculateTimeSpent(childIssue, circularityCache, issueLinkManager, customField, log, level + 1)
              log.info(String.format("%schild compound time spent for %s: %s", pad, childIssue.getKey(), childTimeSpent))
              subsTimeSpent += childTimeSpent
            }
          }
        }
        result = subsTimeSpent
      } else {
        result = thisTimeSpent
      }
      break
    case "Epic":
      result = thisTimeSpent
      break;
    default:
      result = 0
      break;
    }
  }
  log.info(String.format("%send calculate %s for %s: %s", pad, customField.getName(), issue.getKey(), result))
  return result
}
return calculateTimeSpent(issue, circularityCache, issueLinkManager, customField, log, 0)