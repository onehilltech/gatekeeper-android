package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ConfigurationTest
{
  @Test
  public void testLoadFromMetadata () throws Exception
  {
    Context targetContext = InstrumentationRegistry.getTargetContext ();
    Configuration config = Configuration.loadFromMetadata (targetContext);

    Assert.assertEquals (targetContext.getString (com.onehilltech.gatekeeper.test.R.string.gatekeeper_baseuri), config.baseUri);
    Assert.assertEquals (targetContext.getString (com.onehilltech.gatekeeper.test.R.string.gatekeeper_client_id), config.clientId);
    Assert.assertEquals (targetContext.getString (com.onehilltech.gatekeeper.test.R.string.gatekeeper_client_secret), config.clientSecret);
  }
}