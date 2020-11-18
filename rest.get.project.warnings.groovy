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

getProjectWarnings(httpMethod: "GET", groups: ["jira-users", "jira-software-users"]) { MultivaluedMap queryParams, String body ->
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
        results.getIssues().each { 
            issue ->
            List<IssueLink> links = issueLinkManager.getInwardLinks(issue.id);
            if (links.size() == 0) {
                issues.add(issue);
            } else {
                links.each { 
                    issueLink ->
                    if (issueLink.issueLinkType.name != "Hierarchy"
                            && issueLink.issueLinkType.name != "Epic-Story Link") {
                        issues.add(issue);
                    }
                }
            }
        }

        
        def customWarningField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Warnings");

        List<Map<String,String>> warnings = new ArrayList<Map<String,String>>()
        issues.each { 
            issue ->
            if(customWarningField != null) {
                def customWarnings = issue.getCustomFieldValue(customWarningField);
                if (customWarnings != null) {
                    def jsonParser = new JsonSlurper()
                    def childWarnings = jsonParser.parseText(customWarnings.toString())
                    warnings.addAll((List<Map<String, String>>) childWarnings)
                }
            }
        }

        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("warnings", warnings);

        return Response.ok(new JsonBuilder(result).toString()).build();
    } else {
        return Response.ok("{}").build();
    }
}
