package com.archsynthe;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.codecommit.AWSCodeCommit;
import com.amazonaws.services.codecommit.AWSCodeCommitClientBuilder;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

/**
 * @author James Adams (jamesa@slalom.com)
 * @version ${VERSION}
 * @since ${VERSION}
 */
public class MagicBeansServiceManager {

    private static AwsClientBuilder initAwsBuilder(MagicBeansConfig config, AwsClientBuilder builder) {
        builder.setRegion(config.getRegion());
        builder.setCredentials(new ProfileCredentialsProvider(config.getProfile()));
        return builder;
    }

    static AmazonCloudFormation initCloudFormation(MagicBeansConfig config) {
        AmazonCloudFormationClientBuilder builder = AmazonCloudFormationClientBuilder.standard();
        initAwsBuilder(config, builder);
        return builder.build();
    }

    static AWSCodeCommit initCodeCommit(MagicBeansConfig config) {
        AWSCodeCommitClientBuilder builder = AWSCodeCommitClientBuilder.standard();
        initAwsBuilder(config, builder);
        return builder.build();
    }

    static AmazonS3 initS3(MagicBeansConfig config) {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        initAwsBuilder(config, builder);
        return builder.build();
    }

    static AmazonIdentityManagement initIAM(MagicBeansConfig config) {
        AmazonIdentityManagementClientBuilder builder = AmazonIdentityManagementClientBuilder.standard();
        initAwsBuilder(config, builder);
        return builder.build();
    }

}
