# AWS SAM DynamoDB Application for Managing Packages

## Requirements

* AWS CLI already configured with at least PowerUser permission
* [Java SE Development Kit 8 installed](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Docker installed](https://www.docker.com/community-edition)
* [Maven](https://maven.apache.org/install.html)
* [SAM CLI](https://github.com/awslabs/aws-sam-cli)
* [Python 3](https://docs.python.org/3/)

## Setup process

### Installing dependencies

We use `maven` to install our dependencies and package our application into a JAR file:

```bash
mvn clean package
```

### Local Installation Recipe

**Invoking function locally through local API Gateway**
1. Create docker network bridge. `docker network create dynamodb-network`
2. Start DynamoDB Local in a Docker container. `docker run -d -v "$PWD":/dynamodb_local_db -p 8000:8000 --network dynamodb-network --name dynamodb cnadiminti/dynamodb-local`
3. Create the DynamoDB table. `aws dynamodb create-table --table-name products_table --attribute-definitions AttributeName=productId,AttributeType=S --key-schema AttributeName=productId,KeyType=HASH --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 --endpoint-url http://localhost:8000`
1. List created tables on DynamoDB . `aws dynamodb list-tables --endpoint-url http://localhost:8000`
4. Start the SAM local API.
 - On a Mac: `sam local start-api -d 5858 --docker-network dynamodb-network -n src/test/resources/test_environment_mac.json`
 - On Windows: `sam local start-api -d 5858 --docker-network dynamodb-network -n src/test/resources/test_environment_windows.json`
 - On Linux: `sam local start-api -d 5858 --docker-network dynamodb-network -n src/test/resources/test_environment_linux.json`
 
 Basic API calls
 
 ```bash
 curl --location --request POST 'http://127.0.0.1:3000/products' \
 --header 'Content-Type: application/json' \
 --data-raw '{
   "productType" : "box",
   "deliveryDate" : "2020-10-08 13:00 - 15:00"
 }
 ```
 
 ```bash
 curl --location --request GET 'http://127.0.0.1:3000/products/3fce08a3-cc35-46a5-a440-e2d1b43d2979'
 ```
 ```bash
 curl --location --request GET 'http://127.0.0.1:3000/packages'
 ```

If the previous command ran successfully you should now be able to create and get product inside of the package to hit the following local endpoint to
invoke the functions rooted at `http://localhost:3000/products`

**SAM CLI** is used to emulate both Lambda and API Gateway locally and uses our `template.yaml` to
understand how to bootstrap this environment (runtime, where the source code is, etc.) - The
following excerpt is what the CLI will read in order to initialize an API and its routes:

```yaml
...
Events:
    GetPackages:
        Type: Api
        Properties:
            Path: /packages
            Method: get
```

## Packaging and deployment

AWS Lambda Java runtime accepts either a zip file or a standalone JAR file - We use the latter in
this example. SAM will use `CodeUri` property to know where to look up for both application and
dependencies:

```yaml
...
    GetPackagesFunction:
        Type: AWS::Serverless::Function
        Properties:
            CodeUri: target/package-service-1.0.0.jar
            Handler: com.postnl.handler.GetPackagesHandler::handleRequest
```

Firstly, we need a `S3 bucket` where we can upload our Lambda functions packaged as ZIP before we
deploy anything - If you don't have a S3 bucket to store code artifacts then this is a good time to
create one:

```bash
export BUCKET_NAME=my_cool_new_bucket
aws s3 mb s3://$BUCKET_NAME
```

Next, run the following command to package our Lambda function to S3:

```bash
sam package \
    --template-file template.yaml \
    --output-template-file packaged.yaml \
    --s3-bucket $BUCKET_NAME
```

Next, the following command will create a Cloudformation Stack and deploy your SAM resources.

```bash
sam deploy \
    --template-file packaged.yaml \
    --stack-name sam-packageHandler \
    --capabilities CAPABILITY_IAM
```

After deployment is complete you can run the following command to retrieve the API Gateway Endpoint URL:

```bash
aws cloudformation describe-stacks \
    --stack-name sam-packageHandler \
    --query 'Stacks[].Outputs'
```

## Testing

### Running unit tests
We use `JUnit` for testing our code.
Unit tests in this sample package mock out the DynamoDBTableMapper class for Product objects.
Unit tests do not require connectivity to a DynamoDB endpoint. You can run unit tests with the
following command:

```bash
mvn clean test
```

### Running integration tests
Integration tests in this sample package do not mock out the DynamoDBTableMapper and use a real
AmazonDynamoDB client instance. Integration tests require connectivity to a DynamoDB endpoint, and
as such the POM starts DynamoDB Local from the Dockerhub repository for integration tests.

```bash
mvn clean verify
```

The number that follows the test script name is the number of products to create in the
test. For these tests to work, you must follow the steps for [local development](#local-development).  

# Appendix

## AWS CLI commands

AWS CLI commands to package, deploy and describe outputs defined within the cloudformation stack:

```bash
sam package \
    --template-file template.yaml \
    --output-template-file packaged.yaml \
    --s3-bucket REPLACE_THIS_WITH_YOUR_S3_BUCKET_NAME

sam deploy \
    --template-file packaged.yaml \
    --stack-name sam-packageHandler \
    --capabilities CAPABILITY_IAM \
    --parameter-overrides MyParameterSample=MySampleValue

aws cloudformation describe-stacks \
    --stack-name sam-packageHandler --query 'Stacks[].Outputs'
```
