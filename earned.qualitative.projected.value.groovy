// Number|Number
enableCache = { ->false }

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.issue.fields.CustomField
import org.apache.commons.lang3.StringUtils
import org.apache.log4j.Logger

def log = Logger.getLogger('SCRIPTED')

def customCompoundOriginalEstimateField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Original Estimate");
def customProgressField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Custom Progress");
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

Double calculateEstimate(Issue issue, List circularityCache, CustomField customCompoundOriginalEstimateField, CustomField customProgressField, Logger log, Integer level) {
  
  def pad = StringUtils.repeat(" ", level * 2)
  Double result

  // avoiding circularity
  if (circularityCache.contains(issue) == false) {
    circularityCache.add(issue)

    // getting compound original estimate
    Double compoundOriginalEstimate = getCustomFieldValue(issue, customCompoundOriginalEstimateField)
    log.info(String.format("%sget %s for %s: %s", pad, customCompoundOriginalEstimateField.getName(), issue.getKey(), compoundOriginalEstimate))

    // getting custom progress
    Double customProgress = getCustomFieldValue(issue, customProgressField)
    log.info(String.format("%sget %s for %s: %s", pad, customProgressField.getName(), issue.getKey(), customProgress))

    result = compoundOriginalEstimate * (customProgress).div(100)
  }
  return result
}

Double result = calculateEstimate(issue, circularityCache, customCompoundOriginalEstimateField, customProgressField, log, 0)