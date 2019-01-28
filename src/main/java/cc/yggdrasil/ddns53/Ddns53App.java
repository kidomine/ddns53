package cc.yggdrasil.ddns53;

import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.io.InputStream;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.*;

import org.apache.commons.io.*;


/**
 * @author yan
 *
 */
public class Ddns53App {

    private Route53Client client;
    private Ddns53Config config;

    public Ddns53App(Ddns53Config ddnsConfiguration) {
        this.client = Route53Client.builder()
                .credentialsProvider(ProfileCredentialsProvider.create())
                .endpointOverride(URI.create("https://route53.amazonaws.com"))
                .build();
        this.config = ddnsConfiguration;
    }

    public static void uninit() {
        // TODO add uninit code
    }

    public boolean update_route53_record() {
        String new_ip;
        boolean result = false;

        System.out.println("I: " + new Date() + ": updating route53 record...");

        new_ip = get_public_ip();
        if(this.config.currentIP == null) {
            this.config.currentIP = new_ip;
            result = update_route53_IP();
            if (result) {
                System.out.println("I: " + new Date() + ": assigned new IP: " + new_ip);
            }
        } else {
            if(this.config.currentIP.compareTo(new_ip) != 0) {
                this.config.currentIP = new_ip;
                result = update_route53_IP();
                if (result) {
                    System.out.println("I: " + new Date() + ": assigned new IP: " + new_ip);
                }
            } else {
                System.out.println("I: " + new Date() + ": no change in IP: " + new_ip);
            }
        }

        System.out.println("I: " + new Date() + ": finished updating route53 record!");
        return result;
    }

    public String get_public_ip() {
        String new_ip = null;

        try {
            System.out.println("I: " + new Date() + ": obtaining IP from " + this.config.ipProvider);
            URL url = new URL(this.config.ipProvider);

            String propKey = "User-Agent";
            String propVal = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";
            String encoding;

            URLConnection con = url.openConnection();
            con.setReadTimeout(2 * 1000);
            con.setDoOutput(true);
            con.setRequestProperty(propKey, propVal);

            InputStream in = con.getInputStream();

            encoding = con.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;

            new_ip = IOUtils.toString(in, encoding);

            System.out.println("I: " + new Date() + ": got new IP     : " + new_ip);

            in.close();
        } catch (Exception e) {
            System.out.println( e );
            e.printStackTrace();
        }

        return new_ip;
    }

    public boolean update_route53_IP() {
        boolean result = false;

        System.out.println("I: " + new Date() + ": updating route53 record for ID "  + this.config.hostedZoneId);

        ChangeResourceRecordSetsResponse resource_recordset_response;
        ChangeResourceRecordSetsRequest.Builder resource_recordset_request;
        ChangeBatch.Builder change_batch = ChangeBatch.builder();
        ResourceRecord.Builder resource_record = ResourceRecord.builder()
                .value(this.config.currentIP);

        List<ResourceRecord> resource_record_list = new ArrayList<ResourceRecord>();
        resource_record_list.add(resource_record.build());

        // Create a ResourceRecordSet
        ResourceRecordSet.Builder resource_recordset = ResourceRecordSet.builder()
                .name(this.config.domainName)
                .type(RRType.A)
                .ttl(300L)
                .resourceRecords(resource_record_list);

        // Create a change
        Change.Builder change = Change.builder()
                .action(ChangeAction.UPSERT)
                .resourceRecordSet(resource_recordset.build());

        List<Change> changes_list = new ArrayList<Change>();
        changes_list.add(change.build());

        // Create a change batch
        change_batch.changes(changes_list);

        // Create ChangeResourceRecordSetRequest.
        resource_recordset_request = ChangeResourceRecordSetsRequest.builder()
                .hostedZoneId(this.config.hostedZoneId)
                .changeBatch(change_batch.build());

        try {
            // Send the request and get the response.
            resource_recordset_response = this.client.changeResourceRecordSets(resource_recordset_request.build());

            // Print the result
            System.out.println(resource_recordset_response.changeInfo());

            result = true;
        } catch (Route53Exception e) {
            System.out.println("I: " + new Date() + ": encountered error: " + e);
        }

        System.out.println("I: " + new Date() + ": finished updating route53");
        return result;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        Ddns53App ddns53;
        Ddns53Config ddns_conf = new Ddns53Config();

        System.out.println("===========================================");
        System.out.println("Running DDNS Client for AWS Route53");
        System.out.println("===========================================");

        if (ddns_conf.parse_arguments(args)) {
            ddns_conf.print_details();

            ddns53 = new Ddns53App(ddns_conf);
            if (ddns53.update_route53_record()) {
                ddns_conf.update_input_file();
            }
        }
    }
}

