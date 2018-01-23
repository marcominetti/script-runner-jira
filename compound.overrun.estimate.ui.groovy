// FreeText|Text
enableCache = {-> false}

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
def customFieldName = "Compound Overrun Estimate"

def customField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(customFieldName);
def customValue
if(customField != null) {
    customValue = issue.getCustomFieldValue(customField);
} 
if (customValue != null) {
    return ((double) customValue).round(2) + "d"
} else {
    return null;
}