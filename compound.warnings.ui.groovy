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

def getCustomFieldValue(Issue issue, CustomField customField) {
  def customValue
  if (customField != null) {
    customValue = issue.getCustomFieldValue(customField);
  }
  if (customValue != null) {
    return customValue
  }
  return null
}

def returnWarnings(Issue issue, CustomField customField, StringWriter writer, MarkupBuilder builder) {

  // get warnings from Compound Warnings
  def warnings = getCustomFieldValue(issue, customField)
  if (warnings != null) {
    List<Map<String, String>> warningsList = new ArrayList<Map<String, String>> ()
    warningsList = new JsonSlurper().parseText(warnings.toString()) as List;

    // creating warning table
    builder.div(style: "display: inline-flex;") {
      table(style: "width:100%; border-collapse: collapse;") {
        tbody {
          warningsList.each {
            map ->
            tr {
              td(style: "font-weight: bold; text-align: left;", map.title + ":")
            }
            tr {
              td(style: "text-align: left; padding-left: 5px; padding-bottom: 10px;", "- " + map.description)
            }
          }
        }
      }
    }
    return writer.toString()
  } else {
    return ""
  }
}

return returnWarnings(issue, customWarning, writer, builder)