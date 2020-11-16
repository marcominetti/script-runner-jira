// Number|Number
enableCache = { ->false }

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.CustomField
import org.apache.commons.lang3.StringUtils
import org.apache.log4j.Logger

def log = Logger.getLogger('SCRIPTED')

def customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Earned Qualitative Commercial Value")
def customCompoundCommercialEstimateField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Commercial Estimate");
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

Double calculateEstimate(Issue issue, List circularityCache, CustomField customField, CustomField customCompoundCommercialEstimateField, CustomField customProgressField, Logger log, Integer level) {
  def pad = StringUtils.repeat(" ", level * 2)
  log.info(String.format("%sbegin calculate %s for %s", pad, customField.getName(), issue.getKey()))

  Double result

  // avoiding circularity
  if (circularityCache.contains(issue) == false) {
    circularityCache.add(issue)

    // getting compound commercial estimate
    Double compoundCommercialEstimate = getCustomFieldValue(issue, customCompoundCommercialEstimateField)
    log.info(String.format("%sget %s for %s: %s", pad, customCompoundCommercialEstimateField.getName(), issue.getKey(), compoundCommercialEstimate))

    // getting custom progress
    Double customProgress = getCustomFieldValue(issue, customProgressField)
    log.info(String.format("%sget %s for %s: %s", pad, customProgressField.getName(), issue.getKey(), customProgress))

    result = compoundCommercialEstimate * (customProgress).div(100)
  }
  log.info(String.format("%send calculate %s for %s: %s", pad, customField.getName(), issue.getKey(), result))
  return result
}

Double result = calculateEstimate(issue, circularityCache, customField, customCompoundCommercialEstimateField, customProgressField, log, 0)
if (result > 0) {
  return result.round(2) + "d"
} else {
  return "n.d.";
}