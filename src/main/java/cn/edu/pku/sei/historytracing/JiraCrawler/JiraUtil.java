package cn.edu.pku.sei.historytracing.JiraCrawler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.joda.time.DateTime;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.BasicComponent;
import com.atlassian.jira.rest.client.domain.Comment;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;
public class JiraUtil {

    /**
     * 登录JIRA并返回指定的JiraRestClient对象
     *
     * @param username
     * @param password
     * @return
     * @throws URISyntaxException
     */
    public static JiraRestClient login_jira(String username, String password) throws URISyntaxException {
        try {
            final JerseyJiraRestClientFactory factory = new JerseyJiraRestClientFactory();
            final URI jiraServerUri = new URI("https://issues.apache.org/jira");
            final JiraRestClient restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, username,
                    password);
            return restClient;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取并返回指定的Issue对象
     *
     * @param issueNum
     * @param username
     * @param password
     * @return
     * @throws URISyntaxException
     */
    public static Issue get_issue(String issueNum, String username, String password) throws URISyntaxException {
        try {
            final JiraRestClient restClient = login_jira(username, password);
            final NullProgressMonitor pm = new NullProgressMonitor();
            final Issue issue = restClient.getIssueClient().getIssue(issueNum, pm);
            return issue;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取指定JIRA备注部分的内容
     *
     * @param issue
     * @param username
     * @param password
     * @return
     * @throws URISyntaxException
     */
    public static List<String> get_comments_body(Issue issue) throws URISyntaxException {
        try {
            List<String> comments = new ArrayList<String>();
            for (Comment comment : issue.getComments()) {
                comments.add(comment.getBody().toString());
            }
            return comments;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取指定JIRA的创建时间
     *
     * @param issueNum
     * @param username
     * @param password
     * @return
     * @throws URISyntaxException
     */
    public static DateTime get_create_time(Issue issue) throws URISyntaxException {
        try {
            return issue.getCreationDate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取指定JIRA的描述部分
     *
     * @param issueNum
     * @param username
     * @param password
     * @return
     * @throws URISyntaxException
     */
    public static String get_description(Issue issue) throws URISyntaxException {
        try {
            return issue.getDescription();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取指定JIRA的标题
     *
     * @param issueNum
     * @param username
     * @param password
     * @return
     * @throws URISyntaxException
     */
    public static String get_summary(Issue issue) throws URISyntaxException {
        try {
            return issue.getSummary();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取指定JIRA的报告人的名字
     *
     * @param issueNum
     * @param username
     * @param password
     * @return
     * @throws URISyntaxException
     */
    public static String get_reporter(Issue issue) throws URISyntaxException {
        try {
            return issue.getReporter().getDisplayName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取指定JIRA的状态
     *
     * @param issue
     * @param username
     * @param password
     * @return
     * @throws URISyntaxException
     */
    public static String get_status(Issue issue) throws URISyntaxException {
        try {
            return issue.getStatus().getName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取指定JIRA的类型
     *
     * @param issue
     * @param username
     * @param password
     * @return
     * @throws URISyntaxException
     */
    public static String get_issue_type(Issue issue) throws URISyntaxException {
        try {
            return issue.getIssueType().getName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取指定JIRA的模块
     *
     * @param issue
     * @param username
     * @param password
     * @return
     * @throws URISyntaxException
     */
    public static ArrayList<String> get_modules(Issue issue) throws URISyntaxException {
        try {
            ArrayList<String> arrayList = new ArrayList<String>();
            Iterator<BasicComponent> basicComponents = issue.getComponents().iterator();
            while (basicComponents.hasNext()) {
                String moduleName = basicComponents.next().getName();
                arrayList.add(moduleName);
            }
            return arrayList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 测试函数
     *
     * @param args
     * @throws URISyntaxException
     */
    public static void main(String[] args) throws URISyntaxException {
        String username = "pkupxl";
        String password = "05937768098";
        String issueNum = "LUCENE-8753";
        final Issue issue = get_issue(issueNum, username, password);

    }

}