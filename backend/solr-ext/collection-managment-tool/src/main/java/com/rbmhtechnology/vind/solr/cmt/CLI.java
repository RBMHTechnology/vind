package com.rbmhtechnology.vind.solr.cmt;

import com.google.common.base.Strings;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * Created by fonso on 07.04.17.
 */
public class CLI {

    private static final Logger logger = LoggerFactory.getLogger(CLI.class);
    private static HelpFormatter formatter = new HelpFormatter();

    public static void main( String[] args ) {

        // parse the command line arguments
        final Options options = configCliOptions();
        CommandLine line = null;
        try {
            line = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            logger.error("CLI parsing error: {}", e.getMessage());
            formatter.printHelp("Collections Management Tool", options);
            System.exit(-1);
        }
        try {
            exec(options, line);
        } catch ( IOException e) {
            logger.error("CLI parsing error: {}", e.getMessage());
            formatter.printHelp("Collections Management Tool", options);
            System.exit(-1);
        }
    }

    public static void exec(final Options options, final CommandLine line) throws IOException {

        try {
            if (Strings.isNullOrEmpty(line.getOptionValue("createCollection"))
                    && Strings.isNullOrEmpty(line.getOptionValue("updateCollection"))) {
                throw new ParseException("To create a new collection the collection name should be provided.");
            }

            String configSetName = line.getOptionValue("from");
            if (Strings.isNullOrEmpty(configSetName)) {
                throw new ParseException("To create a new collection the configset name should be provided.");
            }

            String zkHost = line.getOptionValue("in");
            if (Strings.isNullOrEmpty(zkHost)) {
                throw new ParseException("To create a new collection The zookeeper address should be provided.");
            }

            String[] repositories = line.getOptionValues("repositories");


            if( line.hasOption( "createCollection" ) ) {

                Number collectionShards = (Number)line.getParsedOptionValue("shards");
                if (Objects.isNull(collectionShards)) {
                    collectionShards = 1;
                }

                Number collectionReplicas = (Number)line.getParsedOptionValue("replicas");
                if (Objects.isNull(collectionReplicas)) {
                    collectionReplicas = 1;
                }

                final String collectionName = line.getOptionValue("createCollection");


                logger.info("Creating collection {} from configset {} in [{}] with {} shards and replication factor of {}.", collectionName,configSetName,zkHost, collectionShards, collectionReplicas);
                final CollectionManagementService cmService = new CollectionManagementService(zkHost, repositories);
                cmService.createCollection(collectionName,configSetName,collectionShards.intValue(),collectionReplicas.intValue());

                logger.info("Created collection {} successfully", collectionName);
                System.exit(0);
            }

            if( line.hasOption( "updateCollection" ) ) {

                if(line.hasOption("shards") || line.hasOption("replicas")) {
                    throw new ParseException("CLI error: shards/replicas cannot be set when updating a collection.");
                }

                final String collectionName = line.getOptionValue("updateCollection");

                logger.info("Updating collection {} from configSet {} in [{}]", collectionName,configSetName,zkHost);
                final CollectionManagementService cmService = new CollectionManagementService(zkHost, repositories);
                cmService.updateCollection(collectionName, configSetName);

                logger.info("Updated collection {} successfully", collectionName);
                System.exit(0);
            }

            if( !((line.hasOption( "help")||line.hasOption( "createCollection")||line.hasOption( "updateCollection" ))&&
                    line.hasOption( "from" ) && line.hasOption( "in" ))) {
                formatter.printHelp("Collections Management Tool", options);
                System.exit(-1);
            }

        } catch(ParseException e) {
            logger.error("Unexpected command line exception: {}", e.getMessage() );
            formatter.printHelp("Collections Management Tool", options);
            System.exit(-1);
        } catch (Exception e) {
            logger.error("Error occurred while initializing core: {}", e.getMessage());
            System.exit(-1);
        }
    }

    public static Options configCliOptions() {

        final Options options = new Options();

        OptionGroup optionGroup = new OptionGroup();
        optionGroup.setRequired(true);
        optionGroup.addOption(Option.builder("cc")
                .optionalArg(false)
                .hasArg()
                .argName("COLLECTION NAME")
                .longOpt("createCollection")
                .desc("Creates a new collection in the solr cloud. Expects a collection name, a configSet name and the Zookeeper address.")
                .required(false)
                .build());
        optionGroup.addOption(Option.builder("uc")
                .hasArg()
                .optionalArg(false)
                .argName("COLLECTION NAME")
                .longOpt("updateCollection")
                .desc("Updates an existing collection in the solr cloud. Expects a collection name, a configSet name and the Zookeeper address.")
                .required(false)
                .build());
        optionGroup.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Shows this help info.")
                .required(false)
                .build());

        OptionGroup configSetGroup = new OptionGroup();
        configSetGroup.setRequired(true);
        configSetGroup.addOption(Option.builder("from")
                .hasArg()
                .optionalArg(false)
                .argName("CONFIGSET NAME")
                .longOpt("from")
                .desc("Defines the configset of the collection. It should be a valid artifact name.")
                .optionalArg(false)
                .required(true)
                .build());

        OptionGroup zookeeperGroup = new OptionGroup();
        zookeeperGroup.setRequired(true);
        zookeeperGroup.addOption(Option.builder("in")
                .hasArg()
                .optionalArg(false)
                .argName("ZOOKEEPER ADDRESS")
                .longOpt("in")
                .desc("Defines the solr cloud entry point. It should be a reachable URL.")
                .optionalArg(false)
                .required(true)
                .build());

        OptionGroup shardGroup = new OptionGroup();
        shardGroup.setRequired(false);
        shardGroup.addOption(Option.builder("s")
                .hasArg()
                .type(Number.class)
                .optionalArg(false)
                .argName("NUMBER OF SHARDS")
                .longOpt("shards")
                .desc("Defines the number of shards in the solr cloud. Just valid when creating a new collection. Default value is 1.")
                .required(false)
                .build());

        OptionGroup replicaGroup = new OptionGroup();
        replicaGroup.setRequired(false);
        replicaGroup.addOption(Option.builder("r")
                .hasArg()
                .type(Number.class)
                .optionalArg(false)
                .argName("NUMBER OF REPLICAS")
                .longOpt("replicas")
                .desc("Defines the number of replicas per shard in the solr cloud. Just valid when creating a new collection. Default value is 1.")
                .required(false)
                .build());

        OptionGroup repoGroup = new OptionGroup();
        repoGroup.setRequired(false);
        repoGroup.addOption(Option.builder("re")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .optionalArg(false)
                .argName("REPOSITORY URLS")
                .longOpt("repositories")
                .desc("A space separated list of repository URLs.")
                .required(false)
                .build());

        options.addOptionGroup(optionGroup);
        options.addOptionGroup(configSetGroup);
        options.addOptionGroup(zookeeperGroup);
        options.addOptionGroup(shardGroup);
        options.addOptionGroup(replicaGroup);
        options.addOptionGroup(repoGroup);

        return options;
    }
}

