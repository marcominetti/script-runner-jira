// Number|Number
enableCache = { ->false }

def customUiFieldName = '$ AC (d)'

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.CustomField
import org.apache.commons.lang3.StringUtils
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import org.apache.log4j.Logger

def log = Logger.getLogger('SCRIPTED')
def customCompoundTimeSpentField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Time Spent");
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

Double calculateEstimate(Issue issue, List circularityCache, CustomField customCompoundTimeSpentField, Logger log, Integer level) {
  def pad = StringUtils.repeat(" ", level * 2)
  log.info(String.format("%sbegin calculate %s for %s", pad, customCompoundTimeSpentField.getName(), issue.getKey()))

  Double result

  // avoiding circularity
  if (circularityCache.contains(issue) == false) {
    circularityCache.add(issue)

    // getting compound time spent
    Double compoundTimeSpent = getCustomFieldValue(issue, customCompoundTimeSpentField)
    log.info(String.format("%sget %s for %s: %s", pad, customCompoundTimeSpentField.getName(), issue.getKey(), compoundTimeSpent))
    
    result = compoundTimeSpent
  }
  log.info(String.format("%send calculate %s for %s: %s", pad, customCompoundTimeSpentField.getName(), issue.getKey(), result))
  return result
}

Double result = calculateEstimate(issue, circularityCache, customCompoundTimeSpentField, log, 0)

// memoizing data in number field (for Portfolio)
log.info(String.format("update %s for %s: %s", customUiField.getName(), issue.getKey(), result))
customUiField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(customUiField), result), new DefaultIssueChangeHolder());

return result