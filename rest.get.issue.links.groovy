import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.*
import groovy.transform.BaseScript
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.link.IssueLinkManager

import org.apache.log4j.Logger
import org.apache.log4j.Level
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

@BaseScript CustomEndpointDelegate delegate

getIssueLinks(httpMethod: "GET", groups: ["jira-users"]) { MultivaluedMap queryParams, String body ->
	List<HashMap<String,String>> issueLinks = new ArrayList<HashMap<String,String>>();

	IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
    IssueManager issueManager = ComponentAccessor.getIssueManager();

    Issue issue = issueManager.getIssueObject(queryParams.get("issueKey").get(0).toString());

    issueLinkManager.getOutwardLinks(issue.id).each {
        issueLink ->
        HashMap<String,String> issueLinkObject = new HashMap<String,String>();
        issueLinkObject["issuetype"] = issueLink.getDestinationObject().getIssueType().getName();
        issueLinkObject["linktype"] = issueLink.getIssueLinkType().getName();
        issueLinkObject["destination"] = issueLink.getDestinationObject().getKey();
        issueLinks.add(issueLinkObject);
    }

    return Response.ok(new JsonBuilder(issueLinks).toString()).build();
}
