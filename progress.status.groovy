// None|HTML

import com.atlassian.jira.ComponentManager
import com.atlassian.jira.component.ComponentAccessor
import groovy.xml.MarkupBuilder
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.ManagerFactory
import com.atlassian.jira.config.properties.APKeys

import org.apache.log4j.Logger
import org.apache.log4j.Level

def log = Logger.getLogger("COMPUTED")
log.setLevel(Level.DEBUG)

StringWriter writer = new StringWriter()
MarkupBuilder builder = new MarkupBuilder(writer)

def thisCompoundTimeSpent = 0;
def thisCompoundRemainingEstimate = 0;
def thisCompoundOriginalEstimate = 0;
def thisCompoundProgress = 0;

def customTimeSpentField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Time Spent");
def customTimeSpent
if(customTimeSpentField != null) {
    customTimeSpent = issue.getCustomFieldValue(customTimeSpentField);
}
if (customTimeSpent != null) {
    thisCompoundTimeSpent = (double) customTimeSpent
}

def customRemainingEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Remaining Estimate");
def customRemainingEstimate
if(customRemainingEstimateField != null) {
    customRemainingEstimate = issue.getCustomFieldValue(customRemainingEstimateField);
} 
if (customRemainingEstimate != null) {
    thisCompoundRemainingEstimate = (double) customRemainingEstimate
}

def customOriginalEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Original Estimate");
def customOriginalEstimate
if(customOriginalEstimateField != null) {
    customOriginalEstimate = issue.getCustomFieldValue(customOriginalEstimateField);
} 
if (customOriginalEstimate != null) {
    thisCompoundOriginalEstimate = (double) customOriginalEstimate
}

def customProgressField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Progress (Time Spent)");
def customProgress
if(customProgressField != null) {
    customProgress = issue.getCustomFieldValue(customProgressField);
} 
if (customProgress != null) {
    thisCompoundProgress = (double) customProgress
}

def isUnderEstimated = true
def total = thisCompoundTimeSpent + thisCompoundRemainingEstimate
def difference = total - thisCompoundOriginalEstimate
if (thisCompoundOriginalEstimate > total) {
    isUnderEstimated = false
    difference = thisCompoundOriginalEstimate - total
    total = thisCompoundOriginalEstimate
}

def percentOriginalEstimate = (total > 0) ? (int)(thisCompoundOriginalEstimate / total * 100) : 0
def percentRemainingEstimate = (total > 0) ? (int)(thisCompoundRemainingEstimate / total * 100) : 0
def percentTimeSpent = (total > 0) ? (int)(thisCompoundTimeSpent / total * 100) : 0
def percentDifference = (total > 0) ? (int)(difference / total * 100) : 0

builder.div (class:"", style: "display: inline-flex; font-weight: 400; max-height: 22px;") {
    if (thisCompoundOriginalEstimate > 0) {
        div (class: "", style: "padding: 3px 7px; color: #89afd7; background-color: #fff; margin-right: 0px; font-size: 11px; text-overflow: ellipsis; overflow: hidden;") {
            span ("E ")
        }
        div (class: "", title: "Original Estimate", style: "padding: 0px 8px; color: #ffffff; background-color: #89afd7; border-radius: 4px; margin-right: 10px;") {
            span (class: "", String.format((thisCompoundOriginalEstimate*10 < 1) ? "%.2fd" : "%.1fd", thisCompoundOriginalEstimate))
        }
    }
    if (thisCompoundRemainingEstimate > 0) {
        div (class: "", style: "padding: 3px 7px; color: #e2b36b; background-color: #fff; margin-right: 0px; font-size: 11px; text-overflow: ellipsis; overflow: hidden;") {
            span ("R ")
        }
        div (class: "", title: "Remaining Estimate", style: "padding: 0px 8px; color: #ffffff; background-color: #ec8e00; border-radius: 4px; margin-right: 10px;") {
            span (class: "", String.format((thisCompoundRemainingEstimate*10 < 1) ? "%.2fd" : "%.1fd", thisCompoundRemainingEstimate))
        }
    }
    if (thisCompoundTimeSpent > 0) {
        div (class: "", style: "padding: 3px 7px; color: #75af59; background-color: #fff; margin-right: 0px; font-size: 11px; text-overflow: ellipsis; overflow: hidden;") {
            span ("L ")
        }
        div (class: "", title: "Logged", style: "padding: 0px 8px; color: #ffffff; background-color: #51a825; border-radius: 4px; margin-right: 10px;") {
            span (class: "", String.format((thisCompoundTimeSpent*10 < 1) ? "%.2fd" : "%.1fd", thisCompoundTimeSpent))
        }
    }
    if (thisCompoundOriginalEstimate > 0 || thisCompoundRemainingEstimate > 0 || thisCompoundTimeSpent > 0) {
        div (class: "", style: "padding: 3px 7px; color: #cccccc; font-size: 11px; text-overflow: ellipsis; overflow: hidden;") {
            span ("P ")
        }
        div (class: "", title: "Progress", style: "padding: 0px 8px; color: #333333; background-color: #eeeeee; border-radius: 4px; margin-right: 10px;") {
            span (class: "", String.format("%.1f%%", (double)(thisCompoundProgress * 100)))
        }
        div (class: "", style: "") {
            table (style: "width: 150px; margin: 1px;", cellpadding:"0", cellspacing:"0") {
                tbody () {
                    tr () {
                        td (style:"width:100%") { 
                            table (style:"width:100%;", cellpadding:"0", cellspacing:"0") {
                                tbody() {
                                    tr() {
                                        td() {
                                            table (class:"tt_graph", cellpadding:"0", cellspacing:"0", style:"margin-bottom: 2px;") {
                                                tbody() {
                                                    tr (style:"height: 8px;") {
                                                        td (style:"width:" + percentOriginalEstimate + "%; background-color:#89afd7;") {
                                                            img (src:"/jira/images/border/spacer.gif", class:"hideOnPrint", title:"Original Estimate - " + thisCompoundOriginalEstimate, alt:"Original Estimate - " + thisCompoundOriginalEstimate)
                                                        }
                                                        if (isUnderEstimated == true) {
                                                            td (style:"width:" + percentDifference + "%; background-color:transparent;") {
                                                                img (src:"/jira/images/border/spacer.gif", class:"hideOnPrint")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    tr () {
                                        td () {
                                            table (class:"tt_graph", cellpadding:"0", cellspacing:"0") {
                                                tbody() {
                                                    tr (style:"height: 8px;") {
                                                        td (style:"width:" + percentTimeSpent + "%; background-color:#51a825; border:0; font-size:0;") {
                                                            img (src:"/jira/images/border/spacer.gif", class:"hideOnPrint", title:"Time Spent - " + thisCompoundTimeSpent, alt:"Time Spent - " + thisCompoundTimeSpent)
                                                        }
                                                        td (class:"tt_spacer") {
                                                            img (src:"/jira/images/border/spacer.gif", class:"hideOnPrint")
                                                        }
                                                        td (style:"width:" + percentRemainingEstimate + "%; background-color:#ec8e00; border:0; font-size:0;") {
                                                            img (src:"/jira/images/border/spacer.gif", class:"hideOnPrint", title:"Remaining Estimate - " + thisCompoundRemainingEstimate, alt:"Remaining Estimate - " + thisCompoundRemainingEstimate)
                                                        }
                                                        if (isUnderEstimated == false) {
                                                            td (style:"width:" + percentDifference + "%; background-color:transparent;") {
                                                                img (src:"/jira/images/border/spacer.gif", class:"hideOnPrint")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

return writer.toString()