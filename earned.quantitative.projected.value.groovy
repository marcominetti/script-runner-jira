// Number|Number
enableCache = { ->false }

def customUiFieldName = '$ EV Projected (d)'

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.CustomField
import org.apache.commons.lang3.StringUtils
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import org.apache.log4j.Logger

def log = Logger.getLogger('SCRIPTED')

def customCompoundOriginalEstimateField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Original Estimate");
def customCompoundProgressField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Progress");
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

Double calculateEstimate(Issue issue, List circularityCache, CustomField customCompoundOriginalEstimateField, CustomField customCompoundProgressField, Logger log, Integer level) {
  
  def pad = StringUtils.repeat(" ", level * 2)
  Double result

  // avoiding circularity
  if (circularityCache.contains(issue) == false) {
    circularityCache.add(issue)

    // getting compound original estimate
    Double compoundOriginalEstimate = getCustomFieldValue(issue, customCompoundOriginalEstimateField)
    log.info(String.format("%sget %s for %s: %s", pad, customCompoundOriginalEstimateField.getName(), issue.getKey(), compoundOriginalEstimate))

    // getting compound progress
    Double compoundProgress = getCustomFieldValue(issue, customCompoundProgressField)
    log.info(String.format("%sget %s for %s: %s", pad, customCompoundProgressField.getName(), issue.getKey(), compoundProgress))

    result = compoundOriginalEstimate * compoundProgress
  }
  return result.round(2)
}

Double result = calculateEstimate(issue, circularityCache, customCompoundOriginalEstimateField, customCompoundProgressField, log, 0)

// memoizing data in number field (for Portfolio)
log.info(String.format("update %s for %s: %s", customUiField.getName(), issue.getKey(), result))
customUiField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(customUiField), result), new DefaultIssueChangeHolder());

return result