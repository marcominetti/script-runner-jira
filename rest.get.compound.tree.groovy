import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.*
import groovy.transform.BaseScript
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.link.IssueLink
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.project.Project
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.query.Query
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;

import org.apache.log4j.Logger
import org.apache.log4j.Level
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

def buildTree (Issue issue, List circularityCache, IssueLinkManager issueLinkManager, Logger log, Map<String,Object> tree) {
    // avoiding circularity
    if (circularityCache.contains(issue) == false) {
        circularityCache.add(issue)
        
        tree.put("key", issue.getKey())
        
        def originalEstimate = issue.getOriginalEstimate()
        if (originalEstimate > 0) {
            originalEstimate = originalEstimate / (8 * 3600) as Double;
        }
        tree.put("originalEstimate", originalEstimate)

        def remainingEstimate = issue.getEstimate()
        if (remainingEstimate > 0) {
            remainingEstimate = remainingEstimate / (8 * 3600) as Double;
        }
        tree.put("remainingEstimate", remainingEstimate)

        def timespent = issue.getTimeSpent()
        if (timespent > 0) {
            timespent = timespent / (8 * 3600) as Double;
        }
        tree.put("timeSpent", timespent)

        tree.put("status", issue.getStatus().getName())
        tree.put("type", issue.getIssueType().getName())
        tree.put("resolution", issue.getResolution())

        def customValue
        def customOriginalEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Original Estimate");
        def customRemainingEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Remaining Estimate");
        def customWorkableEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Workable Estimate");
        def customBlockedEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Blocked Estimate");
        def customOverrunEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Overrun Estimate");
        def customTimeSpentField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Time Spent");
        def customProgressRemainingField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Progress (Remaining)");
        def customProgressTimeSpentField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Progress (Time Spent)");

        if(customOriginalEstimateField != null) {
            customValue = issue.getCustomFieldValue(customOriginalEstimateField);
            tree.put("compoundOriginalEstimate", (customValue != null) ? customValue as Double : 0);
        }
        if(customRemainingEstimateField != null) {
            customValue = issue.getCustomFieldValue(customRemainingEstimateField);
            tree.put("compoundRemainingEstimate", (customValue != null) ? customValue as Double : 0);
        }
        if(customWorkableEstimateField != null) {
            customValue = issue.getCustomFieldValue(customWorkableEstimateField);
            tree.put("compoundWorkableEstimate", (customValue != null) ? customValue as Double : 0);
        }
        if(customBlockedEstimateField != null) {
            customValue = issue.getCustomFieldValue(customBlockedEstimateField);
            tree.put("compoundBlockedEstimate", (customValue != null) ? customValue as Double : 0);
        }
        if(customOverrunEstimateField != null) {
            customValue = issue.getCustomFieldValue(customOverrunEstimateField);
            tree.put("compoundOverrunEstimate", (customValue != null) ? customValue as Double : 0);
        }
        if(customTimeSpentField != null) {
            customValue = issue.getCustomFieldValue(customTimeSpentField);
            tree.put("compoundTimeSpentEstimate", (customValue != null) ? customValue as Double : 0);
        }        
        if(customProgressRemainingField != null) {
            customValue = issue.getCustomFieldValue(customProgressRemainingField);
            tree.put("compoundProgressRemaining", (customValue != null) ? customValue as Double : 0);
        }
        if(customProgressTimeSpentField != null) {
            customValue = issue.getCustomFieldValue(customProgressTimeSpentField);
            tree.put("compoundProgressTimeSpent", (customValue != null) ? customValue as Double : 0);
        }
        
        // traversing direct children
        int childCount = 0
        List<Map<String,Object>> children = new ArrayList<Map<String,Object>>()
        issueLinkManager.getOutwardLinks(issue.id).each {
            issueLink ->
            if (issueLink.issueLinkType.name == "Hierarchy"
                || issueLink.issueLinkType.name == "Epic-Story Link"
                || issueLink.issueLinkType.isSubTaskLinkType() == true) {

                childCount++;
                Issue childIssue = issueLink.getDestinationObject()

                Map<String,Object> childTree = new HashMap<String,Object>()
                buildTree(childIssue,circularityCache,issueLinkManager,log,childTree)
                children.add((Map<String, Object>)childTree)
            }
        }
        tree.put("childCount", childCount)
        tree.put("children", children)
    }
}

@BaseScript CustomEndpointDelegate delegate

getCompoundTree(httpMethod: "GET", groups: ["jira-administrators"]) { MultivaluedMap queryParams, String body ->
    IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
    IssueManager issueManager = ComponentAccessor.getIssueManager();
    ProjectManager projectManager = ComponentAccessor.getProjectManager();
	CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
	Project project;

    List<String> params = queryParams.get("projectKey");
    if (params != null) {
    	String projectKey = params.get(0).toString();    
    	project = projectManager.getProjectObjByKey(projectKey);
	}
    
    if (project != null) {
        JqlQueryParser jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class)
        SearchProvider searchProvider = ComponentAccessor.getComponent(SearchProvider.class)
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

        Query query = jqlQueryParser.parseQuery("project = " + project.getKey() + " AND \"Epic Link\" = EMPTY AND issuetype in standardIssueTypes()")
        SearchResults results = searchProvider.search(query, user, PagerFilter.getUnlimitedFilter())
        List<Issue> issues = new ArrayList<Issue>();
        results.getIssues().each { issue ->
            List<IssueLink> links = issueLinkManager.getInwardLinks(issue.id);
            if (links.size() == 0) {
                issues.add(issue);
            } else {
                links.each { issueLink ->
                    if (issueLink.issueLinkType.name != "Hierarchy"
                            && issueLink.issueLinkType.name != "Epic-Story Link") {
                        issues.add(issue);
                    }
                }
            }
        }

        def circularityCache = []
        Map<String,Object> root = new HashMap<String,Object>()
        List<Map<String,Object>> rootChildren = new ArrayList<Map<String,Object>>()
        root.put("key", project.getKey())
        root.put("childCount",issues.size())

        issues.each { issue ->
            Map<String,Object> childTree = new HashMap<String,Object>()
            buildTree(issue,circularityCache,issueLinkManager,log,childTree)
            rootChildren.add(childTree)
        }
        root.put("children",rootChildren)

        return Response.ok(new JsonBuilder(root).toString()).build();
    } else {
        return Response.ok("{}").build();
    }
}
