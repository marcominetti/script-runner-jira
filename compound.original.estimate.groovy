// Number|Number
enableCache = { ->false }
def customFieldName = "Compound Original Estimate"
def customScrumFieldName = "Compound Original Estimate for Scrum"
def customUiFieldName = "∑ Original Estimate (d)"
Long customPortfolioFieldId = 10200

import com.atlassian.jira.ComponentManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.link.IssueLink
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.issue.link.IssueLinkTypeManager
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import org.apache.commons.lang3.StringUtils;
import groovyx.net.http.RESTClient;

import org.apache.log4j.Logger
import org.apache.log4j.Level
def log = Logger.getLogger("SCRIPTED")

def loggedInUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def issueManager = ComponentAccessor.getIssueManager()
def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def issueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager)
def customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(customFieldName);
def customScrumField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(customScrumFieldName);
def customUiField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(customUiFieldName);
def circularityCache = []

// cleaning unsupported hierarchy link
/*issueLinkManager.getOutwardLinks(issue.id).each {
    issueLink ->
    Issue parentIssue = issueLink.getSourceObject()
    Issue childIssue = issueLink.getDestinationObject()
    if (issueLink.issueLinkType.name == "Hierarchy") {
      if (!(parentIssue.getIssueType().getName() == "Milestone" && childIssue.getIssueType().getName() == "Epic")) {
        log.info(String.format("remove forbidden jira hierarchy link from %s to %s", parentIssue.getKey(), childIssue.getKey()))
        issueLinkManager.removeIssueLink(issueLink, loggedInUser)
      }
    }
}*/

if (issue.getIssueType().getName() == "Epic") {
  // get portfolio jira issue value
  Issue milestonePortfolio
  def customPortfolioField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(customPortfolioFieldId);
  def milestonePortfolioKey = issue.getCustomFieldValue(customPortfolioField);
  if (milestonePortfolioKey != null) {
    milestonePortfolio = issueManager.getIssueByCurrentKey(milestonePortfolioKey.toString())
  }
  if (milestonePortfolio != null) {
  	log.info(String.format("portfolio milestone for %s: %s", issue.getKey(), milestonePortfolio.getKey()))
  }

  // get eventual issue milestone linked inward hierarchy
  Issue milestoneJira
  circularityCache.add(issue)
  issueLinkManager.getInwardLinks(issue.id).each {
    issueLink ->
    Issue parentIssue = issueLink.getSourceObject()
    // avoiding circularity
    if (circularityCache.contains(parentIssue) == false) {
      circularityCache.add(parentIssue)
      if (issueLink.issueLinkType.name == "Hierarchy" && parentIssue.getIssueType().getName() == "Milestone") {
        if (milestonePortfolio != null && parentIssue.getId() != milestonePortfolio.getId()) {
          //log.info(String.format("remove mismatching jira milestone for %s: %s should be %s", issue.getKey(), parentIssue.getKey(), milestonePortfolio.getKey()))
          //issueLinkManager.removeIssueLink(issueLink, loggedInUser)
        } else if (milestonePortfolio == null) {
          //log.info(String.format("remove leftover jira milestone for %s: %s", issue.getKey(), parentIssue.getKey()))
          //issueLinkManager.removeIssueLink(issueLink, loggedInUser)
        } else {
          milestoneJira = parentIssue
        }
      }
    }
  
  }
  
  if (milestoneJira != null) {
  	log.info(String.format("jira milestone for %s: %s", issue.getKey(), milestoneJira.getKey()))
  }
    
  // fixing
  if (milestonePortfolio == null && milestoneJira != null) {
    // set custom portfolio fields
    // not possible
  } else if (milestonePortfolio != null && milestoneJira == null) {
    // create link from portfolio
    log.info(String.format("link jira milestone for %s: %s", issue.getKey(), milestonePortfolio.getKey()))
    def issueLinkType = issueLinkTypeManager.issueLinkTypes.findByName("Hierarchy")
    issueLinkManager.createIssueLink(milestonePortfolio.id, issue.id, issueLinkType.id, 1L, loggedInUser)
  }
}
circularityCache = []

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

