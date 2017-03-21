package com.archsynthe;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * These tests rely on the {@code MB_HOME} environment variable being set to the {@code src/main} directory under
 * the project home.
 *
 * @author James Adams (jamesa@slalom.com)
 * @version ${VERSION}
 * @since ${VERSION}
 */
public class MagicBeansTest {

    private String mbHome = System.getenv("MB_HOME");
    private MagicBeansConfig config;

    @Before
    public void verifyHomeDirectorySet() {

        // Verify MB_HOME environment variable is set
        assertNotNull(mbHome);
        assertNotEquals("",mbHome);

        // Load configuration file
        config = MagicBeansConfig.load();
        assertNotNull(config);
        assertNotNull(config.getCompany());
        assertNotNull(config.getDevopsBucketName());
        assertNotNull(config.getDevopsEnvironmentTemplatePath());
        assertNotNull(config.getDevopsGroupName());
        assertNotNull(config.getDevopsPolicyName());
        assertNotNull(config.getDevopsPrivateKeyfilePath());
        assertNotNull(config.getDevopsPublicKeyfilePath());
        assertNotNull(config.getDevopsRepositoryDescription());
        assertNotNull(config.getDevopsRepositoryName());
        assertNotNull(config.getDevopsStackName());
        assertNotNull(config.getDevopsUserName());
        assertNotNull(config.getHomePath());
        assertNotNull(config.getProfile());
        assertNotNull(config.getRegion());

    }

    @Test
    public void testKeyGen() {

        // Generate keypair
        String keyFingerprint = KeyManager.createSSHKey(config);

        // Validate keypair creation
        assertNotNull(keyFingerprint);
        assertEquals(47, keyFingerprint.length());
        assertTrue(Files.exists(Paths.get(config.getDevopsPrivateKeyfilePath())));
        assertTrue(Files.exists(Paths.get(config.getDevopsPublicKeyfilePath())));

    }

    @Test
    public void testKeyUpload() {

        // Assert keypair exists
        assertTrue(Files.exists(Paths.get(config.getDevopsPrivateKeyfilePath())));
        assertTrue(Files.exists(Paths.get(config.getDevopsPublicKeyfilePath())));

        // TODO: Create IAM Mock
        // TODO: Test actual method

    }

    @Test
    public void testRetrieveStackOutputs() {

        AmazonCloudFormation cloudFormation = ServiceManager.initCloudFormation(config);
        DevopsEnvironmentStackOutputs outputs = MagicBeans.retrieveStackOutputs(config, cloudFormation);
        assertNotNull(outputs);
        assertNotNull(outputs.getRepositoryCloneUrl());
        assertNotNull(outputs.getS3BucketUrl());
        assertNotNull(outputs.getUserAccessKeyId());
        assertNotNull(outputs.getUserSecretAccessKey());

    }

}
