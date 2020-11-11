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
def customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Earned Qualitative Projected Value")
def customCompoundOriginalEstimateField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Original Estimate");
def customProgressField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Custom Progress");
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

Double calculateEstimate(Issue issue, List circularityCache, IssueLinkManager issueLinkManager, CustomField customField, CustomField customCompoundOriginalEstimateField, CustomField customProgressField, Logger log, Integer level) {
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
    Double customProgress = getCustomFieldValue(issue, customProgressField)
    log.info(String.format("%sget %s for %s: %s", pad, customProgressField.getName(), issue.getKey(), customProgress))

    result = compoundOriginalEstimate * customProgress
  }
  log.info(String.format("%send calculate %s for %s: %s", pad, customField.getName(), issue.getKey(), result))
  return result
}

return calculateEstimate(issue, circularityCache, issueLinkManager, customField, customCompoundOriginalEstimateField, customProgressField, log, 0)