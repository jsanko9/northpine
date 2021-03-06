package com.northpine.scrape.ogr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public abstract class OgrCollector {

  protected static final Logger log = LoggerFactory.getLogger(OgrCollector.class);

  protected String poolBase;

  private String ogrFormat;

  private String extension;

  protected OgrCollector(String poolBase, String ogrFormat, String extension) {
    this.poolBase = poolBase;
    this.ogrFormat = ogrFormat;
    this.extension = extension;
  }

  public boolean convert(String file) {
    ProcessBuilder builder = new ProcessBuilder( "ogr2ogr", "-f", ogrFormat, poolBase + extension, file );
    try {
      Process p;
      p = builder.start();
      log.info("Running command: " + builder.command().stream().reduce("", (pr, c) -> pr + " " + c));
      int status = p.waitFor();
      log.info("Process finished, status code: " + status);
      var error = new BufferedReader(new InputStreamReader(p.getErrorStream()))
          .lines().parallel()
          .collect(Collectors.joining("\n")
      );
      if(error.length() > 0) log.warn(error);
      if(status == 0) {
        CompletableFuture.runAsync( () -> {
          try {
            Files.delete( Paths.get( file ) );
          } catch ( IOException e ) {
            log.warn("Couldn't delete " + file, e);
          }
        } );
        return true;
      } else {
        return false;
      }
    } catch ( IOException | InterruptedException e ) {
      log.error("ogr2ogr failed", e);
      return false;
    }
  }

  public abstract File zipUpPool();
}
