package com.archsynthe;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;

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

}
