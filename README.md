android-gatekeeper
==================

[![Download](https://jitpack.io/v/onehilltech/android-gatekeeper.svg)](https://jitpack.io/#onehilltech/android-gatekeeper)
[![Build Status](https://travis-ci.org/onehilltech/android-gatekeeper.svg)](https://travis-ci.org/onehilltech/android-gatekeeper)

Gatekeeper support library for Android.

## Installation

#### Gradle

```
buildscript {
  repositories {
    maven { url "https://jitpack.io" }
  }
}

dependencies {
  compile com.github.onehilltech:android-gatekeeper:x.y.z
}
```

## Getting Started

### Configuring meta-data in AndroidManifest.xml

Use the `gatekeeper-cli` to add a new client that represents the mobile application to 
the database. Then, define the following values in `strings.xml`:

```xml
<!-- make sure to add trailing / -->
<resources>
  <!-- // other things -->
  
  <string name="gatekeeper_baseuri">URL for Gatekeeper</string>

  <!-- values generated by gatekeeper-cli -->
  <string name="gatekeeper_client_id">CLIENT ID</string>
  <string name="gatekeeper_client_secret">CLIENT SECRET</string>
</resources>  
```

The strings then need to be referenced as meta-data in `AndroidManifest.xml`.

```xml
<application>
  <!-- // other things -->
  
  <!-- Gatekeeper metadata -->
  <meta-data
      android:name="com.onehilltech.gatekeeper.android.baseuri"
      android:resource="@string/gatekeeper_baseuri"/>

  <meta-data
      android:name="com.onehilltech.gatekeeper.android.client_id"
      android:resource="@string/gatekeeper_client_id"/>

  <meta-data
      android:name="com.onehilltech.gatekeeper.android.client_secret"
      android:resource="@string/gatekeeper_client_secret"/>
</application>
```

### Initialize Gatekeeper in the application

Update your `Application` class to call `Gatekeeper.init (context)`. You must also
initialize [DBFlow](https://github.com/Raizlabs/DBFlow).

```javascript
public class TheApplication extends Application
{
  @Override
  public void onCreate ()
  {
    super.onCreate ();

    FlowManager.init (
        new FlowConfig.Builder (this)
            .openDatabasesOnInit (true)
            .build ());

    // Initialize Gatekeeper
    Gatekeeper.initialize (this);
  }
}
```

### Login / New account activity 

Export the default activities for login and creating a new account, if applicable, to 
`AndroidManifest.xml`.

```xml
<activity android:name="com.onehilltech.gatekeeper.android.SingleUserLoginActivity" />
<activity android:name="com.onehilltech.gatekeeper.android.NewAccountActivity" />
```

### Protecting activities

Last, we need to protect the activities that require login. Create a
`SingleUserSessionClient` in each activity, and make sure the user is 
logged in before continuing on the `Activity.onStart()` method. This 
will ensure that regardless of how the user enter the application, they 
must be logged in.

```java
public class MyActivity extends AppCompatActivity
  implements SingleUserSessionClient.Listener
{
  private SingleUserSessionClient session_;

  @Override
  protected void onCreate (@Nullable Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);

    this.setContentView (R.layout.activity_main);

    this.session_ = new SingleUserSessionClient.Builder (this).build ();
    this.session_.setListener (this);
  }

  @Override
  protected void onStart ()
  {
    super.onStart ();

    // Make sure the user it logged in.
    this.session_.checkLoggedIn (this, SingleUserLoginActivity.class);
    
    // If you do not want create a SingleUserSessionClient object, 
    // then use:
    //
    // SingleUserSessionClient.ensureLoggedIn (this, SingleUserLoginActivity.class);
  }

  @Override
  public void onLogin (SingleUserSessionClient client)
  {
    // Handle the user logging in
  }

  @Override
  public void onLogout (SingleUserSessionClient client)
  {
    // Handle the user logging out
  }
}
```

## Custom Activities