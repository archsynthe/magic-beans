package com.archsynthe;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.*;
import com.amazonaws.services.codecommit.AWSCodeCommit;
import com.amazonaws.services.codecommit.AWSCodeCommitClient;
import com.amazonaws.services.codecommit.AWSCodeCommitClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author James Adams (jamesa@slalom.com)
 * @version ${VERSION}
 * @since ${VERSION}
 */
public class MagicBeans {

    private static final Logger LOGGER = Logger.getLogger(MagicBeans.class.getName());

    private static final String AWS_REGION_CONFIG_PROPERTY = "awsRegion";
    private static final String AWS_PROFILE_CONFIG_PROPERTY = "awsProfile";
    private static final String COMPANY_CONFIG_PROPERTY = "company";

    public static void main(String[] args) {

        LOGGER.info("########## START RUN ##########");

        // Load configuration from file
        MagicBeansConfig config = loadConfig();

        // Initialize services
        AWSCodeCommit codeCommit = initCodeCommit(config);
        AmazonS3 amazonS3 = initS3(config);
        AmazonCloudFormation cloudFormation = initCloudFormation(config);

        // Create DevOps Environment (root stack)
        CreateStackResult stackResult = createDevOpsEnvironment(config, cloudFormation);

        // Only proceed if we have a result
        if (stackResult != null) {

            createSSHKey(config);

        }

        LOGGER.info("########## STOP RUN ##########");

    }

    static MagicBeansConfig loadConfig() {

        // Load path from environment
        String homePath = System.getenv("MB_HOME");

        MagicBeansConfig config = new MagicBeansConfig();
        config.setHomePath(homePath);
        try (InputStream is = new FileInputStream(homePath + "/conf/config.yaml")) {

            // Load configuration data from classpath resource
            Yaml yaml = new Yaml();
            Map map = (Map) yaml.load(is);

            // Map data loaded from config file to configuration object
            config.setRegion((String) map.get(AWS_REGION_CONFIG_PROPERTY));
            config.setProfile((String) map.get(AWS_PROFILE_CONFIG_PROPERTY));
            config.setCompany((String) map.get(COMPANY_CONFIG_PROPERTY));

            // Map generated names back to config object
            config.setDevopsEnvironmentTemplatePath(config.getCompany() + "/templates/devops-environment-template.yaml");
            config.setDevopsStackName(config.getRegion() + "-" + config.getCompany() + "-devops-environment");
            config.setDevopsRepositoryName(config.getRegion() + "-" + config.getCompany() + "-cloudformation");
            config.setDevopsRepositoryDescription("CloudFormation Templates: " + config.getCompany());
            config.setDevopsBucketName(config.getRegion() + "-" + config.getCompany() + "-devops-artifacts");
            config.setDevopsPolicyName(config.getRegion() + "-" + config.getCompany() + "-devops-policy");
            config.setDevopsGroupName(config.getRegion() + "-" + config.getCompany() + "-devops-group");
            config.setDevopsUserName(config.getRegion() + "-" + config.getCompany() + "-devops-user");
            config.setDevopsPrivateKeyfilePath(config.getHomePath() + "/keys/" + config.getDevopsUserName());
            config.setDevopsPublicKeyfilePath(config.getDevopsPrivateKeyfilePath() + ".pub");

        } catch (IOException e) {
            e.printStackTrace();
        }

        return config;
    }

    private static CreateStackResult createDevOpsEnvironment(MagicBeansConfig config, AmazonCloudFormation cloudFormation) {

        String templatePath = config.getDevopsEnvironmentTemplatePath();

        String templateBody = "";
        try {
            templateBody = new String(Files.readAllBytes(Paths.get(templatePath)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Stack Request
        CreateStackRequest stackRequest = new CreateStackRequest();
        stackRequest.setTemplateBody(templateBody);
        stackRequest.setStackName(config.getDevopsStackName());

        // Capabilities
        List<String> capabilities = new ArrayList<>(1);
        capabilities.add(Capability.CAPABILITY_NAMED_IAM.toString());
        stackRequest.setCapabilities(capabilities);

        // Parameters
        List<Parameter> parameters = new ArrayList<>(3);

        // Parameter: Repository Name
        Parameter repositoryNameParam = new Parameter();
        repositoryNameParam.setParameterKey("repositoryName");
        repositoryNameParam.setParameterValue(config.getDevopsRepositoryName());
        parameters.add(repositoryNameParam);

        // Parameter: Repository Description
        Parameter repositoryDescriptionParam = new Parameter();
        repositoryDescriptionParam.setParameterKey("repositoryDescription");
        repositoryDescriptionParam.setParameterValue(config.getDevopsRepositoryDescription());
        parameters.add(repositoryDescriptionParam);

        // Parameter: DevOps Bucket Name
        Parameter devopsBucketNameParam = new Parameter();
        devopsBucketNameParam.setParameterKey("devopsBucketName");
        devopsBucketNameParam.setParameterValue(config.getDevopsBucketName());
        parameters.add(devopsBucketNameParam);

        // Parameter: DevOps Group Name
        Parameter devopsGroupNameParam = new Parameter();
        devopsGroupNameParam.setParameterKey("devopsGroupName");
        devopsGroupNameParam.setParameterValue(config.getDevopsGroupName());
        parameters.add(devopsGroupNameParam);

        // Parameter: DevOps User Name
        Parameter devopsUserNameParam = new Parameter();
        devopsUserNameParam.setParameterKey("devopsUserName");
        devopsUserNameParam.setParameterValue(config.getDevopsUserName());
        parameters.add(devopsUserNameParam);

        // Set params
        stackRequest.setParameters(parameters);

        // Run Stack
        CreateStackResult stackResult = null;
        try {

            // Create Stack
            LOGGER.info("Creating stack: " + stackRequest.getStackName());
            stackResult = cloudFormation.createStack(stackRequest);
            LOGGER.info(stackResult.toString());

        } catch (AlreadyExistsException e) {

            LOGGER.severe("Stack already exists: " + stackRequest.getStackName());

        }

        return stackResult;

    }

    static String createSSHKey(MagicBeansConfig config) {
        JSch jsch=new JSch();

        String keyFileName = config.getHomePath() + "/keys/" + config.getDevopsUserName();
        String keyFingerprint = null;
        KeyPair keyPair= null;
        try {
            keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA);
            keyPair.writePrivateKey(keyFileName);
            keyPair.writePublicKey(keyFileName + ".pub", config.getDevopsUserName());
            keyFingerprint = keyPair.getFingerPrint();
            keyPair.dispose();
        } catch (JSchException | IOException e) {
            e.printStackTrace();
        }

        return keyFingerprint;

    }

    private static AwsClientBuilder initAwsBuilder(MagicBeansConfig config, AwsClientBuilder builder) {
        builder.setRegion(config.getRegion());
        builder.setCredentials(new ProfileCredentialsProvider(config.getProfile()));
        return builder;
    }

    private static AWSCodeCommit initCodeCommit(MagicBeansConfig config) {
        AWSCodeCommitClientBuilder builder = AWSCodeCommitClientBuilder.standard();
        initAwsBuilder(config, builder);
        return builder.build();
    }

    private static AmazonS3 initS3(MagicBeansConfig config) {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        initAwsBuilder(config, builder);
        return builder.build();
    }

    private static AmazonCloudFormation initCloudFormation(MagicBeansConfig config) {
        AmazonCloudFormationClientBuilder builder = AmazonCloudFormationClientBuilder.standard();
        initAwsBuilder(config, builder);
        return builder.build();
    }

}
