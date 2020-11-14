enableCache = { ->false }

import groovy.json.JsonSlurper
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.CustomField;
import groovy.xml.MarkupBuilder
import org.apache.log4j.Logger

def log = Logger.getLogger("SCRIPTED")
def customWarning = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Warnings");

StringWriter writer = new StringWriter()
MarkupBuilder builder = new MarkupBuilder(writer)
List<Map<String, String>> warnings = new ArrayList<Map<String, String>>()

def getCustomFieldValue(Issue issue, CustomField customField) {
  def customValue
  if (customField != null) {
    customValue = issue.getCustomFieldValue(customField);
  }
  if (customValue != null) {
    return customValue
  }
}

def json = getCustomFieldValue(issue, customWarning)
warnings = new JsonSlurper().parseText(json.toString()) as List;

builder.div(style: "display: inline-flex;") {
  table(style: "width:100%; border-collapse: collapse;") {
    tbody {
      warnings.each {
        map ->
        tr {
          td(style: "font-weight: bold; text-align: left;", map.title + ":")
        }
        tr {
          td(style: "text-align: left; padding-left: 5px; padding-bottom: 10px;","- " + map.description)
        }
      }
    }
  }
}
return writer.toString()