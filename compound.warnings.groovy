enableCache = { ->false }

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import com.atlassian.jira.ComponentManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.resolution.Resolution
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import org.apache.log4j.Logger
import org.apache.log4j.Level

def log = Logger.getLogger("SCRIPTED")

def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def customWarningField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Warnings");
def circularityCache = []

def createAnomaly(List<Map<String, String>> anomalies, Issue issue, String level, String title, String description) {
  Map<String, String> anomaly = new HashMap<String, String>()
  anomaly.put("title", title)
  anomaly.put("description", description)
  anomaly.put("issue", issue.getKey())
  anomaly.put("level", level)
  anomaly.put("resolution", (issue.getResolution() != null) ? "true" : "false")
  anomaly.put("status", issue.getStatus().getName())
  anomaly.put("type", issue.getIssueType().getName())
  anomalies.add(anomaly)
}

// Checking for Original Estimate
def checkForOriginalEstimate(Issue issue, IssueLinkManager issueLinkManager, List<Map<String, String>> anomalies) {

  String issueTypeName = issue.getIssueType().getName()
  switch (issueTypeName) {
  case "Milestone":
    if (issue.getOriginalEstimate() == null || issue.getOriginalEstimate() == 0) {
      createAnomaly(anomalies, issue, "WARN", issueTypeName + " not estimated", "For this issue is not defined original estimate.")
    }
    issueLinkManager.getOutwardLinks(issue.id).each {
      issueLink ->
      Issue childIssue = issueLink.getDestinationObject()
      String childIssueTypeName = childIssue.getIssueType().getName()
      if (issueLink.issueLinkType.name == "Hierarchy" && childIssueTypeName == "Epic") {
        if (childIssue.getOriginalEstimate() == null || childIssue.getOriginalEstimate() == 0) {
          createAnomaly(anomalies, childIssue, "WARN", childIssueTypeName + " not estimated", "For this issue is not defined original estimate.")
        }
          issueLinkManager.getOutwardLinks(childIssue.id).each {
          subIssueLink ->
          Issue subChildIssue = subIssueLink.getDestinationObject()
          String subChildIssueTypeName = subChildIssue.getIssueType().getName()
          if (subIssueLink.issueLinkType.name == "Epic-Story Link" && 
          ((subChildIssueTypeName == "Story" 
          || subChildIssueTypeName == "Task" 
          || subChildIssueTypeName == "Bug" 
          || subChildIssueTypeName == "Change Request" 
          || subChildIssueTypeName == "Spike"))) {
            if (subChildIssue.getOriginalEstimate() == null || subChildIssue.getOriginalEstimate() == 0) {
              createAnomaly(anomalies, subChildIssue, "WARN", "Operative issue not estimated", "For this issue is not defined original estimate.")
            }
          }
        }
      }
    }
    break;
  case "Epic":
    if (issue.getOriginalEstimate() == null || issue.getOriginalEstimate() == 0) {
      createAnomaly(anomalies, issue, "WARN", issueTypeName + " not estimated", "For this issue is not defined original estimate.")
    }
    issueLinkManager.getOutwardLinks(issue.id).each {
      issueLink ->
      Issue childIssue = issueLink.getDestinationObject()
      String childIssueTypeName = childIssue.getIssueType().getName()
      if (issueLink.issueLinkType.name == "Epic-Story Link" && 
      ((childIssueTypeName == "Story" 
      || childIssueTypeName == "Task" 
      || childIssueTypeName == "Bug" 
      || childIssueTypeName == "Change Request" 
      || childIssueTypeName == "Spike"))) {
        if (childIssue.getOriginalEstimate() == null || childIssue.getOriginalEstimate() == 0) {
          createAnomaly(anomalies, childIssue, "WARN", "Operative issue not estimated", "For this issue is not defined original estimate.")
        }
      }
    }
    break;
  case "Story":
  case "Task":
  case "Bug":
  case "Change Request":
  case "Spike":
    if (issue.getOriginalEstimate() == null || issue.getOriginalEstimate() == 0) {
      createAnomaly(anomalies, issue, "WARN", "Operative issue not estimated", "For this issue is not defined original estimate.")
    }
    break;
  }
}

// Checking for linked Issues
def checkForLinkedIssues(Issue issue, IssueLinkManager issueLinkManager, List<Map<String, String>> anomalies, Logger log) {

  def numberOfMilestones = 0
  def numberOfEpics = 0

  def projectId = issue.getProjectId()
  def issueManager = ComponentAccessor.getIssueManager()
  List<Issue> allIssues = issueManager.getIssueObjects(issueManager.getIssueIdsForProject(projectId))
  allIssues.each {
    i ->
    if (i.getIssueType().getName() == "Milestone") {
      numberOfMilestones++
    } else if (i.getIssueType().getName() == "Epic") {
      numberOfEpics++
    }
  }

  String issueTypeName = issue.getIssueType().getName()
  switch (issueTypeName) {
  case "Epic":
    if (numberOfMilestones > 0) {
      List<IssueLink> inwardLinks = issueLinkManager.getInwardLinks(issue.id)
      if (inwardLinks.isEmpty()) {
        createAnomaly(anomalies, issue, "WARN", "Epic not linked", "This issue is not linked. Please relates Epics to a Milestone of the project.")
      }
    }
    break;
  case "Story":
  case "Task":
  case "Bug":
  case "Change Request":
  case "Spike":
    if (numberOfEpics > 0) {
      List<IssueLink> inwardLinks = issueLinkManager.getInwardLinks(issue.id)
      if (inwardLinks.isEmpty()) {
        createAnomaly(anomalies, issue, "WARN", "Operative issue not linked", "This issue is not linked. Please relates this issue to an Epic of the project.")
      }
    }
    break;
  }
}

String calculateAnomalies(Issue issue, List circularityCache, IssueLinkManager issueLinkManager, CustomField customWarningField, Logger log) {
  List<Map<String, String>> anomalies = new ArrayList<Map<String, String>>()
  String result
  def json

  // avoiding circularity
  if (circularityCache.contains(issue) == false) {
    circularityCache.add(issue)
    checkForOriginalEstimate(issue, issueLinkManager, anomalies)
    checkForLinkedIssues(issue, issueLinkManager, anomalies, log)
  }
  json = new JsonBuilder(anomalies);
  result = json as String;
}

return calculateAnomalies(issue, circularityCache, issueLinkManager, customWarningField, log)