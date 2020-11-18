// Number|Number
enableCache = { ->false }

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.issue.fields.CustomField
import org.apache.commons.lang3.StringUtils
import org.apache.log4j.Logger

def log = Logger.getLogger('SCRIPTED')
def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def circularityCache = []

Double calculateEstimate(Issue issue, List circularityCache, IssueLinkManager issueLinkManager, Logger log, Integer level) {

  def pad = StringUtils.repeat(' ', level * 2)
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

    // checking issue type
    String issueTypeName = issue.getIssueType().getName()
    switch (issueTypeName) {
    case 'Milestone':
      if (thisEstimate > 0) {
        result = thisEstimate
      } else {
        // traversing direct children
        Double subsEstimate = 0
        issueLinkManager.getOutwardLinks(issue.id).each {
          issueLink ->Issue childIssue = issueLink.getDestinationObject()
          String childIssueTypeName = childIssue.getIssueType().getName()
          log.info(String.format('%sprocessing child of %s: %s', pad, issue.getKey(), childIssue.getKey()))
          if (issueLink.issueLinkType.name == 'Hierarchy' && childIssue.getIssueType().getName() == 'Epic') {
            // getting this estimate from child
            Double childEstimate = calculateEstimate(childIssue, circularityCache, issueLinkManager, log, level + 1)
            // adding each child estimate
            subsEstimate += childEstimate
          }
        }
        result = subsEstimate
      }
      break
    default:
      result = 0
      break
    }
  }
  return result
}

Double result = calculateEstimate(issue, circularityCache, issueLinkManager, log, 0)