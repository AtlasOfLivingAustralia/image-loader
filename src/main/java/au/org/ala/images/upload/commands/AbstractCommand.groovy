package au.org.ala.images.upload.commands

import au.org.ala.images.upload.service.CSVService
import au.org.ala.images.upload.service.ImageService
import au.org.ala.images.upload.service.WebService

abstract class AbstractCommand {

    public ImageService imageService
    public CSVService csvService
    public WebService webService

    abstract String getCommandName()

    abstract List<CommandArgument> getAcceptedArgs()

    abstract void executeImpl(Map args)

    public void execute(Map args, Closure onComplete = null) {

        executeImpl(args)

        if (onComplete) {
            onComplete()
        }
    }

}
