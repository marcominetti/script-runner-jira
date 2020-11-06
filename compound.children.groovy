enableCache = {-> false}
def customFieldName = "Compound Children"

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
def log = Logger.getLogger("SCRIPTED")

def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()

def circularityCache = []
List<String> children = new ArrayList<String>()

def listChildren(Issue issue, List circularityCache, IssueLinkManager issueLinkManager, Logger log, List<String> children) {   
    // avoiding circularity
    if (circularityCache.contains(issue) == false) {
        circularityCache.add(issue)
        
        // traversing direct children
        int childCount = 0
        issueLinkManager.getOutwardLinks(issue.id).each {
            issueLink ->
            if (issueLink.issueLinkType.name == "Hierarchy"
                || issueLink.issueLinkType.name == "Epic-Story Link"
                || issueLink.issueLinkType.isSubTaskLinkType() == true) {

                childCount++;
                Issue childIssue = issueLink.getDestinationObject()

                def customTreeField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Children");
                if(customTreeField != null) {
                    def customTree = childIssue.getCustomFieldValue(customTreeField);
                    if (customTree != null) {
                        def jsonParser = new JsonSlurper()
                        def childChildren = jsonParser.parseText(customTree.toString())
                        children.addAll((List<String>)childChildren)
                    }
                }
            }
        }
        
        if (childCount == 0) {
            children.add(issue.getKey())
        }
    }
}

listChildren(issue,circularityCache,issueLinkManager,log,children)

def result = new JsonBuilder(children);
return result.toString()