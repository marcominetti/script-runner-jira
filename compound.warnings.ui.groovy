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
  List<Map<String, String>> warningsList = new ArrayList<Map< String, String>>()

  // get warnings from Compound Warnings
  def warnings = getCustomFieldValue(issue, customField)
  warningsList = new JsonSlurper().parseText(warnings.toString()) as List;

  if (!warningsList.isEmpty()) {
    // creating warning table
    builder.div(style: "display: inline-flex;") {
      table(style: "width:100%; border-collapse: collapse; border: 1px solid #ccc;") {
        tbody {
          tr (style: "padding: 3px; border: 1px solid #ccc;"){
            th(style: "padding: 3px; background-color: #ddd;", "Issue Key")
            th(style: "padding: 3px; background-color: #ddd;", "Issue Type")
            th(style: "padding: 3px; background-color: #ddd;", "Title")
            th(style: "padding: 3px; background-color: #ddd;", "Description")
          }
          warningsList.each {
            map ->tr {
              td(style: "padding: 3px; border: 1px solid #ccc; background-color: #f5f5f5;", map.issue)
              td(style: "padding: 3px; border: 1px solid #ccc; background-color: #f5f5f5;", map.type)
              td(style: "padding: 3px; border: 1px solid #ccc;", map.title)
              td(style: "padding: 3px; border: 1px solid #ccc;", map.description)
            }
          }
        }
      }
    }
    return writer.toString()
  }
  return ""
}
return returnWarnings(issue, customWarning, writer, builder)