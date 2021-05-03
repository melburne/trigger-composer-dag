# Trigger Composer DAG using Cloud Function
Trigger a DAG in Cloud Composer when an object is uploaded to a Google Cloud Storage bucket.


## Requirements
 - Java 11


## Setup

### Cloud Function
Configure the following environment variables

| Key          | Value                        |
| ------------ | ---------------------------- |
| WEBSERVER_ID | <tenant-project-id>          |
| DAG_NAME     | <dag-to-trigger>             |
| CLIENT_ID    | <client-id-of-the-IAM-proxy> |

The tenant project ID can be found in the Airflow Webserver URL in the form <tenant-project-id>.appspot.com

The Client ID of the IAM proxy can be found in the IAP URL after clicking on the Airflow Webserver link in the Composer console.

### Composer
Add the following Airflow Configuration override

|Section | Key          | Value                            |
| ------ | ------------ | -------------------------------- |
| api	 | auth_backend	| airflow.api.auth.backend.default |