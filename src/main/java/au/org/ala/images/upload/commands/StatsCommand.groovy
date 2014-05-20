package au.org.ala.images.upload.commands

class StatsCommand extends AbstractCommand {
    @Override
    String getCommandName() {
        return "stats"
    }

    @Override
    List<CommandArgument> getAcceptedArgs() {
        return [CommandArguments.databaseFile]
    }

    @Override
    void executeImpl(Map args) {

        println "Total images: ${imageService.countImages()}"
        println "Total nostatus: ${imageService.countByStatus('')}"
        println "Total OK: ${imageService.countByStatus('OK')}"

    }

}
