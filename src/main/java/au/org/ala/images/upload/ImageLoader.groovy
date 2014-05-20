

package au.org.ala.images.upload

import au.com.bytecode.opencsv.CSVReader
import au.org.ala.images.upload.commands.AbstractCommand
import au.org.ala.images.upload.commands.CheckStatusCommand
import au.org.ala.images.upload.commands.CommandArgument
import au.org.ala.images.upload.commands.CommandArguments
import au.org.ala.images.upload.commands.LoadDBCommand
import au.org.ala.images.upload.commands.StatsCommand
import au.org.ala.images.upload.service.CSVService
import au.org.ala.images.upload.service.ImageService
import au.org.ala.images.upload.service.WebService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import net.sf.json.JSON
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.StringBody

import java.text.SimpleDateFormat

class ImageLoader {

    static {
        registerCommand(new LoadDBCommand())
        registerCommand(new StatsCommand())
        registerCommand(new CheckStatusCommand())
    }

    private static Map<String, AbstractCommand> _COMMAND_REGISTRY = [:]

    public static registerCommand(AbstractCommand command) {
        if (command) {
            _COMMAND_REGISTRY[command.commandName] = command
        }
    }

    static void main(String[] args) {

        if (!args) {
            usage()
            System.exit(1);
        }

        List<AbstractCommand> commands = [];
        def commandArgs = [:]

//        commandArgs.threadCount = 4
//        commandArgs.serviceBaseUrl = "http://images.ala.org.au"
//        commandArgs.filename = ""

        for (int i = 0; i < args.length; ++i) {
            def arg = args[i]

            if (arg.startsWith("-")) {
                CommandArgument cmdArg = CommandArguments.findBySwitch(arg)
                if (cmdArg) {
                    i += 1
                    commandArgs[cmdArg.name] = args[i]
                }
            } else {
                def command = _COMMAND_REGISTRY[arg]
                if (command) {
                    commands << command
                }
            }

        }

        if (!commands) {
            usage();
            System.exit(0);
        }

        commandArgs = consolidateArguments(commands, commandArgs)

        ImageService imageService = null;
        if (commandArgs.containsKey("databaseFile")) {
            imageService = new ImageService(commandArgs.databaseFile)
        }

        CSVService csvService = null;
        if (commandArgs.containsKey("filename")) {
            csvService = new CSVService(commandArgs.filename)
        }

        WebService webService = null
        if (commandArgs.containsKey("serviceBaseUrl")) {
            webService = new WebService(commandArgs.serviceBaseUrl)
        }

        commands.each { command ->

            println "Executing ${command.commandName}"
            println "  with args:"
            commandArgs.each { kvp ->
                println "    ${kvp.key} = ${kvp.value}"
            }
            command.imageService = imageService
            command.csvService = csvService
            command.webService = webService

            command.execute(commandArgs)
        }

    }

    private static Map consolidateArguments(List<AbstractCommand> commands, Map args) {
        def finalArgs = [:]

        def argList = []

        commands.each { command ->
            command.acceptedArgs?.each {
                if (!argList.contains(it)) {
                    argList << it
                }
            }
        }

        argList.each {
            finalArgs[it.name] = it.defaultValue
        }

        args.each { kvp ->
            finalArgs[kvp.key] = kvp.value
        }

        return finalArgs
    }

    private static void processFile(File file, String serviceBaseUrl, int threadCount) {

        def jobDispatcher = new JobDispatcher(threadCount, serviceBaseUrl)

        println "Processing file ${file.absolutePath}..."

        def sdf = new SimpleDateFormat("yyyyMMddHHmmss")
        def importBatchId = sdf.format(new Date())
        String [] fields;
        CSVReader reader = new CSVReader(new FileReader(file));
        int count = 0
        def startTimeMillis = new Date().getTime()
        while ((fields = reader.readNext()) != null) {
            try {
                def imageUrl = fields[0]
                def metadata = [:]
                metadata.sourceUrl = fields[0]
                metadata.occurrenceId = fields[1]
                metadata.dataResourceUid = fields[2]
                metadata.collectionCode = fields[3]
                metadata.institutionCode = fields[4]
                metadata.scientificName = fields[5]
                metadata.importBatchId = importBatchId

                def uploadJob = new UploadJob(imageUrl: imageUrl, metaData: metadata)
                jobDispatcher.addJob(uploadJob)

                if (++count % 1000 == 0) {
                    println "${count} urls queued"
                }

            } catch (Exception ex) {
                println "Error on line ${count}: ${ex.message}"
            }
        }

        jobDispatcher.waitUntilFinished()

        def now = new Date()
        def expiredMillis = now.getTime() - startTimeMillis
        def ratePerSecond = (count / expiredMillis) * 1000
        println "Upload complete. ${count} image urls sent (averaging ${ratePerSecond} images per second)"
    }

    private static void usage() {
        println "Usage:"
        println "au.org.ala.images.upload.imageLoader <command> [<args>]"
        println "  where <command> is one of:"
        _COMMAND_REGISTRY.each { kvp ->

        }
    }
}
