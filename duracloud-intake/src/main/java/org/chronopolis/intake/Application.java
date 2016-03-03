package org.chronopolis.intake;

import org.chronopolis.common.settings.ChronopolisSettings;
import org.chronopolis.intake.duracloud.config.IntakeSettings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

/**
 *
 * Created by shake on 8/1/14.
 */
@ComponentScan(basePackageClasses = {ChronopolisSettings.class,
        IntakeSettings.class},
        // JPASettings.class,
        // JPAConfiguration.class},
        basePackages = {"org.chronopolis.intake.config",
                        "org.chronopolis.intake.rest"})
@EnableAutoConfiguration
public class Application {

    private static String readLine() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException("Can't read from STDIN");
        }
    }

    public static void main(String[] args) {
        disableCertValidation();
        SpringApplication springApplication = new SpringApplication(Application.class);
        ApplicationContext ctx = springApplication.run(args);
        System.out.println("Enter 'q' to exit");

        boolean done = false;
        while (!done) {
            if ("q".equalsIgnoreCase(readLine())) {
                done = true;
                SpringApplication.exit(ctx);
            }
        }

        //SpringApplication.exit(springApplication.run(args));
    }

    private static void disableCertValidation() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (GeneralSecurityException e) {
        }
    }

}
