package cc.yggdrasil.ddns53;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;

@SpringBootApplication
public class Ddns53Application implements CommandLineRunner
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Ddns53 ddns53;
    private final Ddns53Config ddns53Config;

    @Autowired
    public Ddns53Application(final Ddns53 ddns53, final Ddns53Config ddns53Config)
    {
        this.ddns53 = ddns53;
        this.ddns53Config = ddns53Config;
    }

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
