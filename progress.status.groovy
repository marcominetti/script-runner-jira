// None|HTML
enableCache = {-> false}

import com.atlassian.jira.ComponentManager
import com.atlassian.jira.component.ComponentAccessor
import groovy.xml.MarkupBuilder
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.config.properties.APKeys
import com.atlassian.jira.issue.fields.CustomField;

import org.apache.log4j.Logger
import org.apache.log4j.Level
def log = Logger.getLogger("SCRIPTED")

def customCommercialField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Commercial Estimate");
def customTimeSpentField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Time Spent");
def customRemainingEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Remaining Estimate");
def customOriginalEstimateField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Original Estimate");
def customProgressField =  ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Compound Progress");

Double getCustomFieldValue(Issue issue, CustomField customField) {
  def customValue
  if (customField != null) {
    customValue = issue.getCustomFieldValue(customField);
  }
  if (customValue != null) {
    return customValue as Double;
  }
  return 0
}

StringWriter writer = new StringWriter()
MarkupBuilder builder = new MarkupBuilder(writer)

def thisCompoundCommercialEstimate = getCustomFieldValue(issue, customCommercialField);
def thisCompoundTimeSpent = getCustomFieldValue(issue, customTimeSpentField);
def thisCompoundRemainingEstimate = getCustomFieldValue(issue, customRemainingEstimateField);
def thisCompoundOriginalEstimate = getCustomFieldValue(issue, customOriginalEstimateField);
def thisCompoundProgress = getCustomFieldValue(issue, customProgressField);

def isUnderEstimated = true
def total = thisCompoundTimeSpent + thisCompoundRemainingEstimate
def difference = total - thisCompoundOriginalEstimate
if (thisCompoundOriginalEstimate > total) {
    isUnderEstimated = false
    difference = thisCompoundOriginalEstimate - total
    total = thisCompoundOriginalEstimate
}

def percentOriginalEstimate = (total > 0) ? (thisCompoundOriginalEstimate / total * 100) as Integer : 0
def percentRemainingEstimate = (total > 0) ? (thisCompoundRemainingEstimate / total * 100) as Integer : 0
def percentTimeSpent = (total > 0) ? (thisCompoundTimeSpent / total * 100) as Integer : 0
def percentDifference = (total > 0) ? (difference / total * 100) as Integer : 0

// Commercial Estimate
builder.div (class:"", style: "display: inline-flex; font-weight: 400; max-height: 22px;") {
    if ((issue.getIssueType().getName() == "Milestone" || issue.getIssueType().getName() == "Epic") && thisCompoundOriginalEstimate > 0) {
        div (class: "", style: "text-align: center; width: 20px; color: #999; background-color: #fff; margin-right: 0px; font-size: 11px; text-overflow: ellipsis; overflow: hidden;") {
            span ("C ")
        }
        div (class: "", title: "Commercial Estimate", style: "text-align: center; width: 50px; padding: 0px 8px; color: #ffffff; background-color: #999; border-radius: 4px; margin-right: 10px;") {
            span (class: "", String.format((thisCompoundCommercialEstimate*10 < 1) ? "%.2fd" : "%.1fd", thisCompoundCommercialEstimate))
        }
    }else{
        div (class: "", style: "text-align: center; width: 20px; color: #999; background-color: #fff; margin-right: 0px; font-size: 11px; text-overflow: ellipsis; overflow: hidden;") {
            span ("C ")
        }
        div (class: "", title: "Commercial Estimate", style: "text-align: center; width: 50px; padding: 0px 8px; color: #ffffff; background-color: #999; border-radius: 4px; margin-right: 10px;") {
            span (class: "", "n.d.")
        }
    }
}

// Original Estimate
builder.div (class:"", style: "display: inline-flex; font-weight: 400; max-height: 22px;") {
    if (thisCompoundOriginalEstimate > 0) {
        div (class: "", style: "text-align: center; width: 20px; color: #89afd7; background-color: #fff; margin-right: 0px; font-size: 11px; text-overflow: ellipsis; overflow: hidden;") {
            span ("O ")
        }
        div (class: "", title: "Original Estimate", style: "text-align: center; width: 50px; padding: 0px 8px; color: #ffffff; background-color: #89afd7; border-radius: 4px; margin-right: 10px;") {
            span (class: "", String.format((thisCompoundOriginalEstimate*10 < 1) ? "%.2fd" : "%.1fd", thisCompoundOriginalEstimate))
        }
    }else{
        div (class: "", style: "text-align: center; width: 20px; color: #89afd7; background-color: #fff; margin-right: 0px; font-size: 11px; text-overflow: ellipsis; overflow: hidden;") {
            span ("O ")
        }
        div (class: "", title: "Original Estimate", style: "text-align: center; width: 50px; padding: 0px 8px; color: #ffffff; background-color: #89afd7; border-radius: 4px; margin-right: 10px;") {
            span (class: "", "n.d.")
        }
    }
}

