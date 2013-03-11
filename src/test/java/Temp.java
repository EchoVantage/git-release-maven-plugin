import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;


public class Temp {
	public static void main(String[] args) throws Exception {

        Git git = Git.open(new File("c:/temp/example-remote"));
        Iterable<PushResult> results = git.push().setRemote("git@github.com:mikedeck/example.git").setRefSpecs(new RefSpec("master:test")).call();
        
        for(PushResult result : results) {
        	System.out.println(result.getMessages());
        }
	}
}
