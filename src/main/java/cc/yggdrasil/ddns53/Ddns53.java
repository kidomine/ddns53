package cc.yggdrasil.ddns53;

import org.apache.commons.io.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.services.route53.*;
import software.amazon.awssdk.services.route53.model.*;

import java.io.*;
import java.net.*;
import java.util.*;


/**
 * Class used to update a Route53 record set to make it point to the network we are currently connected on..
 *
 * @author yan
 */
@Component
public class Ddns53
{

    Logger logger = LoggerFactory.getLogger(this.getClass());

    private Ddns53Config config;
    private Route53Client route53Client;

    @Autowired
    public Ddns53(Ddns53Config ddnsConfiguration)
    {
        route53Client = Route53Client.builder()
                                     .credentialsProvider(ProfileCredentialsProvider.create())
                                     .endpointOverride(URI.create("https://route53.amazonaws.com"))
                                     .build();
        config = ddnsConfiguration;
    }

    public boolean updateRoute53Record()
    {
        String new_ip;
        boolean result = false;

        logger.info("Updating route53 record...");

        new_ip = getPublicIp();
        if (this.config.currentIP == null)
        {
            this.config.currentIP = new_ip;
            result = updateRoute53Ip();
            if (result)
            {
                logger.info("Assigned new IP: " + new_ip);
            }
        } else
        {
            if (this.config.currentIP.compareTo(new_ip) != 0)
            {
                this.config.currentIP = new_ip;
                result = updateRoute53Ip();
                if (result)
                {
                    logger.info("Assigned new IP: " + new_ip);
                }
            } else
            {
                logger.info("No change in IP: " + new_ip);
            }
        }

        logger.info("Finished updating route53 record!");
        return result;
    }

    /**
     * Retrieve the public IP of the network that we are currently connected to.
     *
     * @return the public IP of the network we are currently connected to
     */
    private String getPublicIp()
    {
        String new_ip = null;

        logger.info("Obtaining IP from " + config.ipProvider);

        try
        {

            URL url = new URL(this.config.ipProvider);

            String propKey = "User-Agent";
            String propVal = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 " +
                    "Firefox/3.6.2";
            String encoding;

            URLConnection con = url.openConnection();
            con.setReadTimeout(2 * 1000);
            con.setDoOutput(true);
            con.setRequestProperty(propKey, propVal);

            InputStream in = con.getInputStream();

            encoding = con.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;

            new_ip = IOUtils.toString(in, encoding);

            logger.info("Got new IP     : " + new_ip);

            in.close();
        } catch (Exception caught)
        {
            logger.error("Unable to get new IP from provider.", caught);
        }

        return new_ip;
    }

    /**
     * Update the IP on the Route53 record.
     *
     * @return true if the IP has been updated, otherwise returns false
     */
    private boolean updateRoute53Ip()
    {
        logger.info("Updating route53 record for ID " + this.config.hostedZoneId);

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

        boolean result = false;
        try
        {
            resource_recordset_response = this.route53Client.changeResourceRecordSets(resource_recordset_request.build());

            logger.info(String.valueOf(resource_recordset_response.changeInfo()));
            logger.info("Finished updating Route53");

            result = true;
        } catch (Route53Exception caught)
        {
            logger.error("Unable to update IP on Route53." + caught);
        }

        return result;
    }
}

