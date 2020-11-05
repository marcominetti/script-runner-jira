// FreeText|Text
enableCache = { ->true }

import com.atlassian.jira.ComponentManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;

import org.apache.log4j.Logger
import org.apache.log4j.Level

def log = Logger.getLogger("COMPUTED")
log.setLevel(Level.DEBUG)

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def customFieldName = "Compound Commercial Estimate"

def customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(customFieldName);
def customValue
if (customField != null) {
  customValue = issue.getCustomFieldValue(customField);
}
Double doubleValue = 0
if (customValue != null) {
  doubleValue = (double) customValue
}

if (doubleValue > 0) {
  return (doubleValue).round(2) + "d"
} else {
  return "n.d.";
}