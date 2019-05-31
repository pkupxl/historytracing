package cn.edu.pku.sei.historytracing.CodeTrace;

import cn.edu.pku.sei.historytracing.entity.HistoryResult;
import cn.edu.pku.sei.historytracing.entity.IssueResult;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import javafx.util.Pair;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.compiler.SpoonResource;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.compiler.VirtualFile;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HistoryTrace {
    private CodeAnalyzer analyzer;
    private String project;
    public HistoryTrace(CodeAnalyzer analyzer,String project){
        this.analyzer = analyzer;
        this.project = project;
    }

    public List<HistoryResult>TraceResult(){
        if(analyzer.getType().equals("Class")){
            return searchClassHistory();
        }else if(analyzer.getType().equals("Method")){
            return searchMethodHistory();
        }else if(analyzer.getType().equals("OneLine")){
            return searchOneLineHistory();
        }else if(analyzer.getType().equals("MultiLines")){
            return searchMultilinesHistory();
        }else{
            return null;
        }
    }


    public List<HistoryResult> searchClassHistory(){
        List<HistoryResult>result=new ArrayList<>();
        String FilePath=analyzer.getFilePath();
        String FileContent=null;
        String PreFileContent=null;
        try{
            Repository repository = analyzer.getRepository();
            GitAnalyzer gitAnalyzer=new GitAnalyzer(repository);
            List<Pair<ObjectId, Pair<String, String>>>res= gitAnalyzer.getAllCommitModifyAFile(FilePath);
            try (RevWalk revWalk = new RevWalk(repository)) {
                boolean First=true;
                for(Pair<ObjectId, Pair<String, String>> p:res){
                    RevCommit commit = revWalk.parseCommit(p.getKey());
                    RevTree tree = commit.getTree();
                    try (TreeWalk treeWalk = new TreeWalk(repository)) {
                        treeWalk.addTree(tree);
                        treeWalk.setRecursive(true);
                        FilePath=p.getValue().getValue();
                        treeWalk.setFilter(PathFilter.create(FilePath));
                        while(treeWalk.next()){
                            ObjectId objectId = treeWalk.getObjectId(0);
                            ObjectLoader loader = repository.open(objectId);
                            if(First){
                                FileContent = new String(loader.getBytes());
                                First=false;
                                break;
                            }
                            PreFileContent=new String(loader.getBytes());
                            if(!PreFileContent.equals(FileContent)){
                                Diff diff =new AstComparator().compare(PreFileContent,FileContent);
                                List<Operation> allOperations=diff.getRootOperations();
                                List<String> changeSummary=new ArrayList<>();
                                for(Operation op : allOperations){
                                    changeSummary.add(op.toString());
                                }

                                String time = TimeStamp2Date(String.valueOf(commit.getCommitTime()));
                                String Message=commit.getFullMessage().split("git-svn-id")[0];
                                Pattern pattern = Pattern.compile(project.toUpperCase()+"-(\\d+):(.*)");
                                Matcher matcher = pattern.matcher(Message);
                                boolean hasIssue=false;
                                IssueResult issueResult = null;
                                if(matcher.find() && matcher.start()==0){
                                    hasIssue=true;
                                    String issueId = project.toUpperCase()+"-"+matcher.group(1);
                                    issueResult = IssueResult.getIssueResult(issueId);
                                }
                                result.add(new HistoryResult(PreFileContent,FileContent,Message,time,changeSummary,hasIssue,issueResult));
                            }

                            FileContent=PreFileContent;
                        }
                    }
                    revWalk.dispose();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public List<HistoryResult> searchMethodHistory(){
        List<HistoryResult>result=new ArrayList<>();
        String FilePath=analyzer.getFilePath();
        String Content=analyzer.getCode();
        String MethodContent=null;
        String PreMethodContent=null;
        String MethodSignature=null;
        String MethodName=null;

        try{
            Repository repository = analyzer.getRepository();
            GitAnalyzer gitAnalyzer=new GitAnalyzer(repository);
            List<Pair<ObjectId, Pair<String, String>>>res= gitAnalyzer.getAllCommitModifyAFile(FilePath);
            try (RevWalk revWalk = new RevWalk(repository)) {
                boolean First=true;
                boolean Finish=false;
                for(Pair<ObjectId, Pair<String, String>> p:res){
                    RevCommit commit = revWalk.parseCommit(p.getKey());
                    RevTree tree = commit.getTree();
                    try (TreeWalk treeWalk = new TreeWalk(repository)) {
                        treeWalk.addTree(tree);
                        treeWalk.setRecursive(true);
                        FilePath=p.getValue().getValue();
                        treeWalk.setFilter(PathFilter.create(FilePath));
                        if(Finish)break;
                        while(treeWalk.next()){
                            ObjectId objectId = treeWalk.getObjectId(0);
                            ObjectLoader loader = repository.open(objectId);
                            if(First){
                                String fileContent=new String(loader.getBytes());
                                int start=0;
                                int end=0;
                                String filelines[]=fileContent.split("\\n");
                                String methodlines[]=Content.split(("\\n"));
                                for(int i=0;i<filelines.length;++i){
                                    boolean find=true;
                                    for(int j=0;j<methodlines.length;++j){
                                        if(!filelines[i+j].trim().equals(methodlines[j].trim())){
                                            find=false;
                                            break;
                                        }
                                    }
                                    if(find){
                                        start=i+1;
                                        end=start+methodlines.length-1;
                                    }
                                }
                                SpoonAPI spoon = new Launcher();
                                VirtualFile resource = new VirtualFile(new String(loader.getBytes()), "/test");
                                ((Launcher) spoon).addInputResource((SpoonResource) resource);
                                spoon.buildModel();

                                final int Start=start;
                                final int End=end;
                                for (CtMethod<?> meth : spoon.getModel().getRootPackage().getElements(new TypeFilter<CtMethod>(CtMethod.class) {
                                    @Override
                                    public boolean matches(CtMethod element) {
                                        return super.matches(element)&&element.getPosition().getLine()>=Start &&element.getPosition().getEndLine()<=End;
                                    }
                                })) {
                                    System.out.println("找到第一个版本的方法");
                                    MethodContent=meth.toString();
                                    MethodSignature=meth.getSignature();
                                    MethodName=meth.getSimpleName();
                                }
                                MethodContent=Content;
                                First=false;
                                break;
                            }
                            PreMethodContent=getPreMethodContent(new String(loader.getBytes()),MethodName,MethodSignature);

                            String Message=commit.getFullMessage().split("git-svn-id")[0];
                            Pattern pattern = Pattern.compile(project.toUpperCase()+"-(\\d+):(.*)");
                            Matcher matcher = pattern.matcher(Message);
                            boolean hasIssue=false;
                            IssueResult issueResult = null;
                            if(matcher.find() && matcher.start()==0){
                                hasIssue=true;
                                String issueId = project.toUpperCase()+"-"+matcher.group(1);
                                issueResult = IssueResult.getIssueResult(issueId);
                            }

                            if(PreMethodContent==null){
                                String time = TimeStamp2Date(String.valueOf(commit.getCommitTime()));
                                result.add(new HistoryResult(PreMethodContent,MethodContent,Message,time,null,hasIssue,issueResult));
                                Finish=true;
                                break;
                            }else if(!MethodContent.trim().equals(PreMethodContent.trim())) {
                                String time = TimeStamp2Date(String.valueOf(commit.getCommitTime()));
                                result.add(new HistoryResult(PreMethodContent, MethodContent, Message,time,null,hasIssue,issueResult));
                            }

                            MethodContent=PreMethodContent;
                        }
                    }
                    revWalk.dispose();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public String getPreMethodContent(String content,String methodName,String methodSignature){
        SpoonAPI spoon = new Launcher();
        VirtualFile resource = new VirtualFile(content, "/test");
        ((Launcher) spoon).addInputResource((SpoonResource) resource);
        spoon.buildModel();
        List<CtMethod>potentialMethod=new ArrayList<>();
        for (CtMethod<?> meth : spoon.getModel().getRootPackage().getElements(new TypeFilter<CtMethod>(CtMethod.class) {
            @Override
            public boolean matches(CtMethod element) {
                return super.matches(element)&&element.getSimpleName().equals(methodName);
            }
        })) {
            potentialMethod.add(meth);
        }
        if(potentialMethod.size()==0){
            return null;
        }else if(potentialMethod.size()==1){
            int start=potentialMethod.get(0).getPosition().getLine();
            int end=potentialMethod.get(0).getPosition().getEndLine();
            String result="";
            String lines[]=content.split("\\n");
            for(int i=start;i<=end;++i){
                result+=lines[i-1]+"\n";
            }
            return result;
        }else{
            double Maxsim=0;
            int index=0;
            for(int i=0;i<potentialMethod.size();++i){
                double similarity=getSimilariry(methodSignature,potentialMethod.get(i).getSignature());
                if(similarity<Maxsim){
                    Maxsim=similarity;
                    index=i;
                }
            }


            int start=potentialMethod.get(index).getPosition().getLine();
            int end=potentialMethod.get(index).getPosition().getEndLine();
            String result="";
            String lines[]=content.split("\\n");
            for(int i=start;i<=end;++i){
                result+=lines[i-1]+"\n";
            }
            return result;
        }
    }

    public List<HistoryResult> searchOneLineHistory(){
        List<HistoryResult>result=new ArrayList<>();
        String FilePath=analyzer.getFilePath();
        String Content=analyzer.getCode();
        String MethodContent=null;
        String MethodSignature=null;
        String MethodName=null;
        try{
            Repository repository = analyzer.getRepository();
            GitAnalyzer gitAnalyzer=new GitAnalyzer(repository);
            List<Pair<ObjectId, Pair<String, String>>>res= gitAnalyzer.getAllCommitModifyAFile(FilePath);
            try (RevWalk revWalk = new RevWalk(repository)) {
                boolean First=true;
                boolean Finish=false;
                for(Pair<ObjectId, Pair<String, String>> p:res){
                    if(Finish)break;
                    RevCommit commit = revWalk.parseCommit(p.getKey());
                    RevTree tree = commit.getTree();
                    try (TreeWalk treeWalk = new TreeWalk(repository)) {
                        treeWalk.addTree(tree);
                        treeWalk.setRecursive(true);
                        FilePath=p.getValue().getValue();
                        treeWalk.setFilter(PathFilter.create(FilePath));
                        while(treeWalk.next()){
                            ObjectId objectId = treeWalk.getObjectId(0);
                            ObjectLoader loader = repository.open(objectId);
                            if(First){
                                String fileContent=new String(loader.getBytes());
                                int linenum=0;
                                String filelines[]=fileContent.split("\\n");
                                for(int i=0;i<filelines.length;++i){
                                    if(filelines[i].trim().equals(Content.trim())){
                                        linenum=i+1;
                                    }
                                }

                                SpoonAPI spoon = new Launcher();
                                VirtualFile resource = new VirtualFile(new String(loader.getBytes()), "/test");
                                ((Launcher) spoon).addInputResource((SpoonResource) resource);
                                spoon.buildModel();

                                final int Linenum=linenum;
                                for (CtMethod<?> meth : spoon.getModel().getRootPackage().getElements(new TypeFilter<CtMethod>(CtMethod.class) {
                                    @Override
                                    public boolean matches(CtMethod element) {
                                        return super.matches(element)&&element.getPosition().getLine()<=Linenum &&element.getPosition().getEndLine()>=Linenum;
                                    }
                                })) {
                                    System.out.println("找到第一个版本的方法");
                                    MethodContent=meth.getBody().toString();
                                    MethodSignature=meth.getSignature();
                                    MethodName=meth.getSimpleName();
                                }
                                First=false;
                                break;
                            }

                            String PreMethodContent=getPreMethodContent(new String(loader.getBytes()),MethodName,MethodSignature);
                            if(PreMethodContent==null){
                                String Message=commit.getFullMessage().split("git-svn-id")[0];
                                Pattern pattern = Pattern.compile(project.toUpperCase()+"-(\\d+):(.*)");
                                Matcher matcher = pattern.matcher(Message);
                                boolean hasIssue=false;
                                IssueResult issueResult = null;
                                if(matcher.find() && matcher.start()==0){
                                    hasIssue=true;
                                    String issueId = project.toUpperCase()+"-"+matcher.group(1);
                                    issueResult = IssueResult.getIssueResult(issueId);
                                }
                                String time = TimeStamp2Date(String.valueOf(commit.getCommitTime()));
                                result.add(new HistoryResult(null,Content,Message,time,null,hasIssue,issueResult));
                                Finish=true;
                                break;
                            }else{

                                String Message=commit.getFullMessage().split("git-svn-id")[0];
                                Pattern pattern = Pattern.compile(project.toUpperCase()+"-(\\d+):(.*)");
                                Matcher matcher = pattern.matcher(Message);
                                boolean hasIssue=false;
                                IssueResult issueResult = null;
                                if(matcher.find() && matcher.start()==0){
                                    hasIssue=true;
                                    String issueId = project.toUpperCase()+"-"+matcher.group(1);
                                    issueResult = IssueResult.getIssueResult(issueId);
                                }

                                String PreContent=getPreContentForOneline(PreMethodContent,Content);
                                if(PreContent==null){
                                    String time = TimeStamp2Date(String.valueOf(commit.getCommitTime()));
                                    result.add(new HistoryResult(PreContent,Content,Message,time,null,hasIssue,issueResult));
                                    Finish=true;
                                    break;
                                } else if (!PreContent.trim().equals(Content.trim())) {
                                    String time = TimeStamp2Date(String.valueOf(commit.getCommitTime()));
                                    result.add(new HistoryResult(PreContent,Content,Message,time,null,hasIssue,issueResult));
                                }
                                Content=PreContent;
                            }
                        }
                    }
                    revWalk.dispose();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public List<HistoryResult> searchMultilinesHistory(){
        List<HistoryResult>result=new ArrayList<>();
        String FilePath=analyzer.getFilePath();
        String Content=analyzer.getCode();
        String MethodContent=null;
        String MethodSignature=null;
        String MethodName=null;
        try{
            Repository repository = analyzer.getRepository();
            GitAnalyzer gitAnalyzer=new GitAnalyzer(repository);
            List<Pair<ObjectId, Pair<String, String>>>res= gitAnalyzer.getAllCommitModifyAFile(FilePath);
            try (RevWalk revWalk = new RevWalk(repository)) {
                boolean First=true;
                boolean Finish=false;
                for(Pair<ObjectId, Pair<String, String>> p:res){
                    if(Finish)break;
                    RevCommit commit = revWalk.parseCommit(p.getKey());
                    RevTree tree = commit.getTree();
                    try (TreeWalk treeWalk = new TreeWalk(repository)) {
                        treeWalk.addTree(tree);
                        treeWalk.setRecursive(true);
                        FilePath=p.getValue().getValue();
                        treeWalk.setFilter(PathFilter.create(FilePath));
                        while(treeWalk.next()){
                            ObjectId objectId = treeWalk.getObjectId(0);
                            ObjectLoader loader = repository.open(objectId);
                            if(First){
                                String fileContent=new String(loader.getBytes());
                                int start=0;
                                int end=0;
                                String filelines[]=fileContent.split("\\n");
                                String methodlines[]=Content.split(("\\n"));
                                for(int i=0;i<filelines.length;++i){
                                    boolean find=true;
                                    for(int j=0;j<methodlines.length;++j){
                                        if(!filelines[i+j].trim().equals(methodlines[j].trim())){
                                            find=false;
                                            break;
                                        }
                                    }
                                    if(find){
                                        start=i+1;
                                        end=start+methodlines.length-1;
                                    }
                                }
                                SpoonAPI spoon = new Launcher();
                                VirtualFile resource = new VirtualFile(new String(loader.getBytes()), "/test");
                                ((Launcher) spoon).addInputResource((SpoonResource) resource);
                                spoon.buildModel();

                                final int Start=start;
                                final int End=end;
                                for (CtMethod<?> meth : spoon.getModel().getRootPackage().getElements(new TypeFilter<CtMethod>(CtMethod.class) {
                                    @Override
                                    public boolean matches(CtMethod element) {
                                        return super.matches(element)&&element.getPosition().getLine()<=Start &&element.getPosition().getEndLine()>=End;
                                    }
                                })) {
                                    System.out.println("找到第一个版本的方法");
                                    MethodContent=meth.getBody().toString();
                                    MethodSignature=meth.getSignature();
                                    MethodName=meth.getSimpleName();
                                }
                                First=false;
                                break;
                            }

                            String PreMethodContent=getPreMethodContent(new String(loader.getBytes()),MethodName,MethodSignature);
                            if(PreMethodContent==null){
                                String Message=commit.getFullMessage().split("git-svn-id")[0];
                                Pattern pattern = Pattern.compile(project.toUpperCase()+"-(\\d+):(.*)");
                                Matcher matcher = pattern.matcher(Message);
                                boolean hasIssue=false;
                                IssueResult issueResult = null;
                                if(matcher.find() && matcher.start()==0){
                                    hasIssue=true;
                                    String issueId = project.toUpperCase()+"-"+matcher.group(1);
                                    issueResult = IssueResult.getIssueResult(issueId);
                                }

                                String time = TimeStamp2Date(String.valueOf(commit.getCommitTime()));
                                result.add(new HistoryResult(null,Content,Message,time,null,hasIssue,issueResult));
                                Finish=true;
                                break;
                            }else{

                                String Message=commit.getFullMessage().split("git-svn-id")[0];
                                Pattern pattern = Pattern.compile(project.toUpperCase()+"-(\\d+):(.*)");
                                Matcher matcher = pattern.matcher(Message);
                                boolean hasIssue=false;
                                IssueResult issueResult = null;
                                if(matcher.find() && matcher.start()==0){
                                    hasIssue=true;
                                    String issueId = project.toUpperCase()+"-"+matcher.group(1);
                                    issueResult = IssueResult.getIssueResult(issueId);
                                }

                                String PreContent=getPreContentForMultilines(PreMethodContent,Content);
                                if(PreContent==null){
                                    String time = TimeStamp2Date(String.valueOf(commit.getCommitTime()));
                                    result.add(new HistoryResult(PreContent,Content,Message,time,null,hasIssue,issueResult));
                                    Finish=true;
                                    break;
                                } else if (!PreContent.trim().equals(Content.trim())) {
                                    String time = TimeStamp2Date(String.valueOf(commit.getCommitTime()));
                                    result.add(new HistoryResult(PreContent,Content,Message,time,null,hasIssue,issueResult));
                                }
                                Content=PreContent;
                            }
                        }
                    }
                    revWalk.dispose();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public String TimeStamp2Date(String timestampString){
        Long timestamp = Long.parseLong(timestampString)*1000;
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
        return date;
    }

    public String getPreContentForOneline(String PreMethodContent,String Content){
        String lines[]=PreMethodContent.split("\\n");
        double MaxSim=0;
        int index=1;
        for(int i=1;i<lines.length-1;++i){
            double similarity=getSimilariry(lines[i],Content);
            if(similarity>MaxSim){
                MaxSim=similarity;
                index=i;
            }
        }
        if(MaxSim<0.1)return null;

        return lines[index];
    }

    public String getPreContentForMultilines(String PreMethodContent,String Content){
        String MethodLine[]=PreMethodContent.split("\\n");
        String ContentLine[]=Content.split("\\n");
        int MLen=MethodLine.length;
        int CLen=ContentLine.length;
        int Max=0;
        int Min=MLen-1;
        for(int i=0;i<CLen;++i){
            int index=1;
            double MaxSim=0;
            for(int j=1;j<MLen-1;++j){
                double similarity=getSimilariry(MethodLine[j],ContentLine[i]);
                if(similarity>MaxSim){
                    MaxSim=similarity;
                    index=j;
                }
            }
            if(Max<index)Max=index;
            if(Min>index)Min=index;
        }
        String result="";
        for(int i=Min;i<=Max;++i){
            result+=MethodLine[i]+"\n";
        }
        return result;
    }

    public double getSimilariry(String source, String target) {
        source=source.trim();
        target=target.trim();
        char[] sources = source.toCharArray();
        char[] targets = target.toCharArray();
        int sourceLen = sources.length;
        int targetLen = targets.length;
        if(sourceLen==0||targetLen==0){
            return 0;
        }
        int[][] d = new int[sourceLen + 1][targetLen + 1];
        for (int i = 0; i <= sourceLen; i++) {
            d[i][0] = i;
        }
        for (int i = 0; i <= targetLen; i++) {
            d[0][i] = i;
        }

        for (int i = 1; i <= sourceLen; i++) {
            for (int j = 1; j <= targetLen; j++) {
                if (sources[i - 1] == targets[j - 1]) {
                    d[i][j] = d[i - 1][j - 1];
                } else {
                    //插入
                    int insert = d[i][j - 1] + 1;
                    //删除
                    int delete = d[i - 1][j] + 1;
                    //替换
                    int replace = d[i - 1][j - 1] + 1;
                    d[i][j] = Math.min(insert, delete) > Math.min(delete, replace) ? Math.min(delete, replace) :
                            Math.min(insert, delete);
                }
            }
        }
        double Max=0;
        if(sourceLen>targetLen){
            Max=sourceLen;
        }else{
            Max=targetLen;
        }
        double result=1-(double)d[sourceLen][targetLen]/Max;
        return result;
    }
}
