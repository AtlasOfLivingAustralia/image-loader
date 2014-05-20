package au.org.ala.images.upload.commands

class LoadDBCommand extends AbstractCommand {

    @Override
    String getCommandName() {
        return "loaddb"
    }

    @Override
    List<CommandArgument> getAcceptedArgs() {
        return [ CommandArguments.databaseFile, CommandArguments.filename ]
    }

    @Override
    void executeImpl(Map args) {

        imageService.initializeDatabase()

        def batch = []
        csvService.eachLine { map, lineNumber ->

            batch << [sourceUrl: map.sourceUrl, metadata: map]
            if (lineNumber % 1000 == 0) {
                println "${lineNumber}. flushing batch"
                imageService.insertImages(batch)
                batch = []
            }
        }

        if (batch) {
            println "Last batch (${batch.size()})"
            imageService.insertImages(batch)
        }

    }

}