Double calculateEstimate(Issue issue, List circularityCache, IssueLinkManager issueLinkManager, CustomField customField, Logger log, Integer level) {
  def pad = StringUtils.repeat(" ", level*2)
  log.info(String.format("%sbegin calculate %s for %s", pad, customField.getName(), issue.getKey()))
  
  Double result
  
  // avoiding circularity
  if (circularityCache.contains(issue) == false) {
    circularityCache.add(issue)

    // getting original estimate
    Double thisEstimate = 0
    def estimate = issue.getOriginalEstimate()
    if (estimate > 0) {
      thisEstimate = estimate / (8 * 3600) as Double;
    }
    log.info(String.format("%sthis %s for %s: %s", pad, customField.getName(), issue.getKey(), thisEstimate))

    // checking issue type
    String issueTypeName = issue.getIssueType().getName()
    switch (issueTypeName) {
      case "Milestone":
        // traversing direct children
        Double subsEstimate = 0
        issueLinkManager.getOutwardLinks(issue.id).each {
          issueLink ->
          Issue childIssue = issueLink.getDestinationObject()
          String childIssueTypeName = childIssue.getIssueType().getName()
          log.info(String.format("%sprocessing child of %s: %s", pad, issue.getKey(), childIssue.getKey()))
          if (issueLink.issueLinkType.name == "Hierarchy" && childIssue.getIssueType().getName() == "Epic") {
            // getting this estimate from child
            //Double childEstimate = getCustomFieldValue(childIssue, customField)
            Double childEstimate = calculateEstimate(childIssue, circularityCache, issueLinkManager, customField, log, level+1)
            log.info(String.format("%schild %s for %s: %s", pad, customField.getName(), childIssue.getKey(), childEstimate))
            // adding each child estimate
            subsEstimate += childEstimate
          }
        }
        result = subsEstimate
        break;
      case "Epic":
        // traversing direct children
        Double subsEstimate = 0
        issueLinkManager.getOutwardLinks(issue.id).each {
          issueLink ->
          Issue childIssue = issueLink.getDestinationObject()
          String childIssueTypeName = childIssue.getIssueType().getName()
          log.info(String.format("%sprocessing child of %s: %s", pad, issue.getKey(), childIssue.getKey()))
          if (issueLink.issueLinkType.name == "Epic-Story Link" && (
            childIssue.getIssueType().getName() == "Story"
              || childIssue.getIssueType().getName() == "Task"
              || childIssue.getIssueType().getName() == "Bug"
              || childIssue.getIssueType().getName() == "Change Request"
              || childIssue.getIssueType().getName() == "Spike")) {
                // getting this estimate from child
                //Double childEstimate = getCustomFieldValue(childIssue, customField)
                Double childEstimate = calculateEstimate(childIssue, circularityCache, issueLinkManager, customField, log, level+1)
                log.info(String.format("%schild %s for %s: %s", pad, customField.getName(), childIssue.getKey(), childEstimate))
                // adding each child estimate
                subsEstimate += childEstimate
          }
        }
        result = subsEstimate
        break;
      case "Story":
      case "Task":
      case "Bug":
      case "Change Request":
      case "Spike":
        result = thisEstimate
        break;
      default:
        result = 0
        break;
    }
  }
  log.info(String.format("%send calculate %s for %s: %s", pad, customField.getName(), issue.getKey(), result))
  return result
}

Double result = calculateEstimate(issue, circularityCache, issueLinkManager, customField, log, 0)
// memoizing data in number field (for Scrum)
log.info(String.format("update %s for %s: %s", customScrumField.getName(), issue.getKey(), result))
customScrumField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(customScrumField), result), new DefaultIssueChangeHolder());
// memoizing data in number field (for Portfolio)
log.info(String.format("update %s for %s: %s", customUiField.getName(), issue.getKey(), result))
customUiField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(customUiField), result.round(2)), new DefaultIssueChangeHolder());

return result