package com.bigbaldy.poker.filter;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;

public class HttpJsonContentServletRequest extends HttpServletRequestWrapper {

  private static final Logger logger = LoggerFactory.getLogger(HttpJsonContentServletRequest.class);

  private ByteArrayOutputStream inputStream;

  public HttpJsonContentServletRequest(HttpServletRequest request) {
    super(request);
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    return new JsonContentRelayInputStream(getOutputStream().toByteArray());
  }

  @Override
  public BufferedReader getReader() throws IOException {
    String enc = getCharacterEncoding();
    if (enc == null) enc = "UTF-8";
    return new BufferedReader(new InputStreamReader(this.getInputStream(), enc));
  }

  public String getRequestBody() {
    switch (getMethod()) {
      case "HEAD":
      case "GET":
      case "DELETE":
        return getQueryString();
      case "POST":
      case "PUT":
      case "PATCH":
        String characterEncoding = getCharacterEncoding();
        Charset charset =
            characterEncoding != null
                ? Charset.forName(characterEncoding)
                : Charset.defaultCharset();

        try {
          return new String(getOutputStream().toByteArray(), charset);
        } catch (IOException e) {
          logger.error("Failed to get output stream", e);
        }

        return "";
      default:
        return "";
    }
  }

  private ByteArrayOutputStream getOutputStream() throws IOException {
    if (inputStream == null) {
      inputStream = new ByteArrayOutputStream();
      IOUtils.copy(super.getInputStream(), inputStream);
    }

    return inputStream;
  }

  private static class JsonContentRelayInputStream extends ServletInputStream {

    private ByteArrayInputStream input;
    private ReadListener readListener = null;

    public JsonContentRelayInputStream(byte[] bytes) {
      input = new ByteArrayInputStream(bytes);
    }

    @Override
    public int available() {
      return input.available();
    }

    @Override
    public boolean isFinished() {
      return available() == 0;
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
      this.readListener = readListener;
      if (!isFinished()) {
        try {
          readListener.onDataAvailable();
        } catch (IOException e) {
          readListener.onError(e);
        }
      } else {
        try {
          readListener.onAllDataRead();
        } catch (IOException e) {
          readListener.onError(e);
        }
      }
    }

    @Override
    public int read() throws IOException {
      int i;
      if (!isFinished()) {
        i = input.read();
        if (isFinished() && (readListener != null)) {
          try {
            readListener.onAllDataRead();
          } catch (IOException ex) {
            readListener.onError(ex);
            throw ex;
          }
        }
        return i;
      } else {
        return -1;
      }
    }
  }
}
