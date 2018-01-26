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

@BaseScript CustomEndpointDelegate delegate

getProjectStatus(httpMethod: "GET", groups: ["jira-administrators"]) { MultivaluedMap queryParams, String body ->
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

        def originalEstimate = 0
        def remainingEstimate = 0
        def workableEstimate = 0
        def backlogEstimate = 0
        def overrunEstimate = 0
        def timeSpent = 0
        def progressRemaining = 0
        def progressTimeSpent = 0

        def customField
        def customValue
        def customOriginalEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Original Estimate");
        def customRemainingEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Remaining Estimate");
        def customWorkableEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Workable Estimate");
        def customBacklogEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Backlog Estimate");
        def customOverrunEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Overrun Estimate");
        def customTimeSpentField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Time Spent");
        def customProgressRemainingField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Progress (Remaining)");
        def customProgressTimeSpentField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Progress (Time Spent)");

        issues.each { issue ->
            def issueOriginalEstimate = 0
            def issueRemainingEstimate = 0
            def issueWorkableEstimate = 0
            def issueBacklogEstimate = 0
            def issueOverrunEstimate = 0
            def issueTimeSpent = 0
            def issueProgressRemaining = 0
            def issueProgressTimeSpent = 0
            if(customOriginalEstimateField != null) {
                customValue = issue.getCustomFieldValue(customOriginalEstimateField);
                issueOriginalEstimate = (customValue != null) ? (Double) customValue : 0;
            }
            if(customRemainingEstimateField != null) {
                customValue = issue.getCustomFieldValue(customRemainingEstimateField);
                issueRemainingEstimate = (customValue != null) ? (Double) customValue : 0;
            }
            if(customWorkableEstimateField != null) {
                customValue = issue.getCustomFieldValue(customWorkableEstimateField);
                issueWorkableEstimate = (customValue != null) ? (Double) customValue : 0;
            }
            if(customBacklogEstimateField != null) {
                customValue = issue.getCustomFieldValue(customBacklogEstimateField);
                issueBacklogEstimate = (customValue != null) ? (Double) customValue : 0;
            }
            if(customOverrunEstimateField != null) {
                customValue = issue.getCustomFieldValue(customOverrunEstimateField);
                issueOverrunEstimate = (customValue != null) ? (Double) customValue : 0;
            }
            if(customTimeSpentField != null) {
                customValue = issue.getCustomFieldValue(customTimeSpentField);
                issueTimeSpent = (customValue != null) ? (Double) customValue : 0;
            }        
            if(customProgressTimeSpentField != null) {
                customValue = issue.getCustomFieldValue(customProgressTimeSpentField);
                issueProgressTimeSpent += (customValue != null) ? (Double) customValue : 0;
            }
            originalEstimate += issueOriginalEstimate;
            remainingEstimate += issueRemainingEstimate;
            workableEstimate += issueWorkableEstimate;
            backlogEstimate += issueBacklogEstimate;
            overrunEstimate += issueOverrunEstimate;
            timeSpent += issueTimeSpent;
            progressTimeSpent += issueProgressTimeSpent * (issueTimeSpent + issueRemainingEstimate)
        }

        if (originalEstimate > 0) {
            progressRemaining = 1 - (remainingEstimate / originalEstimate)
        } else {
            progressRemaining = 0;
        }
        if ((timeSpent + remainingEstimate) > 0) {
            progressTimeSpent /= (timeSpent + remainingEstimate);
        } else {
            progressTimeSpent = 0;
        }

        HashMap<String, Double> result = new HashMap<>();
        result.put("originalEstimate", (Double)originalEstimate);
        result.put("remainingEstimate", (Double)remainingEstimate);
        result.put("workableEstimate", (Double)workableEstimate);
        result.put("backlogEstimate", (Double)backlogEstimate);
        result.put("overrunEstimate", (Double)overrunEstimate);
        result.put("timeSpent", (Double)timeSpent);
        result.put("progressRemaining", (Double)progressRemaining);
        result.put("progressTimeSpent", (Double)progressTimeSpent);
        return Response.ok(new JsonBuilder(result).toString()).build();
    } else {
        return Response.ok("{}").build();
    }
}
