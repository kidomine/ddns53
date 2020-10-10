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
    private final Route53Client route53Client;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public Ddns53()
    {
        route53Client = Route53Client.builder()
                                     .credentialsProvider(ProfileCredentialsProvider.create())
                                     .endpointOverride(URI.create("https://route53.amazonaws.com"))
                                     .build();
    }

    /**
     * Updates the Route53 record with the given configuration.
     *
     * @param ddnsConfiguration the current configuration
     * @return true if the Route53 record has been updated successfully, otherwise returns false
     */
    public boolean updateRoute53Record(@NonNull final Ddns53Config ddnsConfiguration)
    {
        final String newIp = getPublicIp(ddnsConfiguration);

        logger.info("Updating Route53 record...");

        if (!ddnsConfiguration.getCurrentIP()
                   .equals(newIp) && updateRoute53Ip(ddnsConfiguration, newIp))
        {
            logger.info(String.format("Assigned new IP: %s", newIp));
            logger.info("Finished updating Route53 record!");
            return true;
        }

        logger.info(String.format("No change in IP: %s", newIp));
        return false;
    }

    /**
     * Retrieve the public IP of the network that we are currently connected to.
     *
     * @param ddnsConfiguration the current configuration
     * @return the public IP of the network we are currently connected to
     */
    private String getPublicIp(final Ddns53Config ddnsConfiguration)
    {
        String newIp = null;

        logger.info(String.format("Obtaining IP from %s", ddnsConfiguration.getIpProvider()));

        try
        {

            URL url = new URL(ddnsConfiguration.getIpProvider());

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

            logger.info(String.format("Got new IP     : %s", newIp));

            in.close();
        } catch (final Exception caught)
        {
            logger.error("Unable to get new IP from provider.", caught);
        }

        return newIp;
    }

    /**
     * Update the IP on the Route53 record.
     *
     * @param ddnsConfiguration the current configuration
     * @param newIp the new IP address
     * @return true if the IP has been updated, otherwise returns false
     */
    private boolean updateRoute53Ip(final Ddns53Config ddnsConfiguration, final String newIp)
    {
        logger.info(String.format("Updating Route53 record for ID %s", ddnsConfiguration.getHostedZoneId()));

        final ChangeBatch.Builder changeBatch = ChangeBatch.builder();

        final List<ResourceRecord> resourceRecordList = new ArrayList<>();
        resourceRecordList.add(ResourceRecord.builder()
                                             .value(newIp)
                                             .build());

        ResourceRecordSet.Builder resourceRecordset = ResourceRecordSet.builder()
                                                                       .name(ddnsConfiguration.getDomainName())
                                                                       .type(RRType.A)
                                                                       .ttl(300L)
                                                                       .resourceRecords(resourceRecordList);

        Change.Builder change = Change.builder()
                                      .action(ChangeAction.UPSERT)
                                      .resourceRecordSet(resourceRecordset.build());

        final List<Change> changesList = new ArrayList<>();
        changesList.add(change.build());
        changeBatch.changes(changesList);

        final ChangeResourceRecordSetsRequest.Builder resourceRecordsetRequest =
                ChangeResourceRecordSetsRequest.builder()
                                               .hostedZoneId(ddnsConfiguration.getHostedZoneId())
                                               .changeBatch(changeBatch.build());

        boolean result = false;
        try
        {
            final ChangeResourceRecordSetsResponse resourceRecordsetResponse =
                    route53Client.changeResourceRecordSets(resourceRecordsetRequest.build());

            logger.info(String.valueOf(resourceRecordsetResponse.changeInfo()));
            logger.info("Finished updating Route53");

            ddnsConfiguration.setCurrentIP(newIp);

            result = true;
        } catch (final Route53Exception caught)
        {
            logger.error("Unable to update IP on Route53.", caught);
        }

        return result;
    }
}

