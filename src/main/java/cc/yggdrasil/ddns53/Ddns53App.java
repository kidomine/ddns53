package cc.yggdrasil.ddns53;

//import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.*;

import org.apache.commons.io.*;

//import cc.yggdrasil.ddns53.Ddns53Config;

/**
 * @author yan
 *
 */
public class Ddns53App {

    private static Route53Client ddns_R53;
    private static Ddns53Config ddns_Config;

    public static void init(Ddns53Config ddns_conf) {
        ddns_R53 = Route53Client.builder()
                .credentialsProvider(ProfileCredentialsProvider.create())
                .endpointOverride(URI.create("https://route53.amazonaws.com"))
                .build();
        ddns_Config = ddns_conf;
    }

    public static void uninit() {
        // TODO add uninit code
    }

    public static int update_route53_record() {
        String new_ip = null;
        int result = 0;

        System.out.println("I: " + new Date() + ": updating route53 record...");

        new_ip = get_public_ip();
        if(ddns_Config.ddnscfg_CurrentIP == null) {
            ddns_Config.ddnscfg_CurrentIP = new_ip;
            update_route53_IP();
            System.out.println("I: " + new Date() + ": assigned new IP: " + new_ip);
            result = 1;
        } else {
            if(ddns_Config.ddnscfg_CurrentIP.compareTo(new_ip) != 0) {
                ddns_Config.ddnscfg_CurrentIP = new_ip;
                update_route53_IP();
                System.out.println("I: " + new Date() + ": assigned new IP: " + new_ip);
                result = 1;
            } else {
                System.out.println("I: " + new Date() + ": no change in IP: " + new_ip);
                result = 0;
            }
        }

        System.out.println("I: " + new Date() + ": finished updating route53 record!");
        return result;
    }

