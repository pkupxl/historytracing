package cn.edu.pku.sei.historytracing.entity;

import cn.edu.pku.sei.historytracing.JiraCrawler.JiraUtil;
import com.atlassian.jira.rest.client.domain.Comment;
import com.atlassian.jira.rest.client.domain.Issue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IssueResult {
    private String issueId;
    private String summary;
    private String description;
    private String createTime;
    private String issueType;
    private String status;
    private String reporter;
    private List<String>comments;

    public IssueResult(String issueId, String summary, String description, String createTime, String issueType, String status, String reporter, List<String> comments) {
        this.issueId = issueId;
        this.summary = summary;
        this.description = description;
        this.createTime = createTime;
        this.issueType = issueType;
        this.status = status;
        this.reporter = reporter;
        this.comments = comments;
    }

    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReporter() {
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    public static IssueResult getIssueResult(String issueId){
        IssueResult issueResult=null;
        String username = "pkupxl";
        String password = "05937768098";
        try{
            Issue issue= JiraUtil.get_issue(issueId,username,password);
            List<String>comments=new ArrayList<>();
            Iterator<Comment>it=issue.getComments().iterator();
            while(it.hasNext()){
                Comment cm=it.next();
                comments.add(cm.getBody());
            }
            issueResult=new IssueResult(issueId,issue.getSummary(),issue.getDescription(),issue.getCreationDate().toString(),
                    issue.getIssueType().getName(),issue.getStatus().getName(),issue.getReporter().getDisplayName(),comments);
        }catch (Exception e){
            e.printStackTrace();
        }
        return issueResult;
    }
}
