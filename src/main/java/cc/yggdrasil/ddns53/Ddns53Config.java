package cc.yggdrasil.ddns53;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class Ddns53Config {
    String fileName;
    String hostedZoneId;
    String domainName;
    String currentIP;
    String ipProvider;

    public Ddns53Config() {
        this.fileName = null;
        this.hostedZoneId = null;
        this.domainName = null;
        this.ipProvider = null;
        this.currentIP = null;
    }

    public boolean parseInputParameters(String[] args) {
        boolean result = false;
        int     index  = 0;

        while(index < args.length) {
            if(args[index].compareTo("-i") == 0) {
                this.hostedZoneId = args[index + 1];
            } else if (args[index].compareTo("-d") == 0) {
                this.domainName = args[index + 1];
            } else if (args[index].compareTo("-p") == 0) {
                this.ipProvider = args[index + 1];
            } else if (args[index].compareTo("-c") == 0) {
                this.currentIP = args[index + 1];
            } else {
                System.out.println("E: " + new Date() + ": invalid input argument: " + args[index]);
            }
            index++;
        }

        if ((this.hostedZoneId != null) &&
                (this.domainName != null) &&
                (this.ipProvider != null) &&
                (this.currentIP != null))
        {
            System.out.println("I: " + new Date() + ": successfully parsed input parameters!");
            this.printDetails();
            result = true;
        } else {
            System.out.println("E: " + new Date() + ": unable to parse input parameters!");
        }

        return result;
    }

    public boolean parseInputFile(String filename) {
        boolean result = false;
        Path filepath  = Paths.get(filename);

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
                        this.hostedZoneId = s_lines[1];
                    } else if (s_lines[0].compareTo("domain") == 0) {
                        this.domainName = s_lines[1];
                    } else if (s_lines[0].compareTo("ip_provider") == 0) {
                        this.ipProvider = s_lines[1];
                    } else if (s_lines[0].compareTo("current_ip") == 0) {
                        this.currentIP = s_lines[1];
                    }
                }
            }

            rd.close();
        } catch (IOException ex) {
            System.err.println(ex);
        }

        if (this.hostedZoneId != null && domainName != null && ipProvider != null) {
            System.out.println("I: " + new Date() + ": successfully parsed input file: " + filename);
            result = true;
        } else {
            System.out.println("E: " + new Date() + ": unable to parse input file: " + filename);
        }

        return result;
    }

    public boolean updateInputFile() {
        boolean result = false;

        if (this.fileName == null) {
            System.out.println("W: " + new Date() + ": nothing to update, no input file provided");
            return false;
        } else {
            Path filepath = Paths.get(this.fileName);

            System.out.println("I: " + new Date() + ": updating: " + filepath.toString());
            try (OutputStream fp = Files.newOutputStream(filepath);
                 BufferedWriter rd = new BufferedWriter(new OutputStreamWriter(fp))) {
                rd.write("zone_id" + " = " + this.hostedZoneId + "\n");
                rd.write("domain" + " = " + this.domainName + "\n");
                rd.write("ip_provider" + " = " + this.ipProvider + "\n");
                rd.write("current_ip" + " = " + this.currentIP + "\n");

                // not necessary:
                // rd.close();

                System.out.println("I: " + new Date() + ": successfully updated input file: " + this.fileName);
                result = true;
            } catch (IOException ex) {
                System.err.println(ex);
                System.out.println("E: " + new Date() + ": unable to update input file: " + this.fileName);
            }
        }

        return result;
    }

    public boolean parseArguments(String[] args) {
        boolean result = false;

        System.out.println("D: arg count: " + args.length);
        if (args.length != 0) {
            if (args.length == 8)
            {
                result = this.parseInputParameters(args);
            } else if (args.length == 2) {
                if (args[0].equals("-cfg")) {
                    result = this.parseInputFile(args[1]);
                }
            }
        }

        if (!result) {
            result = this.parseInputFile(System.getProperty("user.home") + "/.aws/route53");
        } else {
            if (this.fileName == null) {
                this.fileName = System.getProperty("user.home") + "/.aws/route53";
            }
        }

        return result;
    }

    public void printDetails() {
        System.out.println("I: " + new Date() + ": ******** Current Configuration Settings ********");
        System.out.println("I: " + new Date() + ": Config File: " + this.fileName);
        System.out.println("I: " + new Date() + ": Zone ID    : " + this.hostedZoneId);
        System.out.println("I: " + new Date() + ": Domain Name: " + this.domainName);
        System.out.println("I: " + new Date() + ": IP Provider: " + this.ipProvider);
        System.out.println("I: " + new Date() + ": Current IP : " + this.currentIP);
        System.out.println("I: " + new Date() + ": ************************************************");
    }
}