    public static String get_public_ip() {
        String new_ip = null;

        try {
            System.out.println("I: " + new Date() + ": obtaining IP from " + ddns_Config.ddnscfg_IPProvider);
            URL url = new URL(ddns_Config.ddnscfg_IPProvider);

            String propKey = "User-Agent";
            String propVal = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";
            String encoding = null;

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

    public static void update_route53_IP() {
        System.out.println("I: " + new Date() + ": updating route53 record for ID "  + ddns_Config.ddnscfg_HostedZoneId);

        ChangeResourceRecordSetsResponse resource_recordset_response;
        ChangeResourceRecordSetsRequest.Builder resource_recordset_request;
        ChangeBatch.Builder change_batch = ChangeBatch.builder();
        ResourceRecord.Builder resource_record = ResourceRecord.builder()
                .value(ddns_Config.ddnscfg_CurrentIP);

        List<ResourceRecord> resource_record_list = new ArrayList<ResourceRecord>();
        resource_record_list.add(resource_record.build());

        // Create a ResourceRecordSet
        ResourceRecordSet.Builder resource_recordset = ResourceRecordSet.builder()
                .name(ddns_Config.ddnscfg_DomainName)
                .type(RRType.A)
                .ttl(new Long(300))
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
                .hostedZoneId(ddns_Config.ddnscfg_HostedZoneId)
                .changeBatch(change_batch.build());

        try {
            // Send the request and get the response.
            resource_recordset_response = ddns_R53.changeResourceRecordSets(resource_recordset_request.build());

        } catch (Route53Exception e) {
            System.out.println("I: " + new Date() + ": encountered error: " + e);
            throw e;
        }

        // Print the result
        System.out.println(resource_recordset_response.changeInfo());


        System.out.println("I: " + new Date() + ": finished updating route53");
    }

    public static Ddns53Config parse_input_parameters(String[] args) {
        Ddns53Config ddns_cfg;
        String       zone_id     = null;
        String       domain_name = null;
        String       ip_provider = null;
        String       current_ip  = null;
        int          index       = 0;


        while(index < args.length) {
            if(args[index].compareTo("-i") == 0) {
                zone_id = args[index + 1];
            } else if (args[index].compareTo("-d") == 0) {
                domain_name = args[index + 1];
            } else if (args[index].compareTo("-p") == 0) {
                ip_provider = args[index + 1];
            } else if (args[index].compareTo("-c") == 0) {
                current_ip = args[index + 1];
            } else {
                System.out.println("E: " + new Date() + ": invalid input argument: " + args[index]);
            }
            index++;
        }

        if ((zone_id == null) ||
                (domain_name == null) ||
                (ip_provider == null) ||
                (current_ip == null))
        {
            System.out.println("E: " + new Date() + ": unable to parse input parameters!");
            return null;
        } else {
            System.out.println("I: " + new Date() + ": successfully parsed input parameters!");
        }

        ddns_cfg = new Ddns53Config(null,
                zone_id,
                domain_name,
                ip_provider,
                current_ip);
        ddns_cfg.print_details();

        return ddns_cfg;
    }

    public static Ddns53Config parse_input_file(String filename) {
        Ddns53Config ddns_cfg    = null;
        Path filepath    = Paths.get(filename);
        String          zone_id     = null;
        String          domain      = null;
        String          ip_provider = null;
        String          current_ip  = null;

        System.out.println("I: " + new Date() + ": trying: " + filepath.toString());
        try (InputStream fp = Files.newInputStream(filepath);
             BufferedReader rd = new BufferedReader(new InputStreamReader(fp))) {
            String line      = null;
            String t_line    = null;
            String[] s_lines = null;

            while((line = rd.readLine()) != null) {
                // interpret each line
                t_line = line.trim();
                if (t_line.charAt(0) == '#') {
                    continue;
                }

                s_lines = t_line.split("=");
                if (s_lines.length == 2) {
                    s_lines[0] = s_lines[0].trim();
                    s_lines[1] = s_lines[1].trim();
                    //parse
                    if (s_lines[0].compareTo("zone_id") == 0) {
                        zone_id = s_lines[1];
                    } else if (s_lines[0].compareTo("domain") == 0) {
                        domain = s_lines[1];
                    } else if (s_lines[0].compareTo("ip_provider") == 0) {
                        ip_provider = s_lines[1];
                    } else if (s_lines[0].compareTo("current_ip") == 0) {
                        current_ip = s_lines[1];
                    }
                }
            }

            rd.close();

            if (zone_id != null && domain != null && ip_provider != null) {
                ddns_cfg = new Ddns53Config(filename,
                        zone_id,
                        domain,
                        ip_provider,
                        current_ip);
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }

        if (ddns_cfg == null) {
            System.out.println("E: " + new Date() + ": unable to parse input file: " + filename);
        } else {
            System.out.println("I: " + new Date() + ": successfully parsed input file: " + filename);
        }

        return ddns_cfg;
    }

    public static Ddns53Config parse_arguments(String[] args) {
        Ddns53Config ddns_cfg = null;

        if (args.length != 0) {
            if (args.length == 8)
            {
                ddns_cfg = Ddns53App.parse_input_parameters(args);
            } else if (args.length == 2) {
                if (args[0].equals("-cfg")) {
                    ddns_cfg = Ddns53App.parse_input_file(args[1]);
                }
            }
        }

        if (ddns_cfg == null) {
            ddns_cfg = Ddns53App.parse_input_file(System.getProperty("user.home") + "/.aws/route53");
        } else {
            if (ddns_cfg.ddnscfg_Filename == null) {
                ddns_cfg.ddnscfg_Filename = System.getProperty("user.home") + "/.aws/route53";
            }
        }

        return ddns_cfg;
    }

    public static int update_input_file(Ddns53Config ddns_cfg) {
        if (ddns_cfg.ddnscfg_Filename == null) {
            System.out.println("W: " + new Date() + ": nothing to update, no input file provided");
            return 0;
        }

        Path filepath = Paths.get(ddns_cfg.ddnscfg_Filename);

        System.out.println("I: " + new Date() + ": updating: " + filepath.toString());
        try (OutputStream fp = Files.newOutputStream(filepath);
             BufferedWriter rd = new BufferedWriter(new OutputStreamWriter(fp))) {
            rd.write("zone_id" + " = " + ddns_cfg.ddnscfg_HostedZoneId + "\n");
            rd.write("domain" + " = " + ddns_cfg.ddnscfg_DomainName + "\n");
            rd.write("ip_provider" + " = " + ddns_cfg.ddnscfg_IPProvider + "\n");
            rd.write("current_ip" + " = " + ddns_cfg.ddnscfg_CurrentIP + "\n");

            // not necessary:
            // rd.close();
        } catch (IOException ex) {
            System.err.println(ex);
            System.out.println("E: " + new Date() + ": unable to update input file: " + ddns_cfg.ddnscfg_Filename);
            return -1;
        }

        System.out.println("I: " + new Date() + ": successfully updated input file: " + ddns_cfg.ddnscfg_Filename);

        return 0;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        Ddns53Config ddns_conf;

        System.out.println("===========================================");
        System.out.println("Running DDNS Client for AWS Route53");
        System.out.println("===========================================");

        ddns_conf = Ddns53App.parse_arguments(args);
        if(ddns_conf == null) {
            return;
        }

        ddns_conf.print_details();

        Ddns53App.init(ddns_conf);
        if (Ddns53App.update_route53_record() == 1) {
            Ddns53App.update_input_file(ddns_conf);
        }

    }
}

