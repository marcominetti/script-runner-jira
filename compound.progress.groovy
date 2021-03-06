// Number|Number
enableCache = { ->false }
def customFieldName = "Compound Progress"
def customProjectedEstimateName = "Compound Projected Estimate"
def customSpentFieldName = "Compound Time Spent"
def customUiFieldName = "∑ Progress (%)"

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
def customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(customFieldName);
def customRemainingField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(customProjectedEstimateName);
def customSpentField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(customSpentFieldName);
def customUiField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(customUiFieldName);
def circularityCache = []

Double getCustomFieldValue(Issue issue, CustomField customField) {
  def customValue
  if (customField != null) {
    customValue = issue.getCustomFieldValue(customField);
  }
  if (customValue != null) {
    return customValue as Double;
  }
  return 0
}

Double calculateEstimate(Issue issue, List circularityCache, IssueLinkManager issueLinkManager, CustomField customField, CustomField customRemainingField, CustomField customSpentField, Logger log, Integer level) {
  def pad = StringUtils.repeat(" ", level*2)
  log.info(String.format("%sbegin calculate %s for %s", pad, customField.getName(), issue.getKey()))
  
  Double result
  
  // avoiding circularity
  if (circularityCache.contains(issue) == false) {
    circularityCache.add(issue)

    // checking issue type
    String issueTypeName = issue.getIssueType().getName()
    switch (issueTypeName) {
      case "Milestone":
      case "Epic":
      case "Story":
      case "Task":
      case "Bug":
      case "Change Request":
      case "Spike":
        // getting this compound remaining estimate from child
        Double projectedEstimate = getCustomFieldValue(issue, customRemainingField)
        log.info(String.format("%sget %s for %s: %s", pad, customRemainingField.getName(), issue.getKey(), projectedEstimate))
        // getting this compound remaining estimate from child
        Double timeSpent = getCustomFieldValue(issue, customSpentField)
        log.info(String.format("%sget %s for %s: %s", pad, customSpentField.getName(), issue.getKey(), timeSpent))
        if (projectedEstimate > 0) {
        	result = (timeSpent / projectedEstimate) as Double;
        } else {
            return 0
        }
        break;
      case "Sub-task":
      default:
        result = 0
        break;
    }
  }
  log.info(String.format("%send calculate %s for %s: %s", pad, customField.getName(), issue.getKey(), result))
  return result
}

Double result = calculateEstimate(issue, circularityCache, issueLinkManager, customField, customRemainingField, customSpentField, log, 0)

// memoizing data in number field (for Portfolio)
log.info(String.format("update %s for %s: %s", customUiField.getName(), issue.getKey(), result))
customUiField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(customUiField), result.round(2) * 100), new DefaultIssueChangeHolder());

return result