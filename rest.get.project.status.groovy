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
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.util.UserUtil;

import org.apache.log4j.Logger
import org.apache.log4j.Level
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

@BaseScript CustomEndpointDelegate delegate

getProjectStatus(httpMethod: "GET", groups: ["jira-users", "jira-software-users"]) { MultivaluedMap queryParams, String body ->
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

    UserManager userManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager);
    ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
    boolean isAllowed = userManager.isSystemAdmin(user.getUsername()) || userManager.isAdmin(user.getUsername());

    if (project != null) {
        ProjectRoleManager projectRoleManager = ComponentAccessor.getOSGiComponentInstanceOfType(ProjectRoleManager);
        Collection projectRoles = projectRoleManager.getProjectRoles(user, project);
        for (ProjectRole role : projectRoles) {
            if ("Project Leaders".equals(role.getName()) || "Administrators".equals(role.getName())) {
                isAllowed = true;
            }
        }
    }

    if (project != null && isAllowed == true) {
        JqlQueryParser jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class)
        SearchProvider searchProvider = ComponentAccessor.getComponent(SearchProvider.class)

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
        def projectedEstimate = 0
        def timeSpent = 0
        def progress = 0

        def customField
        def customValue
        def customOriginalEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Original Estimate");
        def customRemainingEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Remaining Estimate");
        def customProjectedEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Projected Estimate");
        def customTimeSpentField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Time Spent");
        def customProgressField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Progress");

        List<Map<String,String>> anomalies = new ArrayList<Map<String,String>>()

        issues.each { issue ->
            def issueOriginalEstimate = 0
            def issueRemainingEstimate = 0
            def issueWorkableEstimate = 0
            def issueBlockedEstimate = 0
            def issueOverrunEstimate = 0
            def issueProjectedEstimate = 0
            def issueTimeSpent = 0
            def issueProgress = 0
            if(customOriginalEstimateField != null) {
                customValue = issue.getCustomFieldValue(customOriginalEstimateField);
                issueOriginalEstimate = (customValue != null) ? customValue as Double : 0;
            }
            if(customRemainingEstimateField != null) {
                customValue = issue.getCustomFieldValue(customRemainingEstimateField);
                issueRemainingEstimate = (customValue != null) ? customValue as Double : 0;
            }
            if(customProjectedEstimateField != null) {
                customValue = issue.getCustomFieldValue(customProjectedEstimateField);
                issueProjectedEstimate = (customValue != null) ? customValue as Double : 0;
            }
            if(customTimeSpentField != null) {
                customValue = issue.getCustomFieldValue(customTimeSpentField);
                issueTimeSpent = (customValue != null) ? customValue as Double : 0;
            }        
            if(customProgressField != null) {
                customValue = issue.getCustomFieldValue(customProgressField);
                issueProgress += (customValue != null) ? customValue as Double : 0;
            }

            originalEstimate += issueOriginalEstimate;
            remainingEstimate += issueRemainingEstimate;
            projectedEstimate += issueProjectedEstimate
            timeSpent += issueTimeSpent;
            progress += issueProgress * issueProjectedEstimate

        }

        if (projectedEstimate > 0) {
            progress /= projectedEstimate;
        } else {
            progress = 0;
        }

        HashMap<String, Object> result = new HashMap<>();
        result.put("originalEstimate", originalEstimate as Double);
        result.put("remainingEstimate", remainingEstimate as Double);
        result.put("projectedEstimate", projectedEstimate as Double);
        result.put("timeSpent", timeSpent as Double);
        result.put("progress", progress as Double);
        return Response.ok(new JsonBuilder(result).toString()).build();
    } else {
        return Response.ok("{}").build();
    }
}