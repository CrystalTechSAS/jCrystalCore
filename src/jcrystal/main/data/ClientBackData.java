package jcrystal.main.data;

import java.util.Random;

import com.jcrystal.back.entidades.ProjectKey;
import com.jcrystal.back.entidades.usage.ProjectRun;

public class ClientBackData {

	public final ProjectKey projectKey;
	
	public final ProjectRun run;
	
	public final Random random;
	
	public ClientBackData(ProjectKey projectKey) {
		this.projectKey = projectKey;
		this.run = new ProjectRun(projectKey.project$Key()).projectKey(projectKey);
		this.random = new Random(projectKey.project$Key());
	}
	
}
