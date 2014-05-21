package au.org.ala.images.upload.commands

class DumpCommand extends AbstractCommand {
    @Override
    String getCommandName() {
        return "dump"
    }

    @Override
    List<CommandArgument> getAcceptedArgs() {
        return [CommandArguments.databaseFile]
    }

    @Override
    void executeImpl(Map args) {
        imageService.dumpImageTable()
    }
}
