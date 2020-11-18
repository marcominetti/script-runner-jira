// FreeText|Text
enableCache = { ->false }
def customFieldName = "Earned Qualitative Commercial Value"

import com.atlassian.jira.ComponentManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import org.apache.log4j.Logger
import org.apache.log4j.Level

def log = Logger.getLogger("SCRIPTED")
def customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(customFieldName);

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

Double result = getCustomFieldValue(issue, customField)
if (result > 0) {
  return result.round(2) + "d"
} else {
  return "n.d.";
}