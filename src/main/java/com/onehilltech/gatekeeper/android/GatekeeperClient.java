package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.onehilltech.metadata.ManifestMetadata;
import com.onehilltech.metadata.MetadataProperty;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Client interface for communicating with a Gatekeeper service.
 */
public class GatekeeperClient
{
  /// Logging tag.
  private static final String TAG = "GatekeeperClient";

  /// Base URI for the Gatekeeper service.
  private final String baseUri_;

  /// The client id.
  private final String clientId_;

  /// Underlying HTTP client.
  private final RequestQueue requestQueue_;

  /// Authorization token for the current user.
  private BearerToken userToken_;

  private BearerToken clientToken_;

  /**
   * Listener interface for initializing the client.
   */
  public interface OnInitialized
  {
    /**
     * Callback for completion of the initialization process.
     * @param client
     */
    void onInitialized (GatekeeperClient client);

    /**
     * Callback for an error.
     *
     * @param error
     */
    void onError (VolleyError error);
  }

  @JsonTypeInfo(
      use=JsonTypeInfo.Id.NAME,
      include=JsonTypeInfo.As.PROPERTY,
      property="grant_type")
  @JsonSubTypes({
      @JsonSubTypes.Type (value=Password.class, name="password"),
      @JsonSubTypes.Type (value=ClientCredentials.class, name="client_credentials"),
      @JsonSubTypes.Type (value=RefreshToken.class, name="refresh_token")})
  @JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE)
  public static class Grant
  {
    @JsonProperty("client_id")
    public String clientId;
  }

  public static class ClientCredentials extends Grant
  {
    @JsonProperty("client_secret")
    public String clientSecret;
  }

  public static class Password extends Grant
  {
    public String username;
    public String password;
  }

  public class RefreshToken extends Grant
  {
    @JsonProperty("refresh_token")
    public String refreshToken;
  }


  /**
   * Configuration options for the GatekeeperClient. The options can be loaded
   * from the AndroidManifest.xml.
   */
  public static class Options
  {
    public static final String CLIENT_ID = "com.onehilltech.gatekeeper.android.client_id";
    public static final String CLIENT_SECRET = "com.onehilltech.gatekeeper.android.client_secret";
    public static final String BASE_URI = "com.onehilltech.gatekeeper.android.baseuri";
    public static final String BASE_URI_EMULATOR = "com.onehilltech.gatekeeper.android.baseuri_emulator";

    @MetadataProperty(name=CLIENT_ID, fromResource=true)
    public String clientId;

    @MetadataProperty(name=CLIENT_SECRET, fromResource=true)
    public String clientSecret;

    @MetadataProperty(name=BASE_URI, fromResource=true)
    public String baseUri;

    @MetadataProperty(name=BASE_URI_EMULATOR, fromResource=true)
    public String getBaseUriEmulator;

    /**
     * Get the correct base uri based on where the application is running. This will return
     * either baseUri or getBaseUriEmulator.
     *
     * @return
     */
    public String getBaseUri ()
    {
      boolean isEmulator = Build.PRODUCT.startsWith ("sdk_google");
      return isEmulator ? this.getBaseUriEmulator : this.baseUri;
    }
  }

  /**
   * Initialize a new GatekeeperClient using information from the metadata in
   * AndroidManifest.xml.
   *
   * @param context         Target context
   * @param onInitialized   Callback for initialization
   *
   * @throws PackageManager.NameNotFoundException
   * @throws IllegalAccessException
   * @throws ClassNotFoundException
   * @throws InvocationTargetException
   */
  public static JsonRequest<Token> initialize (Context context, OnInitialized onInitialized)
      throws PackageManager.NameNotFoundException,
      IllegalAccessException,
      ClassNotFoundException,
      InvocationTargetException
  {
    RequestQueue requestQueue = Volley.newRequestQueue (context);
    return initialize (context, requestQueue, onInitialized);
  }

  public static JsonRequest<Token> initialize (Context context,
                                               RequestQueue requestQueue,
                                               OnInitialized onInitialized)
      throws PackageManager.NameNotFoundException,
      IllegalAccessException,
      ClassNotFoundException,
      InvocationTargetException
  {
    Options options = new Options ();
    ManifestMetadata.get (context).initFromMetadata (options);

    return initialize (options, requestQueue, onInitialized);
  }

  /**
   * Initialize a new GatekeeperClient object.
   *
   * @param options           Initialization options
   * @param requestQueue      Volley RequestQueue for requests
   * @param onInitialized     Callback for initialization.
   */
  public static JsonRequest<Token> initialize (final Options options,
                                               final RequestQueue requestQueue,
                                               final OnInitialized onInitialized)
  {
    // To initialize the client, we must first get a token for the client. This
    // allows us to determine if the client is enabled. It also setups the client
    // object with the required token.
    String url = options.getBaseUri () + "/oauth2/token";

    JsonRequest<Token> request =
        new JsonRequest<> (
            Request.Method.POST,
            url,
            null,
            new ResponseListener <Token> (new TypeReference <Token> () {}) {
              @Override
              public void onErrorResponse (VolleyError error)
              {
                onInitialized.onError (error);
              }

              @Override
              public void onResponse (Token response)
              {
                response.accept (new TokenVisitor () {
                  @Override
                  public void visitBearerToken (BearerToken token)
                  {
                    GatekeeperClient client =
                        new GatekeeperClient (
                            options.baseUri,
                            options.clientId,
                            token,
                            requestQueue);

                    onInitialized.onInitialized (client);
                  }
                });
              }
            });

    ClientCredentials clientCreds = new ClientCredentials ();
    clientCreds.clientId = options.clientId;
    clientCreds.clientSecret = options.clientSecret;
    request.setData (clientCreds);

    requestQueue.add (request);

    return request;
  }

  /**
   * Initializing constructor.
   *
   * @param baseUri
   * @param clientToken
   */
  GatekeeperClient (String baseUri,
                    String clientId,
                    BearerToken clientToken,
                    RequestQueue requestQueue)
  {
    this.baseUri_ = baseUri;
    this.clientId_ = clientId;
    this.clientToken_ = clientToken;
    this.requestQueue_ = requestQueue;
  }

  /**
   * Get the baseuri for the GatekeeperClient.
   *
   * @return
   */
  public String getBaseUri ()
  {
    return this.baseUri_;
  }

  /**
   * Test if the client has a token.
   *
   * @return
   */
  public boolean isLoggedIn ()
  {
    return this.userToken_ != null;
  }

  /**
   * Get the client id.
   *
   * @return
   */
  public String getClientId ()
  {
    return this.clientId_;
  }

  /**
   * Create a new account on the Gatekeeper server. The result of this method is a Boolean
   * value that determines if the account was created. Once the account has been created,
   * the application should request a token on behalf of the newly created user.
   *
   * @param username
   * @param password
   */
  public void createAccount (final String username,
                             final String password,
                             final String email,
                             final ResponseListener<Boolean> listener)
  {
    class Data
    {
      @JsonProperty("client_id")
      public String clientId;

      public String username;
      public String password;
      public String email;
    }

    String url = this.getCompleteUrl ("/accounts");

    JsonRequest<Boolean> request =
        new JsonRequest<> (
            Request.Method.POST,
            url,
            this.clientToken_,
            listener);

    Data data = new Data ();
    data.clientId = this.clientId_;
    data.username = username;
    data.password = password;
    data.email = email;

    request.setData (data);

    this.requestQueue_.add (request);
  }

  /**
   * Get an access token for the user.
   *
   * @param username
   * @param password
   * @param listener
   */
  public JsonRequest<Token> getUserToken (String username,
                                               String password,
                                               ResponseListener<BearerToken> listener)
  {
    Password passwd = new Password ();
    passwd.clientId = this.clientId_;
    passwd.username = username;
    passwd.password = password;

    return this.getToken (passwd, listener);
  }

  /**
   * Refresh the current access token.
   *
   * @param listener
   */
  public JsonRequest<Token> refreshToken (ResponseListener <BearerToken> listener)
  {
    if (!this.userToken_.canRefresh ())
      throw new IllegalStateException ("Current token cannot be refreshed");

    RefreshToken refreshToken = new RefreshToken ();
    refreshToken.clientId = this.clientId_;
    refreshToken.refreshToken = this.userToken_.getRefreshToken ();

    return this.getToken (refreshToken, listener);
  }

  private JsonRequest<Token> getToken (Grant grantType, final ResponseListener<BearerToken> listener)
  {
    final String url = this.getCompleteUrl ("/oauth2/token");

    JsonRequest<Token> request =
        this.makeJsonRequest (
            Request.Method.POST,
            url,
            new ResponseListener<Token> (new TypeReference<Token> ()
            {
            })
            {
              @Override
              public void onErrorResponse (VolleyError error)
              {
                listener.onErrorResponse (error);
              }

              @Override
              public void onResponse (Token response)
              {
                response.accept (new TokenVisitor ()
                {
                  @Override
                  public void visitBearerToken (BearerToken token)
                  {
                    userToken_ = token;
                    listener.onResponse (token);
                  }
                });
              }
            });

    request.setData (grantType);
    this.requestQueue_.add (request);

    return request;
  }

  /**
   * Logout the current user.
   *
   * @param listener      Response listener
   */
  public void logout (ResponseListener <Boolean> listener)
  {
    String url = this.getCompleteUrl ("/oauth2/logout");
    JsonRequest<Boolean> request = this.makeJsonRequest (Request.Method.GET, url, listener);

    this.requestQueue_.add (request);
  }

  /**
   * Factory method for creating a protected request. The request will include the
   * current token in the HTTP request header.
   *
   * @param method        HTTP method
   * @param path          Full path of the resource
   * @param listener      Response listener
   * @param <T>           Object type of response body
   * @return              Request object
   */
  public <T> JsonRequest<T> makeJsonRequest (int method, String path, ResponseListener<T> listener)
  {
    return new JsonRequest<> (method, path, this.userToken_, listener);
  }

  /**
   * Add a request to the queue.
   *
   * @param request        Add request to queue
   */
  public void addRequest (Request <?> request)
  {
    this.requestQueue_.add (request);
  }

  /**
   * Get the complete URL for a path.
   *
   * @param relativePath   Relative path of the url
   * @return String containing the complete url path
   */
  private String getCompleteUrl (String relativePath)
  {
    return this.baseUri_ + relativePath;
  }
}
