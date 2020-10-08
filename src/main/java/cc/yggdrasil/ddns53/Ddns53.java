package cc.yggdrasil.ddns53;

import lombok.*;
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

    @NonNull
    private Ddns53Config config;
    private Route53Client route53Client;

    @Autowired
    public Ddns53(final Ddns53Config ddnsConfiguration)
    {
        route53Client = Route53Client.builder()
                                     .credentialsProvider(ProfileCredentialsProvider.create())
                                     .endpointOverride(URI.create("https://route53.amazonaws.com"))
                                     .build();
        config = ddnsConfiguration;
    }

    /**
     * Updates the Route53 record with the given configuration.
     *
     * @return true if the Route53 record has been updated successfully, otherwise returns false
     */
    public boolean updateRoute53Record()
    {
        final String newIp = getPublicIp();
        boolean result = false;

        logger.info("Updating Route53 record...");

        if (config.getCurrentIP() == null)
        {
            result = updateRoute53Ip(newIp);
            if (result)
            {
                logger.info("Assigned new IP: " + newIp);
            }
        } else
        {
            if (!config.getCurrentIP()
                       .equals(newIp))
            {
                result = updateRoute53Ip(newIp);
                if (result)
                {
                    logger.info("Assigned new IP: " + newIp);
                }
            } else
            {
                logger.info("No change in IP: " + newIp);
            }
        }

        logger.info("Finished updating Route53 record!");
        return result;
    }

    /**
     * Retrieve the public IP of the network that we are currently connected to.
     *
     * @return the public IP of the network we are currently connected to
     */
    private String getPublicIp()
    {
        String newIp = null;

        logger.info("Obtaining IP from " + config.getIpProvider());

        try
        {

            URL url = new URL(config.getIpProvider());

            final String propKey = "User-Agent";
            final String propVal = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316" +
                    " Firefox/3.6.2";

            URLConnection con = url.openConnection();
            con.setReadTimeout(2 * 1000);
            con.setDoOutput(true);
            con.setRequestProperty(propKey, propVal);

            InputStream in = con.getInputStream();

            String encoding = con.getContentEncoding();
            if (encoding == null)
            {
                encoding = "UTF-8";
            }

            newIp = IOUtils.toString(in, encoding);

            logger.info("Got new IP     : " + newIp);

            in.close();
        } catch (Exception caught)
        {
            logger.error("Unable to get new IP from provider.", caught);
        }

        return newIp;
    }

    /**
     * Update the IP on the Route53 record.
     *
     * @param newIp the new IP address
     * @return true if the IP has been updated, otherwise returns false
     */
    private boolean updateRoute53Ip(final String newIp)
    {
        logger.info("Updating Route53 record for ID " + config.getHostedZoneId());

        final ChangeBatch.Builder change_batch = ChangeBatch.builder();

        List<ResourceRecord> resource_record_list = new ArrayList<ResourceRecord>();
        resource_record_list.add(ResourceRecord.builder()
                                               .value(newIp)
                                               .build());

        ResourceRecordSet.Builder resource_recordset = ResourceRecordSet.builder()
                                                                        .name(config.getDomainName())
                                                                        .type(RRType.A)
                                                                        .ttl(300L)
                                                                        .resourceRecords(resource_record_list);

        Change.Builder change = Change.builder()
                                      .action(ChangeAction.UPSERT)
                                      .resourceRecordSet(resource_recordset.build());

        List<Change> changes_list = new ArrayList<Change>();
        changes_list.add(change.build());
        change_batch.changes(changes_list);

        final ChangeResourceRecordSetsRequest.Builder resource_recordset_request =
                ChangeResourceRecordSetsRequest.builder()
                                               .hostedZoneId(config.getHostedZoneId())
                                               .changeBatch(change_batch.build());

        boolean result = false;
        try
        {
            final ChangeResourceRecordSetsResponse resource_recordset_response =
                    route53Client.changeResourceRecordSets(resource_recordset_request.build());

            logger.info(String.valueOf(resource_recordset_response.changeInfo()));
            logger.info("Finished updating Route53");

            config.setCurrentIP(newIp);

            result = true;
        } catch (Route53Exception caught)
        {
            logger.error("Unable to update IP on Route53." + caught);
        }

        return result;
    }
}

