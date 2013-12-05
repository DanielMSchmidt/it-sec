/***********************************************************************

   BadAssWebServer.java


   This toy web server is used to illustrate security vulnerabilities.
   This web server only supports extremely simple HTTP GET and HTTP PUT
   requests.

 ***********************************************************************/

package com.serie5;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.commons.codec.binary.Base64;

public class BadAssWebServer {

  /* Run the HTTP server on this TCP port. */
  private static final int PORT = 8080;

  private static ArrayList<String> users = new ArrayList<String>();
  /*
   * The socket used to process incoming connections from web clients
   */
  private static ServerSocket dServerSocket;

  public BadAssWebServer() throws Exception {
    dServerSocket = new ServerSocket(PORT);

    users.add("daniel:danielspwd");
  }

  public void run() throws Exception {
    while (true) {
      /* wait for a connection from a client */
      Socket s = dServerSocket.accept();

      /* then process the client's request */
      processRequest(s);
    }
  }

  /*
   * Reads the HTTP request from the client, and responds with the file the
   * user requested or a HTTP error code.
   */
  public void processRequest(Socket s) throws Exception {
    /* used to read data from the client */
    BufferedReader br = new BufferedReader(new InputStreamReader(
        s.getInputStream()));

    /* used to write data to the client */
    OutputStreamWriter osw = new OutputStreamWriter(s.getOutputStream());

    /* read the HTTP request from the client */
    String request = br.readLine();

    String command = null;
    String pathname = null;
    String passwordtoken = null;

    /* parse the HTTP request */
    StringTokenizer st = new StringTokenizer(request, " ");

    command = st.nextToken();
    pathname = st.nextToken();
    passwordtoken = st.nextToken();

    if (checkPassword(passwordtoken)){
      if (valid(pathname)) {
        if (command.equals("GET")) {
          /*
           * if the request is a GET try to respond with the file the user
           * is requesting
           */
          serveFile(osw, pathname);
        } else if (command.equals("PUT")) {
          /*
           * if the request is a PUT try to store the file where the user
           * is requesting
           */
          storeFile(br, osw, pathname);
        } else {
          /*
           * if the request is a NOT a GET, return an error saying this
           * server does not implement the requested command
           */
          osw.write("HTTP/1.0 501 Not Implemented\n\n");
        }
      } else {
        System.out.println("This request was evil: " + pathname);
      }
    }else{
      osw.write("HTTP/1.0 403 Forbidden \n\n");
    }

    /* close the connection to the client */
    osw.close();
  }

  private boolean checkPassword(String token){
    // TODO: How do i ask for a password?

    // encode the token
    byte[] byteArray = Base64.decodeBase64(token.getBytes());
    String decodedString = new String(byteArray);

    // check if its within the valid ones
    for(int i = 0; i < users.size(); i++){
      if (users.get(i) == decodedString){
        return true;
      }
    }
    return false;
  }

  private boolean valid(String pathname) {
    return !(pathname.contains("/../") || pathname.contains("//"));
  }

  public void serveFile(OutputStreamWriter osw, String pathname)
      throws Exception {
    FileReader fr = null;
    BufferedReader br = null;
    int c = -1;
    StringBuffer sb = new StringBuffer();

    /*
     * remove the initial slash at the beginning of the pathname in the
     * request
     */
    if (pathname.charAt(0) == '/')
      pathname = pathname.substring(1);

    /*
     * if there was no filename specified by the client, serve the
     * "index.html" file
     */
    if (pathname.equals(""))
      pathname = "index.html";

    /* try to open file specified by pathname */
    try {
      fr = new FileReader(pathname);
      br = new BufferedReader(fr);

      File file = new File(pathname);
      if (file.length() > Runtime.getRuntime().freeMemory()){
        // Hilft nicht, falls wärend die Datei eingelesen wird ein anderer Prozess Speicher verlangt und bekommt
        throw new Exception("I am too fat :(");
      }

      // TODO: Why do we need thi? To test if the file can be read?
      c = fr.read();
    } catch (Exception e) {
      /*
       * if the file is not found,return the appropriate HTTP response
       * code
       */
      osw.write("HTTP/1.0 404 Not Found\n\n");
      return;
    }

    /*
     * if the requested file can be successfully opened and read, then
     * return an OK response code and send the contents of the file
     */
    osw.write("HTTP/1.0 200 OK\n\n");
    String line;
    while (c != -1 && (line = br.readLine()) != null) {
      osw.append(line); // Append the file line by line
    }


  }

  public void storeFile(BufferedReader br, OutputStreamWriter osw,
      String pathname) throws Exception {
    FileWriter fw = null;
    try {
      fw = new FileWriter(pathname);
      String s = br.readLine();
      while (s != null && s.length() > 0) {
        fw.write(s);
        s = br.readLine();
      }
      fw.close();
      osw.write("HTTP/1.0 201 Created");
    } catch (Exception e) {
      osw.write("HTTP/1.0 500 Internal Server Error");
    }
  }

  /*
   * This method is called when the program is run from the command line.
   */
  public static void main(String argv[]) throws Exception {

    /* Create a BadAssWebServer object, and run it */
    BadAssWebServer sws = new BadAssWebServer();
    sws.run();
  }
}