// Remaining Estimate
builder.div (class:"", style: "display: inline-flex; font-weight: 400; max-height: 22px;") {
    if (thisCompoundRemainingEstimate > 0) {
        div (class: "", style: "text-align: center; width: 20px; color: #e2b36b; background-color: #fff; margin-right: 0px; font-size: 11px; text-overflow: ellipsis; overflow: hidden;") {
            span ("R ")
        }
        div (class: "", title: "Remaining Estimate", style: "text-align: center; width: 50px; padding: 0px 8px; color: #ffffff; background-color: #ec8e00; border-radius: 4px; margin-right: 10px;") {
            span (class: "", String.format((thisCompoundRemainingEstimate*10 < 1) ? "%.2fd" : "%.1fd", thisCompoundRemainingEstimate))
        }
    }else{
        div (class: "", style: "text-align: center; width: 20px; color: #e2b36b; background-color: #fff; margin-right: 0px; font-size: 11px; text-overflow: ellipsis; overflow: hidden;") {
            span ("R ")
        }
        div (class: "", title: "Remaining Estimate", style: "text-align: center; width: 50px; padding: 0px 8px; color: #ffffff; background-color: #ec8e00; border-radius: 4px; margin-right: 10px;") {
            span (class: "", "n.d.")
        }
    }
}

// Logged
builder.div (class:"", style: "display: inline-flex; font-weight: 400; max-height: 22px;") {
    if (thisCompoundTimeSpent > 0) {
        div (class: "", style: "text-align: center; width: 20px; color: #75af59; background-color: #fff; margin-right: 0px; font-size: 11px; text-overflow: ellipsis; overflow: hidden;") {
            span ("L ")
        }
        div (class: "", title: "Logged", style: "text-align: center; width: 50px; padding: 0px 8px; color: #ffffff; background-color: #51a825; border-radius: 4px; margin-right: 10px;") {
            span (class: "", String.format((thisCompoundTimeSpent*10 < 1) ? "%.2fd" : "%.1fd", thisCompoundTimeSpent))
        }
    }else{
        div (class: "", style: "text-align: center; width: 20px; color: #75af59; background-color: #fff; margin-right: 0px; font-size: 11px; text-overflow: ellipsis; overflow: hidden;") {
            span ("L ")
        }
        div (class: "", title: "Logged", style: "text-align: center; width: 50px; padding: 0px 8px; color: #ffffff; background-color: #51a825; border-radius: 4px; margin-right: 10px;") {
            span (class: "", "n.d.")
        }
    }
}

// Progress
builder.div (class:"", style: "display: inline-flex; font-weight: 400; max-height: 22px;") {
    if (thisCompoundOriginalEstimate > 0 || thisCompoundRemainingEstimate > 0 || thisCompoundTimeSpent > 0) {
        div (class: "", style: "text-align: center; width: 20px; color: #cccccc; background-color: #fff; margin-right: 0px; font-size: 11px; text-overflow: ellipsis; overflow: hidden;") {
            span ("P ")
        }
        div (class: "", title: "Progress", style: "text-align: center; width: 50px; padding: 0px 8px; color: #333333; background-color: #eeeeee; border-radius: 4px; margin-right: 10px;") {
            span (class: "", String.format("%.1f%%", (thisCompoundProgress * 100) as Double))
        }
    }else{
        div (class: "", style: "text-align: center; width: 20px; color: #cccccc; background-color: #fff; margin-right: 0px; font-size: 11px; text-overflow: ellipsis; overflow: hidden;") {
            span ("P ")
        }
        div (class: "", title: "Progress", style: "text-align: center; width: 50px; padding: 0px 8px; color: #333333; background-color: #eeeeee; border-radius: 4px; margin-right: 10px;") {
            span (class: "", String.format("%.1f%%", 0 as Double))
        }
    }
}

// Progress Bar
builder.div (class:"", style: "display: inline-flex; font-weight: 400; max-height: 22px;") {
    if (thisCompoundOriginalEstimate > 0 || thisCompoundRemainingEstimate > 0 || thisCompoundTimeSpent > 0) {
        div (class: "", style: "position:relative; top:8px;") {
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