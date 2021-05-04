# Trigger Composer DAG using Cloud Function
Trigger a DAG in Cloud Composer when an object is uploaded to a Google Cloud Storage bucket.


## Requirements
 - Java 11 runtime environment for the Cloud Function


## Setup

### Cloud Function
- Set the `Trigger` to Cloud Storage and the `Event Type` to `Finalize/Create`

- Use the Compute Engine Default Service Account or create a new Service Account that has the `Composer User` role.

- Configure the following environment variables

    | Key          | Value                          |
    | ------------ | ------------------------------ |
    | WEBSERVER_ID | \<tenant-project-id\>          |
    | DAG_NAME     | \<dag-to-trigger\>             |
    | CLIENT_ID    | \<client-id-of-the-IAM-proxy\> |

    The tenant project ID can be found in the Airflow Webserver URL in the form \<tenant-project-id\>.appspot.com
    
    The Client ID of the IAM proxy can be found in the IAP URL after clicking on the Airflow Webserver link in the Composer console.

### Composer
Add the following Airflow Configuration override to avoid a 403 Forbidden error.

| Section | Key          | Value                            |
| ------- | ------------ | -------------------------------- |
| api	  | auth_backend | airflow.api.auth.backend.default |