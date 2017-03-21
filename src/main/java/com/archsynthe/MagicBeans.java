package com.archsynthe;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.*;
import com.amazonaws.services.codecommit.AWSCodeCommit;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.GetUserRequest;
import com.amazonaws.services.identitymanagement.model.GetUserResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.UploadSSHPublicKeyResult;
import com.amazonaws.services.s3.AmazonS3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author James Adams (jamesa@slalom.com)
 * @version ${VERSION}
 * @since ${VERSION}
 */
public class MagicBeans {

    private static final Logger LOGGER = Logger.getLogger(MagicBeans.class.getName());

    public static void main(String[] args) {

        LOGGER.info("########## START RUN ##########");

        // Load configuration from file
        MagicBeansConfig config = MagicBeansConfig.load();

        // Initialize services (as profile user)
        AmazonCloudFormation cloudFormation = MagicBeansServiceManager.initCloudFormation(config);
        AmazonIdentityManagement iam = MagicBeansServiceManager.initIAM(config);

        // Initialize services (as devops user)
        AWSCodeCommit codeCommit = MagicBeansServiceManager.initCodeCommit(config);
        AmazonS3 amazonS3 = MagicBeansServiceManager.initS3(config);

        switch (args[0]) {

            case "create":
                // Create DevOps Environment (root stack)
                CreateStackResult stackResult = createDevOpsEnvironment(config, cloudFormation);

                // Only proceed if we have a result
                if (stackResult != null) {

                    String keypairFingerprint = MagicBeansKeyManager.createSSHKey(config);
                    if (keypairFingerprint != null) {

                        // Wait for stack creation
                        waitOnStackStatus(config,cloudFormation,StackStatus.CREATE_COMPLETE);

                        // Retrieve stack outputs
                        DevopsEnvironmentStackOutputs outputs = retrieveStackOutputs(config,cloudFormation);

                        // Upload public key
                        UploadSSHPublicKeyResult uploadResult = MagicBeansKeyManager.uploadSSHKeyToUser(config, iam);

                        // Wait ten seconds
                        // NOTE: We are waiting here because it takes a while for the uploaded key to be
                        //       associated with the user, and there is no good way to poll for this
                        //       with the current AWS APIs.  Redo this when the APIs get better
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // Clone the CodeCommit repository
                        MagicBeansRepositoryManager.cloneRepository(config);

                    }

                }
                break;

            case "clean":
                MagicBeansKeyManager.removeSSHKeysFromUser(config, iam);
                destroyDevOpsEnvironment(config, cloudFormation);
                waitOnStackStatus(config,cloudFormation,StackStatus.DELETE_COMPLETE);
                break;

        }


        LOGGER.info("########## STOP RUN ##########");

    }

    static DevopsEnvironmentStackOutputs retrieveStackOutputs(MagicBeansConfig config, AmazonCloudFormation cloudFormation) {

        DevopsEnvironmentStackOutputs outputs = new DevopsEnvironmentStackOutputs();
        DescribeStacksRequest describeRequest = new DescribeStacksRequest();
        describeRequest.setStackName(config.getDevopsStackName());
        DescribeStacksResult describeResult = cloudFormation.describeStacks(describeRequest);
        List<Output> rawOutputs = describeResult.getStacks().get(0).getOutputs();
        for (Output rawOutput : rawOutputs) {
            switch (rawOutput.getOutputKey()) {
                case "UserAccessKeyId":
                    outputs.setUserAccessKeyId(rawOutput.getOutputValue());
                    break;
                case "UserSecretAccessKey":
                    outputs.setUserSecretAccessKey(rawOutput.getOutputValue());
                    break;
                case "RepositoryCloneUrl":
                    outputs.setRepositoryCloneUrl(rawOutput.getOutputValue());
                    break;
                case "S3BucketUrl":
                    outputs.setS3BucketUrl(rawOutput.getOutputValue());
                    break;
            }
        }
        return outputs;

    }

    static void waitOnStackStatus(MagicBeansConfig config, AmazonCloudFormation cloudFormation, StackStatus stackStatus) {

        DescribeStacksRequest describeRequest = new DescribeStacksRequest();
        describeRequest.setStackName(config.getDevopsStackName());
        DescribeStacksResult describeResult = null;

        int retries = 100;
        int waitTime = 10;

        while (waitTime > 0 && retries > 0) {
            LOGGER.info("Waiting for Stack status: waitTime=" + waitTime + "s, retries=" + retries + ",status=" + stackStatus);

            // Wait seconds
            try {
                TimeUnit.SECONDS.sleep(waitTime);
            } catch (InterruptedException e) {
                LOGGER.warning("Wait was interrupted...");
            }

            // Check for stack status
            try {
                describeResult = cloudFormation.describeStacks(describeRequest);
                StackStatus actualStatus = StackStatus.fromValue(describeResult.getStacks().get(0).getStackStatus());
                LOGGER.info("Stack Detected: stack=" + config.getDevopsStackName() + ",status=" + actualStatus);
                if (actualStatus.equals(stackStatus)) {
                    // Stack is in appropriate state, terminate loop
                    waitTime = 0;
                } else {
                    LOGGER.warning("Waiting on Stack Status: stack=" + config.getDevopsStackName() + ",actualStatus=" + actualStatus + ",desiredStatus=" + stackStatus);
                    waitTime = waitTime + 10;
                    retries = retries - 1;
                }
            } catch (AmazonCloudFormationException e) {

                if (StackStatus.DELETE_COMPLETE.equals(stackStatus)) {
                    // If waiting on stack deletion, this exception is expected
                    waitTime = 0;
                } else {
                    // If waiting for any other stack state, keep waiting
                    LOGGER.warning("Waiting on Stack Status: stack=" + config.getDevopsStackName() + ",actualStatus=" + StackStatus.DELETE_COMPLETE + ",desiredStatus=" + stackStatus);
                    waitTime = waitTime + 10;
                    retries = retries - 1;
                }
            }
        }

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

    static void destroyDevOpsEnvironment(MagicBeansConfig config, AmazonCloudFormation cloudFormation) {
        DeleteStackRequest deleteRequest = new DeleteStackRequest();
        deleteRequest.setStackName(config.getDevopsStackName());
        cloudFormation.deleteStack(deleteRequest);
    }

    static void waitOnDevOpsUserExists(MagicBeansConfig config, AmazonIdentityManagement iam) {

        GetUserRequest getUserRequest = new GetUserRequest();
        getUserRequest.setUserName(config.getDevopsUserName());
        GetUserResult userResult = null;

        int retries = 100;
        int waitTime = 10;

        while (waitTime > 0 && retries > 0) {
            LOGGER.info("Waiting for DevOps user creation: waitTime=" + waitTime + "s, retries=" + retries);

            // Wait ten seconds
            try {
                TimeUnit.SECONDS.sleep(waitTime);
            } catch (InterruptedException e) {
                LOGGER.warning("Wait was interrupted...");
            }

            // Check for user
            try {
                userResult = iam.getUser(getUserRequest);
                LOGGER.info("DevOps User detected: " + config.getDevopsUserName());
                waitTime = 0;
            } catch (NoSuchEntityException e) {
                LOGGER.warning("DevOps User doesn't exist: " + config.getDevopsUserName());
                waitTime = waitTime + 10;
                retries = retries - 1;
            }
        }

    }

}
