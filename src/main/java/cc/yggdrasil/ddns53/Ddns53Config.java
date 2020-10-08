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
        boolean result = false;

        logger.debug("Argument count: " + args.length);
        if (args.length != 0)
        {
            if (args.length == 8)
            {
                result = parseInputArguments(args);
            } else if (args.length == 2)
            {
                if (args[0].equals("-cfg"))
                {
                    result = parseConfigFile(args[1]);
                }
            }
        }

        if (!result)
        {
            result = parseConfigFile(System.getProperty("user.home") + "/.aws/route53");
        } else
        {
            if (fileName == null)
            {
                fileName = System.getProperty("user.home") + "/.aws/route53";
            }
        }

        return result;
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

        logger.info("Updating: " + filepath.toString());
        try (final OutputStream fp = Files.newOutputStream(filepath);
             BufferedWriter rd = new BufferedWriter(new OutputStreamWriter(fp)))
        {
            rd.write("zone_id" + " = " + hostedZoneId + "\n");
            rd.write("domain" + " = " + domainName + "\n");
            rd.write("ip_provider" + " = " + ipProvider + "\n");
            rd.write("current_ip" + " = " + currentIP + "\n");

            logger.info("Successfully updated input file: " + fileName);
        } catch (final IOException caught)
        {
            logger.error("Unable to update input file: " + fileName, caught);
        }
    }

    /**
     * Print the details of the current configuration.
     */
    public void printDetails()
    {
        logger.info("******** Current Configuration Settings ********");
        logger.info("Config File: " + fileName);
        logger.info("Zone ID    : " + hostedZoneId);
        logger.info("Domain Name: " + domainName);
        logger.info("IP Provider: " + ipProvider);
        logger.info("Current IP : " + currentIP);
        logger.info("************************************************");
    }

    /**
     * Parse the input arguments and loads them into this class.
     *
     * @param args the input arguments to be parsed
     * @return true if the input arguments have been parsed successfully, otherwise returns false
     */
    private boolean parseInputArguments(String[] args)
    {
        int index = 0;

        while (index < args.length)
        {
            if (args[index].compareTo("-i") == 0)
            {
                hostedZoneId = args[index + 1];
            } else if (args[index].compareTo("-d") == 0)
            {
                domainName = args[index + 1];
            } else if (args[index].compareTo("-p") == 0)
            {
                ipProvider = args[index + 1];
            } else if (args[index].compareTo("-c") == 0)
            {
                currentIP = args[index + 1];
            } else
            {
                logger.error("Invalid input argument: " + args[index]);
            }
            index++;
        }

        if (isLoaded())
        {
            logger.info("Successfully parsed input parameters!");
            return true;
        }

        logger.error("Unable to parse input parameters!");
        return false;
    }

    /**
     * Parse the given input file, in the process loading the configuration to this instance.
     *
     * @param filename the file containing the configuration
     * @return true if the configuration file has been parsed sucessfully, otherwise returns false
     */
    private boolean parseConfigFile(final String filename)
    {
        boolean result = false;
        final Path filepath = Paths.get(filename);

        logger.info("Trying: " + filepath.toString());

        try
        {
            InputStream fp = Files.newInputStream(filepath);
            BufferedReader rd = new BufferedReader(new InputStreamReader(fp));

            String line;
            String t_line;
            String[] s_lines;

            while ((line = rd.readLine()) != null)
            {
                t_line = line.trim();
                if (t_line.charAt(0) == '#')
                {
                    continue;
                }

                s_lines = t_line.split("=");
                if (s_lines.length == 2)
                {
                    s_lines[0] = s_lines[0].trim();
                    s_lines[1] = s_lines[1].trim();

                    if (s_lines[0].compareTo("zone_id") == 0)
                    {
                        hostedZoneId = s_lines[1];
                    } else if (s_lines[0].compareTo("domain") == 0)
                    {
                        domainName = s_lines[1];
                    } else if (s_lines[0].compareTo("ip_provider") == 0)
                    {
                        ipProvider = s_lines[1];
                    } else if (s_lines[0].compareTo("current_ip") == 0)
                    {
                        currentIP = s_lines[1];
                    }
                }
            }

            if (isLoaded())
            {
                logger.info("Successfully parsed input file: " + filename);
                fileName = filename;
                result = true;
            } else
            {
                logger.error("Unable to parse input file: " + filename);
            }

            rd.close();
        } catch (final IOException caught)
        {
            logger.error("Unable to read config file.", caught);
        }

        return result;
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
            printDetails();
            return true;
        }

        return false;
    }
}