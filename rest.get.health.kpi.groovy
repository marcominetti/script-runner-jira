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

getHealthKpi(httpMethod: "GET", groups: ["jira-users", "jira-software-users"]) { MultivaluedMap queryParams, String body ->
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

        def customValue
        def budgetAtCompletion = 0
        def earnedQuantitativeProjectedValue = 0
        def earnedQuantitativeCommercialValue = 0
        def earnedQualitativeProjectedValue = 0
        def earnedQualitativeCommercialValue = 0
        def actualCost = 0

        
        def customBudgetAtCompletionField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Budget At Completion");
        def customEarnedQuantitativeProjectedValueField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Earned Quantitative Projected Value");
        def customEarnedQuantitativeCommercialValueField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Earned Quantitative Commercial Value");
        def customEarnedQualitativeProjectedValueField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Earned Qualitative Projected Value");
        def customEarnedQualitativeCommercialValueField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Earned Qualitative Commercial Value");
        def customActualCostField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Actual Cost");

        issues.each {  issue ->
            def issueBudgetAtCompletion = 0
            def issueEarnedQuantitativeProjectedValue = 0
            def issueEarnedQuantitativeCommercialValue = 0
            def issueEarnedQualitativeProjectedValue = 0
            def issueEarnedQualitativeCommercialValue = 0
            def issueActualCost = 0
            
            if(customBudgetAtCompletionField != null) {
                customValue = issue.getCustomFieldValue(customBudgetAtCompletionField);
                issueBudgetAtCompletion = (customValue != null) ? customValue as Double : 0;
            }
            if(customEarnedQuantitativeProjectedValueField != null) {
                customValue = issue.getCustomFieldValue(customEarnedQuantitativeProjectedValueField);
                issueEarnedQuantitativeProjectedValue = (customValue != null) ? customValue as Double : 0;
            }
            if(customEarnedQuantitativeCommercialValueField != null) {
                customValue = issue.getCustomFieldValue(customEarnedQuantitativeCommercialValueField);
                issueEarnedQuantitativeCommercialValue = (customValue != null) ? customValue as Double : 0;
            }
            if(customEarnedQualitativeProjectedValueField != null) {
                customValue = issue.getCustomFieldValue(customEarnedQualitativeProjectedValueField);
                issueEarnedQualitativeProjectedValue = (customValue != null) ? customValue as Double : 0;
            }        
            if(customEarnedQualitativeCommercialValueField != null) {
                customValue = issue.getCustomFieldValue(customEarnedQualitativeCommercialValueField);
                issueEarnedQualitativeCommercialValue += (customValue != null) ? customValue as Double : 0;
            }
            if(customActualCostField != null) {
                customValue = issue.getCustomFieldValue(customActualCostField);
                issueActualCost += (customValue != null) ? customValue as Double : 0;
            }

            budgetAtCompletion += issueBudgetAtCompletion;
            earnedQuantitativeProjectedValue += issueEarnedQuantitativeProjectedValue;
            earnedQuantitativeCommercialValue += issueEarnedQuantitativeCommercialValue
            earnedQualitativeProjectedValue += issueEarnedQualitativeProjectedValue
            earnedQualitativeCommercialValue += issueEarnedQualitativeCommercialValue
            actualCost += issueActualCost
        }

        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("budgetAtCompletion", budgetAtCompletion as Double);
        result.put("earnedQuantitativeProjectedValue", earnedQuantitativeProjectedValue as Double);
        result.put("earnedQuantitativeCommercialValue", earnedQuantitativeCommercialValue as Double);
        result.put("earnedQualitativeProjectedValue", earnedQualitativeProjectedValue as Double);
        result.put("earnedQualitativeCommercialValue", earnedQualitativeCommercialValue as Double);
        result.put("actualCost", actualCost as Double);
        return Response.ok(new JsonBuilder(result).toString()).build();
    } else {
        return Response.ok("{}").build();
    }
}
