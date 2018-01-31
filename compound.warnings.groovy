enableCache = {-> false}

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import com.atlassian.jira.ComponentManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.resolution.Resolution
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;

import org.apache.log4j.Logger
import org.apache.log4j.Level

def log = Logger.getLogger("COMPUTED")
log.setLevel(Level.DEBUG)

def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()

def circularityCache = []
List<Map<String,String>> anomalies = new ArrayList<Map<String,String>>()

def createAnomaly(List<Map<String,String>> anomalies, Issue issue, String level, String description) {
    Map<String,String> anomaly = new HashMap<String, String>()
    anomaly.put("description", description)
    anomaly.put("issue", issue.getKey())
    anomaly.put("level", level)
    anomaly.put("resolution", (issue.getResolution() != null)?"true":"false")
    anomaly.put("status", issue.getStatus().getName())
    anomaly.put("type", issue.getIssueType().getName())
    anomalies.add(anomaly)
}

def calculateAnomalies(Issue issue, List circularityCache, IssueLinkManager issueLinkManager, Logger log, List<Map<String,String>> anomalies) {
    // avoiding circularity
    if (circularityCache.contains(issue) == false) {
        circularityCache.add(issue)
    
        String thisKey = issue.getKey()
        Long thisOriginalEstimate = issue.getOriginalEstimate()
        Long thisTimeSpent = issue.getTimeSpent()
        Long thisRemainingEstimate = issue.getEstimate()    
        Long compoundOriginalEstimate
        Long compoundTimeSpent
        Long compoundRemainingEstimate
        String thisStatusName = issue.getStatus().getName()
        Resolution resolution = issue.getResolution()
        int childCount = 0

        // traversing direct children
        issueLinkManager.getOutwardLinks(issue.id).each {
            issueLink ->
            if (issueLink.issueLinkType.name == "Hierarchy"
                || issueLink.issueLinkType.name == "Epic-Story Link"
                || issueLink.issueLinkType.isSubTaskLinkType() == true) {

                childCount++;
                Issue childIssue = issueLink.getDestinationObject()
                
                Long childOriginalEstimate = issue.getOriginalEstimate()
                Long childTimeSpent = issue.getTimeSpent()
                Long childRemainingEstimate = issue.getEstimate() 

                if (childOriginalEstimate != null) {
                    if (compoundOriginalEstimate == null) {
                        compoundOriginalEstimate = 0
                    }
                    compoundOriginalEstimate += childOriginalEstimate
                }

                if (childTimeSpent != null) {
                    if (compoundTimeSpent == null) {
                        compoundTimeSpent = 0
                    }
                    compoundTimeSpent += childTimeSpent
                }

                if (childRemainingEstimate != null) {
                    if (compoundRemainingEstimate == null) {
                        compoundRemainingEstimate = 0
                    }
                    compoundRemainingEstimate += childRemainingEstimate
                }

                def customWarningField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Warnings");
                if(customWarningField != null) {
                    def customAnomalies = childIssue.getCustomFieldValue(customWarningField);
                    if (customAnomalies != null) {
                        def jsonParser = new JsonSlurper()
                        def childAnomalies = jsonParser.parseText(customAnomalies.toString())
                        anomalies.addAll((List<Map<String,String>>) childAnomalies)
                    }
                }

            }
        }

        if (childCount == 0) {
			if (thisOriginalEstimate == null || thisOriginalEstimate == 0) {
                createAnomaly(anomalies, issue, "WARN", "issue senza original estimate e senza figli")
            }
            if (thisRemainingEstimate != null && thisRemainingEstimate > 0 && resolution != null) {
                createAnomaly(anomalies, issue, "WARN", "issue risolta ma con remaining estimate: azzerare il remaining sulla issue " + issue.getKey())
            }
            if ((thisRemainingEstimate == null || thisRemainingEstimate == 0) && resolution == null) {
                createAnomaly(anomalies, issue, "WARN", "issue non risolta ma senza remaining estimate: chiudere la issue o adeguare il remaining sulla issue " + issue.getKey())
            }
        } else {
            if (thisOriginalEstimate != null && thisOriginalEstimate > 0) {
                createAnomaly(anomalies, issue, "WARN", "issue padre ma con original estimate: rimuovere l'original estimate dalla issue " + issue.getKey() + " assicurandosi di averla distribuita tra i figli")
            }
            if (thisTimeSpent != null && thisTimeSpent > 0) {
                createAnomaly(anomalies, issue, "WARN", "issue padre ma con worklog: rimuovere il worklog dalla issue " + issue.getKey() + " ed inserilo in uno dei figli")
            }
            if (thisRemainingEstimate != null && thisRemainingEstimate > 0) {
                createAnomaly(anomalies, issue, "WARN", "issue padre ma con remaining estimate: rimuovere la remaining estimate dalla issue " + issue.getKey() + " assicurandosi di averla distribuita tra i figli")
            }
        }
    }
}

calculateAnomalies(issue,circularityCache,issueLinkManager,log,anomalies)

def result = new JsonBuilder(anomalies);
return result.toString()