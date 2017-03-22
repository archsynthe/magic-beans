package com.archsynthe;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author James Adams (jamesa@slalom.com)
 * @version ${VERSION}
 * @since ${VERSION}
 */
public class MagicBeansRepositoryManager {

    static void cloneRepository(MagicBeansConfig config) {

        SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host host, Session session ) {
                // do nothing
            }

            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch defaultJSch = super.createDefaultJSch( fs );
                defaultJSch.removeAllIdentity();
                defaultJSch.addIdentity( config.getDevopsPrivateKeyfilePath() );
                return defaultJSch;
            }
        };

        try {
            Git git = Git.cloneRepository()
                    .setURI( config.getDevopsRepositoryCloneUrl() )
                    .setDirectory( Paths.get(config.getHomePath(), "repos", config.getDevopsRepositoryName()).toFile() )
                    .setTransportConfigCallback(transport -> {
                        SshTransport sshTransport = (SshTransport)transport;
                        sshTransport.setSshSessionFactory( sshSessionFactory );
                    })
                    .call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

    }

    static void copySeedFiles(MagicBeansConfig config) {

        SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host host, Session session ) {
                // do nothing
            }

            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch defaultJSch = super.createDefaultJSch( fs );
                defaultJSch.removeAllIdentity();
                defaultJSch.addIdentity( config.getDevopsPrivateKeyfilePath() );
                return defaultJSch;
            }
        };

        try {
            Files.copy(Paths.get(config.getHomePath(), "templates", "devops-network-template.yaml"), Paths.get(config.getHomePath(), "repos", config.getDevopsRepositoryName(), "devops-network-template.yaml"));
            Git git = Git.open(Paths.get(config.getHomePath(),"repos",config.getDevopsRepositoryName()).toFile());
            git.add().addFilepattern("devops-network-template.yaml").call();
            git.commit().setMessage("Initial commit.").call();
            git.push().setTransportConfigCallback(transport -> {
                SshTransport sshTransport = (SshTransport)transport;
                sshTransport.setSshSessionFactory( sshSessionFactory );
            }).call();
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }

    }

}
