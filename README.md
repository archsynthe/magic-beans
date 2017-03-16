# Magic Beans

Magic Beans is a DevOps-In-A-Box solution targeted for AWS cloud implementations
which attempts to take the difficulty out of the bootstrapping process and lets
you focus on applications instead of network architecture, users and repositories.

## Software Requirements
Magic Beans relies on NodeJS and NPM (the Node Package Manager).  These two pieces
of software must be installed prior to running Magic Beans.

## Configuration Requirements

Configuration is stored in the *config.json* file at the root level of the source tree.
Magic Beans requires only a single IAM user provisioned with administrator-level privileges
in order to work.  Additional information in the configuration file is for customization
of the created product on AWS.

The IAM user must be assigned an Access Key ID and a Secret Access Key for API access, and those
values must be stored in the ~/.aws/credentials file under a profile with a name that matches
that provided in the *config.yaml* file.

- *companyName*: A short version of the company name that is used to uniquely identify resources
created in AWS
- *awsProfileId*: The profile name to use from the ~/.aws/credentials file

## Bootstrap Process

Magic Beans will follow this bootstrap process for the environment:
* Create an AWS CodeCommit repository for storing versioned CloudFormation Templates
* Create an S3 Bucket for storing published CloudFormation Templates and Scripts
* Create an IAM devops user with permissions limited following the principle of least privilege
* Create an Access Key ID and Secret Access Key for the devops user
* Create an RSA keypair for the DevOps user (used to access the CodeCommit repository)

After this bootstrap process is complete, all activities are carried out using the newly created
devops user instead of the administrator-level IAM user provided for bootstrapping.  The devops
user intentionally does not have the ability to create or remove repositories and buckets to
protect the cloud environment from unintentional harm.

### Steps to clone CodeCommit Repository

* When uploading ssh keyfile to IAM user account, retain the SSH Key ID associated with the key
* Add a block to the end of ~/.ssh/config
> Host git-codecommit.*.amazonaws.com
> > User SSH_KEY_ID \
> > IdentityFile FULL_PATH_TO_PRIVATE_KEYFILE
  
# TODO
* Migrate IAM user creation from API call to cloudformation template
  * Add policy creation for access to CodeCommit Repository
  * Create group for DevOps user and attach policy to it
