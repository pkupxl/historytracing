package cn.edu.pku.sei.historytracing.CodeTrace;

import cn.edu.pku.sei.historytracing.entity.HistoryResult;
import cn.edu.pku.sei.historytracing.entity.IssueResult;
import javafx.util.Pair;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
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

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeAnalyzer {
    private String code;
    private String type;
    private Repository repository;

    public CodeAnalyzer(String code,String type,Repository repository){
        this.code=code;
        this.type=type;
        this.repository=repository;
    }

    public String getCode(){
        return code;
    }

    public String getType(){
        return type;
    }

    public Repository getRepository(){
        return repository;
    }
    public String getFilePath(){
        try{
            Ref head = repository.findRef("HEAD");
            RevWalk walk = new RevWalk(repository);
            RevCommit commit = walk.parseCommit(head.getObjectId());
            RevTree tree = walk.parseTree(commit.getTree().getId());
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);
                String fileContent = new String(loader.getBytes());
                if(fileContent.contains(code)){
                    return treeWalk.getPathString();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public String getFullNameFromCode(){
        ASTParser parser = ASTParser.newParser(AST.JLS10);
        parser.setSource(this.code.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        ASTVisitor codeVisitor=new CodeVisitor();
        CompilationUnit unit=(CompilationUnit)parser.createAST(null);
        unit.accept(codeVisitor);
        return ((CodeVisitor) codeVisitor).getClassName();
    }

    public String getMethodNameFromCode(){
        ASTParser parser = ASTParser.newParser(AST.JLS10);
        parser.setSource(this.code.toCharArray());
        parser.setKind(ASTParser.K_CLASS_BODY_DECLARATIONS);
        ASTVisitor methodVisitor=new MethodVisitor();
        TypeDeclaration unit=(TypeDeclaration)(parser.createAST(null));
        unit.accept(methodVisitor);
        return ((MethodVisitor) methodVisitor).getMethodName();
    }

    class CodeVisitor extends ASTVisitor{
        private List<String> className=null;

        public String getClassName(){
            if(this.className!=null)
                return className.get(0);
            else return null;
        }

        public boolean visit(TypeDeclaration node){
            if(this.className==null){
                this.className=new ArrayList<String>();
                this.className.add(NameResolver.getFullName(node));
            }else{
                this.className.add(NameResolver.getFullName(node));
            }
            return false;
        }
    }

    class MethodVisitor extends ASTVisitor{
        private List<String> MethodName=null;

        public String getMethodName(){
            if(this.MethodName!=null){
                return MethodName.get(0);
            }else return null;
        }

        public boolean visit(MethodDeclaration node){
            if(this.MethodName==null){
                this.MethodName=new ArrayList<String>();
                this.MethodName.add(node.getName().toString());
            }else{
                this.MethodName.add(node.getName().toString());
            }
            return false;
        }
    }



     static class Myvisitor extends ASTVisitor {
        private List<String> Methods=new ArrayList<>();
        private String fileContent=null;
        public Myvisitor(String fileContent){
            super();
            this.fileContent=fileContent;
        }

        public List<String> getMethods(){
            return this.Methods;
        }

        public boolean visit(MethodDeclaration node){
            this.Methods.add(fileContent.substring(node.getStartPosition(),node.getStartPosition()+node.getLength()));
            return true;
        }
    }

    public static class myChange{
        public int fileChange;
        public int methodChange;
        public myChange(int fileChange,int methodChange){
            this.fileChange=fileChange;
            this.methodChange=methodChange;
        }
    }





    public static void main(String args[]){
        try{
            Repository repository =new FileRepository("D:\\项目源代码\\luceneGIT\\.git");
            GitAnalyzer gitAnalyzer=new GitAnalyzer(repository);
            Ref head = repository.findRef("HEAD");
            RevWalk walk = new RevWalk(repository);
            RevCommit Lastcommit = walk.parseCommit(head.getObjectId());
            RevTree tree = walk.parseTree(Lastcommit.getTree().getId());
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            int MethodCnt=0;
            int fileCnt=0;

            List<myChange>Data=new ArrayList<>();

            FileOutputStream fos=new FileOutputStream(new File("D:\\test.txt"));
            OutputStreamWriter osw=new OutputStreamWriter(fos, "UTF-8");
            BufferedWriter  bw=new BufferedWriter(osw);
            while (treeWalk.next()) {
                String FilePath=treeWalk.getPathString();
                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);
                String fileContent = new String(loader.getBytes());
                if(FilePath.endsWith(".java")&&FilePath.startsWith("lucene")) {
                    fileCnt++;
           //         System.out.println(treeWalk.getPathString());
                    List<Pair<ObjectId, Pair<String, String>>>res= gitAnalyzer.getAllCommitModifyAFile(FilePath);
                    System.out.println(fileCnt);
                    int fileChange=res.size();

                    ASTParser parser = ASTParser.newParser(AST.JLS10);
                    parser.setSource(fileContent.toCharArray());
                    parser.setKind(ASTParser.K_COMPILATION_UNIT);
                    ASTVisitor codeVisitor=new Myvisitor(fileContent);
                    CompilationUnit unit=(CompilationUnit)parser.createAST(null);
                    unit.accept(codeVisitor);
                    List<String>Methods= ((Myvisitor)(codeVisitor)).getMethods();

                    for(int t=0;t<Methods.size();++t){
                        MethodCnt++;;
                        String Content=Methods.get(t);
                        int MethodChange=new HistoryTrace(new CodeAnalyzer(Content,"Method",repository),"Lucene").TraceResult().size();
                        String s=fileChange+":"+MethodChange;
                        bw.write(s+"\t\n");
                    }
                //    if(fileCnt>4)break;

                }

            }
            bw.close();
            osw.close();
            fos.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
