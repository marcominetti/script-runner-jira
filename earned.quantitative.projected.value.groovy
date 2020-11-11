// Number|Number
enableCache = { ->false }

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.issue.fields.CustomField
import org.apache.commons.lang3.StringUtils
import org.apache.log4j.Logger

def log = Logger.getLogger('SCRIPTED')

def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Earned Quantitative Projected Value")
def customCompoundOriginalEstimateField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Original Estimate");
def customCompoundProgressField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Progress");
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

Double calculateEstimate(Issue issue, List circularityCache, IssueLinkManager issueLinkManager, CustomField customField, CustomField customCompoundOriginalEstimateField, CustomField customCompoundProgressField, Logger log, Integer level) {
  def pad = StringUtils.repeat(" ", level * 2)
  log.info(String.format("%sbegin calculate %s for %s", pad, customField.getName(), issue.getKey()))

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
  log.info(String.format("%send calculate %s for %s: %s", pad, customField.getName(), issue.getKey(), result))
  return result
}

return calculateEstimate(issue, circularityCache, issueLinkManager, customField, customCompoundOriginalEstimateField, customCompoundProgressField, log, 0)