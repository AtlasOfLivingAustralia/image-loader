package au.org.ala.images.upload.commands

class UploadCommand extends AbstractCommand {

    @Override
    String getCommandName() {
        return "upload"
    }

    @Override
    List<CommandArgument> getAcceptedArgs() {
        return [CommandArguments.databaseFile, CommandArguments.serviceBaseUrl]
    }

    @Override
    void executeImpl(Map args) {

    }
}
