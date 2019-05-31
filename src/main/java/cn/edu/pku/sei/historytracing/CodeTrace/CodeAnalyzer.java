package cn.edu.pku.sei.historytracing.CodeTrace;

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

import java.util.ArrayList;
import java.util.List;

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

    public static void main(String args[]){
        String code = "  public final void setReader(Reader input) {\n" +
                "    if (input == null) {\n" +
                "      throw new NullPointerException(\"input must not be null\");\n" +
                "    } else if (this.input != ILLEGAL_STATE_READER) {\n" +
                "      throw new IllegalStateException(\"TokenStream contract violation: close() call missing\");\n" +
                "    }\n" +
                "    this.inputPending = input;\n" +
                "    setReaderTestPoint();\n" +
                "  }";
        String type="code";
        try{
            Repository repository =new FileRepository("D:\\项目源代码\\luceneGIT\\.git");
            CodeAnalyzer analyzer = new CodeAnalyzer(code,type,repository);
            System.out.println(analyzer.getFilePath());
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
