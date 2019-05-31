package cn.edu.pku.sei.historytracing;

import cn.edu.pku.sei.historytracing.CodeTrace.CodeAnalyzer;
import cn.edu.pku.sei.historytracing.CodeTrace.HistoryTrace;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import cn.edu.pku.sei.historytracing.entity.*;

import static cn.edu.pku.sei.historytracing.entity.Project.getProjectList;

@CrossOrigin
@RestController
public class Controller {
    Map<String, Repository> repositoryMap = new LinkedHashMap<>();

    @Autowired
    private Context context;

    @RequestMapping(value = "/projects", method = {RequestMethod.GET, RequestMethod.POST})
    synchronized public List<Project> getProjects() throws IOException, JSONException {
        List<Project> projects;
        projects = getProjectList(context.infoDir);
        return projects;
    }

    @RequestMapping(value = "/historySearch", method = {RequestMethod.GET, RequestMethod.POST})
    synchronized public List<HistoryResult> getHistoryResult(String query,String project,String type){
        Repository repository = getGitRepository(project);
        HistoryTrace trace = new HistoryTrace(new CodeAnalyzer(query,type,repository),project);
        System.out.println(query);
        System.out.println(type);
        return trace.TraceResult();
    }


    private Repository getGitRepository(String project) {
        if(!repositoryMap.containsKey(project)){
            try{
                File jsonFile = ResourceUtils.getFile(context.infoDir);
                String json = FileUtils.readFileToString(jsonFile, "utf-8");
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jobj = jsonArray.getJSONObject(i);
                    String name = jobj.getString("name");
                    if(name.equals(project)){
                        String gitRepositoryPath = jobj.getString("GitRepositoryPath");
                        repositoryMap.put(project, new FileRepository(gitRepositoryPath));
                        break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return repositoryMap.get(project);
    }
}

@Component
class Context {
    String infoDir = null;

    @Autowired
    public Context(Conf conf) {
        this.infoDir = conf.getInfoDir();
    }
}