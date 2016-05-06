package com.idiominc.external.servlet.fileWriteTest;

import com.idiominc.external.AISUtils;
import com.idiominc.external.config.Config;
import com.idiominc.external.json.JSONArray;
import com.idiominc.external.json.JSONObject;
import com.idiominc.external.servlet.projectcreation.CIProject;
import com.idiominc.external.servlet.projectcreation.CIProjectConfig;
import com.idiominc.external.servlet.projectcreation.ParsedPayload;
import com.idiominc.ws.integration.compassion.CompassionSecurity;
import com.idiominc.ws.integration.profserv.commons.FileUtils;
import com.idiominc.ws.integration.profserv.commons.wssdk.WSAttributeUtils;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSContextManager;
import com.idiominc.wssdk.WSRunnable;
import com.idiominc.wssdk.ais.WSAisException;
import com.idiominc.wssdk.ais.WSNode;
import com.idiominc.wssdk.ais.WSSystemPropertyKey;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.linguistic.WSFilterGroup;
import com.idiominc.wssdk.review.WSQualityModel;
import com.idiominc.wssdk.workflow.WSProject;
import com.idiominc.wssdk.workflow.WSProjectGroup;
import com.idiominc.wssdk.workflow.WSTask;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPathException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


//internal dependencies

import com.idiominc.external.AISUtils;
import com.idiominc.external.config.Config;
import com.idiominc.external.json.JSONArray;
import com.idiominc.external.json.JSONObject;
import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSContextManager;
import com.idiominc.wssdk.WSRunnable;
import com.idiominc.wssdk.ais.WSAisException;
import com.idiominc.wssdk.ais.WSNode;
import com.idiominc.wssdk.ais.WSSystemPropertyKey;
import com.idiominc.wssdk.asset.WSAssetTask;
import com.idiominc.wssdk.linguistic.WSFilterGroup;
import com.idiominc.wssdk.review.WSQualityModel;
import com.idiominc.wssdk.workflow.WSProject;
import com.idiominc.wssdk.workflow.WSProjectGroup;
import com.idiominc.wssdk.workflow.WSTask;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPathException;
import java.io.*;

//sdk
//apache
//sax and DOM
//J2EE and JAVA

/**
 * External component responsible for listening for project creation request and creating projects in WorldServer.
 *
 * @author SDL Professional Services
 */
public class FileWriteTest extends HttpServlet {

    //log
    private final Logger log = Logger.getLogger(FileWriteTest.class);

    protected void doPost(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {

        final Exception[] eRef = new Exception[1];

        //try {

            httpServletResponse.setContentType("application/json");

            try {

                long startTime = System.nanoTime();
                File workingData = File.createTempFile("working", ".transcription");
                long endTime = System.nanoTime();
                System.out.println("Temp File Creation Duration = " + (endTime - startTime));

                String data = "This is the data to write ot he file...";

                startTime = System.nanoTime();
                workingData = FileUtils.getStringAsFile(workingData, data, "UTF-8");
                endTime = System.nanoTime();

                System.out.println("String As File Duration = " + (endTime - startTime));

                workingData.delete();


            } catch (Exception e) {
                log.error("Project creation error:", e);
                eRef[0] = e;
            }
//                }
//            });


    }


}

