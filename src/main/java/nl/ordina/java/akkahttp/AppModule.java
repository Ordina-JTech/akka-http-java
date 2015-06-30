package nl.ordina.java.akkahttp;

import com.google.inject.AbstractModule;

public class AppModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(Server.class);
  }
}