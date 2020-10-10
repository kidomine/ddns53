package cc.yggdrasil.ddns53;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;

@SpringBootApplication
public class Ddns53Application implements CommandLineRunner
{
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Ddns53 ddns53;

    @Autowired
    Ddns53Config ddns53Config;

    public static void main(String[] args)
    {
        SpringApplication.run(Ddns53Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception
    {
        logger.info("===========================================");
        logger.info("Running DDNS Client for AWS Route53");
        logger.info("===========================================");

        if (ddns53Config.parseArguments(args)) {
            ddns53Config.printDetails();

            if (ddns53.updateRoute53Record(ddns53Config)) {
                ddns53Config.updateConfigFile();
            }
        }
    }
}
