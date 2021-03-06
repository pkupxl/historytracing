package cn.edu.pku.sei.historytracing.entity;

import java.util.List;

public class HistoryResult {
    private String preContent;
    private String content;
    private String commitMessage;
    private String time;
    private List<String> changeSummary;
    private boolean hasIssue;
    private String issueId;
    private IssueResult issueResult;

    public String getPreContent(){
        return preContent;
    }
    public String getContent(){
        return content;
    }
    public String getCommitMessage(){
        return commitMessage;
    }
    public String getTime(){return time;}
    public List<String> getChangeSummary(){return changeSummary;}
    public boolean getHasIssue(){
        return hasIssue;
    }
    public String getIssueId(){
        return issueId;
    }
    public IssueResult getIssueResult(){ return issueResult; }

    public HistoryResult(String preContent, String Content, String CommitMessage, String time, List<String> changeSummary, boolean hasIssue,IssueResult issueResult){
        this.preContent=preContent;
        this.content=Content;
        this.commitMessage=CommitMessage;
        this.time=time;
        this.changeSummary=changeSummary;
        this.hasIssue=hasIssue;
        this.issueId=commitMessage.split(":")[0].toUpperCase();
        this.issueResult=issueResult;
    }
}
