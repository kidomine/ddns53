package cc.yggdrasil.ddns53;

import lombok.*;
import org.slf4j.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.nio.file.*;

@Component
@Getter
@Setter
public class Ddns53Config
{
    @NonNull
    private String fileName;

    @NonNull
    private String hostedZoneId;

    @NonNull
    private String domainName;

    @NonNull
    private String currentIP;

    @NonNull
    private String ipProvider;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Parse the input arguments and loads them into this class.
     *
     * @param args the input arguments to be parsed
     * @return true if the input arguments have been parsed successfully, otherwise returns false
     */
    public boolean parseArguments(String[] args)
    {
        logger.debug(String.format("Argument count: %s", args.length));

        if (args.length == 0)
        {
            final String filename = System.getProperty("user.home") + "/.aws/route53";
            return loadConfigFile(filename);
        }

        if (args.length == 8)
        {
            return loadInputArguments(args);
        }

        if (args.length == 2 && args[0].equals("-cfg"))
        {
            return loadConfigFile(args[1]);
        }

        return false;
    }

    /**
     * Updates the config file with new values.
     */
    public void updateConfigFile()
    {
        if (fileName == null)
        {
            logger.warn("Nothing to update, no input file provided");
            return;
        }

        final Path filepath = Paths.get(fileName);

        logger.info(String.format("Updating: %s", filepath.toString()));
        try (final OutputStream fp = Files.newOutputStream(filepath);
             final BufferedWriter rd = new BufferedWriter(new OutputStreamWriter(fp)))
        {
            rd.write("zone_id" + " = " + hostedZoneId + "\n");
            rd.write("domain" + " = " + domainName + "\n");
            rd.write("ip_provider" + " = " + ipProvider + "\n");
            rd.write("current_ip" + " = " + currentIP + "\n");

            logger.info(String.format("Successfully updated input file: %s", fileName));
        }
        catch (final IOException caught)
        {
            logger.error(String.format("Unable to update input file: %s", fileName), caught);
        }
    }

    /**
     * Print the details of the current configuration.
     */
    public void printDetails()
    {
        logger.info("******** Current Configuration Settings ********");
        logger.info(String.format("Config File: %s", fileName));
        logger.info(String.format("Zone ID    : %s", hostedZoneId));
        logger.info(String.format("Domain Name: %s", domainName));
        logger.info(String.format("IP Provider: %s", ipProvider));
        logger.info(String.format("Current IP : %s", currentIP));
        logger.info("************************************************");
    }

    /**
     * Parse the input arguments and loads them into this class.
     *
     * @param args the input arguments to be parsed
     * @return true if the input arguments have been parsed successfully, otherwise returns false
     */
    private boolean loadInputArguments(String[] args)
    {
        int index = 0;

        while (index < args.length)
        {
            if (args[index].equals("-i"))
            {
                hostedZoneId = args[index + 1];
            } else if (args[index].equals("-d"))
            {
                domainName = args[index + 1];
            } else if (args[index].equals("-p"))
            {
                ipProvider = args[index + 1];
            } else if (args[index].equals("-c"))
            {
                currentIP = args[index + 1];
            } else
            {
                logger.error(String.format("Invalid input argument: %s", args[index]));
            }
            index++;
        }

        return isLoaded();
    }

    /**
     * Parse the given input file, in the process loading the configuration to this instance.
     *
     * @param filename the file containing the configuration
     * @return true if the configuration file has been parsed sucessfully, otherwise returns false
     */
    private boolean loadConfigFile(final String filename)
    {
        final Path filepath = Paths.get(filename);

        logger.info(String.format("Trying: %s", filepath.toString()));

        try
        {
            final InputStream fp = Files.newInputStream(filepath);
            try (final BufferedReader rd = new BufferedReader(new InputStreamReader(fp)))
            {

                String line;
                String trimmedLine;
                String[] splitLines;

                while ((line = rd.readLine()) != null)
                {
                    trimmedLine = line.trim();
                    if (trimmedLine.charAt(0) == '#')
                    {
                        continue;
                    }

                    splitLines = trimmedLine.split("=");
                    if (splitLines.length == 2)
                    {
                        splitLines[0] = splitLines[0].trim();
                        splitLines[1] = splitLines[1].trim();

                        if (splitLines[0].equalsIgnoreCase("zone_id"))
                        {
                            hostedZoneId = splitLines[1];
                        } else if (splitLines[0].equalsIgnoreCase("domain"))
                        {
                            domainName = splitLines[1];
                        } else if (splitLines[0].equalsIgnoreCase("ip_provider"))
                        {
                            ipProvider = splitLines[1];
                        } else if (splitLines[0].equalsIgnoreCase("current_ip"))
                        {
                            currentIP = splitLines[1];
                        }
                    }
                }
            }

            printDetails();

            return isLoaded();
        } catch (final IOException caught)
        {
            logger.error("Unable to read config file.", caught);
        }

        return false;
    }

    /**
     * Checks if the configuration is loaded into this class.
     *
     * @return true if this class has been loaded, otherwise returns false
     */
    private boolean isLoaded()
    {
        if (hostedZoneId != null && domainName != null && ipProvider != null && currentIP != null)
        {
            logger.info("Configuration has been successfully loaded!");
            return true;
        }

        logger.info("Configuration has not been loaded!");
        return false;
    }
}