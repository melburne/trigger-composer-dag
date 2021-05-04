package org.example;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TriggerDag implements BackgroundFunction<GcsEvent> {
  private static final Logger LOG = LoggerFactory.getLogger(TriggerDag.class);

  private static final String IAM_SCOPE = "https://www.googleapis.com/auth/iam";

  /*
   * Can be found in the Airflow Webserver URL
   * <tenant-project-id>.appspot.com
   */
  private static final String WEBSERVER_ID = System.getenv("WEBSERVER_ID");

  /*
   * Name of the Composer DAG to trigger.
   */
  private static final String DAG_NAME = System.getenv("DAG_NAME");

  /*
   * Obtained by running 'curl -v $AIRFLOW_URL 2>&1 >/dev/null | grep "location:"'. More info -
   * https://cloud.google.com/composer/docs/how-to/using/triggering-with-gcf#get_the_client_id_of_the_iam_proxy
   *
   * Can also be found in the IAP URL after clicking on the Airflow Webserver link in the Composer console.
   */
  private static final String CLIENT_ID = System.getenv("CLIENT_ID");

  private static final String COMPOSER_URI =
      String.format(
          "https://%s.appspot.com/api/experimental/dags/%s/dag_runs", WEBSERVER_ID, DAG_NAME);

  @Override
  public void accept(GcsEvent gcsEvent, Context context) {
    Preconditions.checkNotNull(WEBSERVER_ID, "WEBSERVER_ID environment variable is not set");
    Preconditions.checkNotNull(DAG_NAME, "DAG_NAME environment variable is not set");
    Preconditions.checkNotNull(CLIENT_ID, "CLIENT_ID environment variable is not set");

    BodyPublisher requestBody = getBody(gcsEvent.getBucket(), gcsEvent.getName());

    triggerDag(requestBody);
  }
  
  /**
   * Builds the body for the POST request.
   *
   * @param bucketName GCS bucket name to pass to the Composer DAG
   * @param filePath GCS file path to pass to the Composer DAG
   * @return {@link BodyPublisher}
   */
  private BodyPublisher getBody(String bucketName, String filePath) {
    String data = "{\"bucketName\": \"" + bucketName + "\", \"filePath\": \"" + filePath + "\"}";
    String conf = "{\"conf\": " + data + "}";
    return BodyPublishers.ofString(conf);
  }

  /**
   * Makes a HTTP POST request to trigger a Composer DAG.
   *
   * @param requestBody Request body for the POST request
   */
  private void triggerDag(BodyPublisher requestBody) {
    URI uri = null;
    try {
      uri = new URI(COMPOSER_URI);
    } catch (URISyntaxException e) {
      LOG.error(e.getMessage(), e);
    }

    String token = getToken();

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(uri)
            .header("Authorization", String.format("Bearer %s", token))
            .header("Content-Type", "application/json")
            .POST(requestBody)
            .build();

    HttpClient client = HttpClient.newBuilder().build();

    HttpResponse<String> response;
    try {
      response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        LOG.info("Status: {}. Response: {}", response.statusCode(), response.body());
      } else {
        LOG.error("Status: {}. Response: {}", response.statusCode(), response.body());
      }
    } catch (IOException | InterruptedException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  /**
   * Retrieves the OpenID Connect (OIDC) token from the service account used by the Cloud Function.
   *
   * @return OIDC token
   */
  private String getToken() {
    GoogleCredentials credentials;
    IdTokenCredentials tokenCredentials;
    try {
      credentials =
          GoogleCredentials.getApplicationDefault().createScoped(Collections.singleton(IAM_SCOPE));
      credentials.refresh();

      tokenCredentials =
          IdTokenCredentials.newBuilder()
              .setIdTokenProvider((IdTokenProvider) credentials)
              .setTargetAudience(CLIENT_ID)
              .build();

      tokenCredentials.refresh();
    } catch (IOException e) {
      LOG.error("Could not generate token from service account");
      throw new RuntimeException("Could not generate credentials from service account");
    }

    return tokenCredentials.getAccessToken().getTokenValue();
  }
}
